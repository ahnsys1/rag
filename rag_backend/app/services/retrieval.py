"""Semantic (pgvector cosine) and hybrid (vector + full-text, RRF-fused) retrieval."""

import uuid

from sqlalchemy import bindparam, text
from sqlalchemy.orm import Session

from ..embeddings import embed_query
from ..schemas import SearchHit

RRF_K = 60


def _vector_literal(vec: list[float]) -> str:
    return "[" + ",".join(f"{x:.8f}" for x in vec) + "]"


def _doc_filter(ids: list[uuid.UUID] | None, alias: str = "c") -> str:
    return f"AND {alias}.document_id IN :ids" if ids else ""


def _rows_to_hits(rows) -> list[SearchHit]:
    return [
        SearchHit(
            chunk_id=r.id,
            document_id=r.document_id,
            filename=r.filename,
            chunk_index=r.chunk_index,
            page=r.page,
            content=r.content,
            score=float(r.score),
        )
        for r in rows
    ]


def semantic_search(
    db: Session, query: str, top_k: int, document_ids: list[uuid.UUID] | None = None
) -> list[SearchHit]:
    qvec = _vector_literal(embed_query(query))
    sql = text(
        f"""
        SELECT c.id, c.document_id, d.filename, c.chunk_index, c.page, c.content,
               1 - (c.embedding <=> CAST(:qvec AS vector)) AS score
        FROM chunks c
        JOIN documents d ON d.id = c.document_id
        WHERE d.status = 'indexed' {_doc_filter(document_ids)}
        ORDER BY c.embedding <=> CAST(:qvec AS vector)
        LIMIT :k
        """
    )
    params: dict = {"qvec": qvec, "k": top_k}
    if document_ids:
        sql = sql.bindparams(bindparam("ids", expanding=True))
        params["ids"] = document_ids
    return _rows_to_hits(db.execute(sql, params).all())


def hybrid_search(
    db: Session, query: str, top_k: int, document_ids: list[uuid.UUID] | None = None
) -> list[SearchHit]:
    qvec = _vector_literal(embed_query(query))
    candidates = max(top_k * 4, 20)
    sql = text(
        f"""
        WITH q AS (
            SELECT CAST(:qvec AS vector) AS v,
                   websearch_to_tsquery('simple', :qtext) AS tsq
        ),
        sem AS (
            SELECT c.id, row_number() OVER (ORDER BY c.embedding <=> q.v) AS rnk
            FROM chunks c
            JOIN documents d ON d.id = c.document_id
            CROSS JOIN q
            WHERE d.status = 'indexed' {_doc_filter(document_ids)}
            ORDER BY c.embedding <=> q.v
            LIMIT :n
        ),
        fts AS (
            SELECT c.id,
                   row_number() OVER (
                       ORDER BY ts_rank_cd(to_tsvector('simple', c.content), q.tsq) DESC
                   ) AS rnk
            FROM chunks c
            JOIN documents d ON d.id = c.document_id
            CROSS JOIN q
            WHERE d.status = 'indexed'
              AND to_tsvector('simple', c.content) @@ q.tsq
              {_doc_filter(document_ids)}
            LIMIT :n
        ),
        fused AS (
            SELECT COALESCE(sem.id, fts.id) AS id,
                   COALESCE(1.0 / (:rrf_k + sem.rnk), 0)
                 + COALESCE(1.0 / (:rrf_k + fts.rnk), 0) AS score
            FROM sem FULL OUTER JOIN fts ON sem.id = fts.id
        )
        SELECT c.id, c.document_id, d.filename, c.chunk_index, c.page, c.content, f.score
        FROM fused f
        JOIN chunks c ON c.id = f.id
        JOIN documents d ON d.id = c.document_id
        ORDER BY f.score DESC, c.id
        LIMIT :k
        """
    )
    params: dict = {"qvec": qvec, "qtext": query, "n": candidates, "k": top_k, "rrf_k": RRF_K}
    if document_ids:
        sql = sql.bindparams(bindparam("ids", expanding=True))
        params["ids"] = document_ids
    hits = _rows_to_hits(db.execute(sql, params).all())
    # Express RRF scores relative to the best hit so they are comparable to cosine similarity.
    if hits:
        best = hits[0].score or 1.0
        for h in hits:
            h.score = round(h.score / best, 4)
    return hits


def search(
    db: Session, query: str, top_k: int, mode: str, document_ids: list[uuid.UUID] | None
) -> list[SearchHit]:
    if mode == "semantic":
        return semantic_search(db, query, top_k, document_ids)
    return hybrid_search(db, query, top_k, document_ids)
