# RAG – indexace dokumentů a sémantické vyhledávání

Retrieval-Augmented Generation systém: nahrané dokumenty (**PDF, DOCX, TXT, MD**) se rozdělí
na úryvky, převedou na vektory a uloží do **PostgreSQL 18 s rozšířením pgvector**. Nad indexem
běží sémantické / hybridní vyhledávání a volitelně generování odpovědí lokálním LLM (Ollama).

```
rag/
├── docker-compose.yml     # db (Postgres 18 + pgvector), backend, frontend, ollama
├── rag_backend/           # Python 3.12 · FastAPI · SQLAlchemy · pgvector · fastembed
└── rag_frontend/          # Angular 22 (standalone komponenty, signals)
```

## Proč PostgreSQL + pgvector

- Jedna open-source databáze pro metadata dokumentů, text úryvků **i** vektory – žádná
  synchronizace mezi dvěma úložišti, transakce, mazání přes `ON DELETE CASCADE`.
- HNSW index (`vector_cosine_ops`) pro rychlé ANN vyhledávání.
- Vestavěný fulltext (`tsvector`) → **hybridní vyhledávání** (vektor + BM25-like ranking
  sloučené pomocí Reciprocal Rank Fusion), které výrazně pomáhá u přesných termínů,
  čísel a názvů.

## Architektura

```mermaid
flowchart LR
  UI[Angular 22] -->|/api| API[FastAPI]
  API -->|extract| L[pypdf / python-docx]
  L --> C[chunking 900/150]
  C --> E[fastembed<br/>multilingual MiniLM 384d]
  E --> PG[(PostgreSQL 18<br/>pgvector HNSW + GIN fulltext)]
  API -->|search / ask| PG
  API -->|generate| O[Ollama LLM]
```

Pipeline indexace: `upload → sha256 dedup → extrakce textu (po stranách) → rekurzivní
chunking s překryvem → embedding → INSERT do chunks` – běží jako background task,
UI stav dokumentu polluje (`processing → indexed | failed`).

## Rychlý start (Docker)

```bash
cd ~/Desktop/rag
cp .env.example .env            # volitelné
docker compose up -d --build
```

| Služba    | URL                          |
|-----------|------------------------------|
| Frontend  | http://localhost:4300        |
| Backend   | http://localhost:8000/docs   |
| Postgres  | localhost:55433 (rag / rag)  |
| Ollama    | http://localhost:11434       |

Pro generování odpovědí stáhněte model (jednorázově):

```bash
docker compose exec ollama ollama pull llama3.2:1b   # jen pro generování odpovědí
```

Bez LLM lze systém používat jen pro vyhledávání; případně nastavte `LLM_PROVIDER=none`,
pak `/api/ask` vrací nalezené pasáže místo generované odpovědi.

Při prvním startu backend stáhne embedding model (~120 MB) do volume `rag_models`.

## Vývoj lokálně

```bash
# databáze
docker compose up -d db

# backend
cd rag_backend
python3 -m venv .venv && .venv/bin/pip install -e ".[dev]"
cp .env.example .env
.venv/bin/uvicorn app.main:app --reload          # http://localhost:8000
.venv/bin/python -m pytest                       # unit testy

# frontend (proxy /api -> localhost:8000)
cd ../rag_frontend
npm install
npm start                                        # http://localhost:4200
```

## REST API

| Metoda | Cesta                    | Popis                                              |
|--------|--------------------------|----------------------------------------------------|
| POST   | `/api/documents`         | multipart upload (`file`) → 202, indexace na pozadí |
| GET    | `/api/documents`         | seznam dokumentů se stavem                          |
| GET    | `/api/documents/{id}`    | detail                                              |
| DELETE | `/api/documents/{id}`    | smazání dokumentu i úryvků                          |
| POST   | `/api/search`            | `{query, top_k, mode: semantic\|hybrid, document_ids?}` |
| POST   | `/api/ask`               | stejný vstup, vrací `answer` + `sources`            |
| GET    | `/api/health`            | model, dimenze, LLM                                 |

```bash
curl -F "file=@smlouva.pdf" http://localhost:8000/api/documents
curl -X POST http://localhost:8000/api/search -H 'content-type: application/json' \
     -d '{"query":"termín dokončení díla","top_k":5,"mode":"hybrid"}'
```

## Konfigurace (env)

| Proměnná              | Default                                                    |
|-----------------------|------------------------------------------------------------|
| `DATABASE_URL`        | `postgresql+psycopg://rag:rag@localhost:55433/rag`         |
| `EMBEDDING_MODEL`     | `sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2` |
| `EMBEDDING_DIM`       | `384` (musí odpovídat modelu; při změně smažte volume db)  |
| `CHUNK_SIZE` / `CHUNK_OVERLAP` | `900` / `150` znaků                               |
| `LLM_PROVIDER`        | `ollama` \| `none`                                         |
| `OLLAMA_URL` / `OLLAMA_MODEL` | `http://localhost:11434` / `llama3.2:1b`           |
| `MAX_UPLOAD_MB`       | `50`                                                       |
| `CORS_ORIGINS`        | `http://localhost:4200`                                    |

Jiné modely podporované fastembed (např. `intfloat/multilingual-e5-large`, 1024 d) stačí
nastavit spolu s `EMBEDDING_DIM` a znovu zaindexovat.

## Datový model

```sql
documents(id uuid, filename, content_type, size_bytes, sha256 unique, status, error,
          chunk_count, page_count, metadata jsonb, created_at, indexed_at)
chunks(id bigserial, document_id → documents ON DELETE CASCADE, chunk_index, page,
       content text, embedding vector(384))
-- ix_chunks_embedding_hnsw : hnsw (embedding vector_cosine_ops)
-- ix_chunks_content_fts    : gin (to_tsvector('simple', content))
```

# spusteni

cd ~/Desktop/rag && docker compose up -d --build
docker compose exec ollama ollama pull llama3.2   # jen pro generování odpovědí


cd ~/Desktop/rag && docker compose up -d --build
docker compose exec ollama ollama pull llama3.2   # jen pro generování odpovědí











# RUNNING
cd /home/jan/Desktop/rag
docker compose up -d db

cd rag_backend
cp .env.example .env   # pouze pokud .env ještě neexistuje
uvicorn app.main:app --reload


cd /home/jan/Desktop/rag
docker compose up -d --build