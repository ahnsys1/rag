"""Document ingestion pipeline: extract -> chunk -> embed -> store."""

import hashlib
import logging
import uuid
from datetime import UTC, datetime

from sqlalchemy import select
from sqlalchemy.orm import Session

from ..config import get_settings
from ..db import SessionLocal
from ..embeddings import embed_passages
from ..ingestion.chunking import chunk_document
from ..ingestion.loaders import extract
from ..models import Chunk, Document, DocumentStatus

log = logging.getLogger(__name__)


def sha256_hex(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def find_by_hash(db: Session, digest: str) -> Document | None:
    return db.scalar(select(Document).where(Document.sha256 == digest))


def register_document(
    db: Session, filename: str, content_type: str | None, data: bytes
) -> Document:
    doc = Document(
        filename=filename,
        content_type=content_type,
        size_bytes=len(data),
        sha256=sha256_hex(data),
        status=DocumentStatus.processing,
    )
    db.add(doc)
    db.commit()
    db.refresh(doc)
    return doc


def index_document(document_id: uuid.UUID, filename: str, data: bytes) -> None:
    """Runs in a background task with its own session."""
    settings = get_settings()
    with SessionLocal() as db:
        doc = db.get(Document, document_id)
        if doc is None:
            return
        try:
            extracted = extract(filename, data)
            chunks = chunk_document(extracted, settings.chunk_size, settings.chunk_overlap)
            if not chunks:
                raise ValueError("No extractable text found in the document")

            vectors = embed_passages([c.content for c in chunks])
            db.add_all(
                Chunk(
                    document_id=doc.id,
                    chunk_index=c.index,
                    page=c.page,
                    content=c.content,
                    embedding=vec,
                )
                for c, vec in zip(chunks, vectors, strict=True)
            )
            doc.chunk_count = len(chunks)
            doc.page_count = extracted.page_count
            doc.status = DocumentStatus.indexed
            doc.indexed_at = datetime.now(UTC)
            doc.error = None
            db.commit()
            log.info("Indexed %s (%d chunks)", filename, len(chunks))
        except Exception as exc:  # noqa: BLE001 - surface any failure on the document row
            db.rollback()
            doc = db.get(Document, document_id)
            if doc is not None:
                doc.status = DocumentStatus.failed
                doc.error = str(exc)[:2000]
                db.commit()
            log.exception("Indexing failed for %s", filename)
