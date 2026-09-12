import uuid
from datetime import datetime
from typing import Literal

from pydantic import BaseModel, Field

from .models import DocumentStatus


class DocumentOut(BaseModel):
    id: uuid.UUID
    filename: str
    content_type: str | None
    size_bytes: int
    status: DocumentStatus
    error: str | None
    chunk_count: int
    page_count: int | None
    created_at: datetime
    indexed_at: datetime | None

    model_config = {"from_attributes": True}


class UploadResponse(BaseModel):
    document: DocumentOut
    duplicate: bool = False


class SearchRequest(BaseModel):
    query: str = Field(min_length=1, max_length=2000)
    top_k: int = Field(default=5, ge=1, le=50)
    mode: Literal["semantic", "hybrid"] = "hybrid"
    document_ids: list[uuid.UUID] | None = None


class SearchHit(BaseModel):
    chunk_id: int
    document_id: uuid.UUID
    filename: str
    chunk_index: int
    page: int | None
    content: str
    score: float


class SearchResponse(BaseModel):
    query: str
    mode: str
    hits: list[SearchHit]


class AskRequest(SearchRequest):
    pass


class AskResponse(BaseModel):
    question: str
    answer: str
    model: str | None
    sources: list[SearchHit]


class HealthResponse(BaseModel):
    status: str
    embedding_model: str
    embedding_dim: int
    llm_provider: str
    llm_model: str | None
