import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from .config import get_settings
from .db import init_db
from .embeddings import get_model
from .routers import documents, search
from .schemas import HealthResponse

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s: %(message)s")


@asynccontextmanager
async def lifespan(_: FastAPI):
    init_db()
    get_model()  # download / load the embedding model once at startup
    yield


settings = get_settings()

app = FastAPI(
    title="RAG backend",
    description="Indexing of PDF/DOCX documents into PostgreSQL 18 + pgvector and semantic search.",
    version="0.1.0",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origin_list,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(documents.router)
app.include_router(search.router)


@app.get("/api/health", response_model=HealthResponse, tags=["system"])
def health() -> HealthResponse:
    llm_none = settings.llm_provider.lower() == "none"
    return HealthResponse(
        status="ok",
        embedding_model=settings.embedding_model,
        embedding_dim=settings.embedding_dim,
        llm_provider=settings.llm_provider,
        llm_model=None if llm_none else settings.ollama_model,
    )
