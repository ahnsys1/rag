from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from ..db import get_db
from ..llm import LLMUnavailable, generate_answer
from ..schemas import AskRequest, AskResponse, SearchRequest, SearchResponse
from ..services.retrieval import search

router = APIRouter(prefix="/api", tags=["search"])


@router.post("/search", response_model=SearchResponse)
def search_documents(req: SearchRequest, db: Session = Depends(get_db)) -> SearchResponse:
    hits = search(db, req.query, req.top_k, req.mode, req.document_ids)
    return SearchResponse(query=req.query, mode=req.mode, hits=hits)


@router.post("/ask", response_model=AskResponse)
def ask(req: AskRequest, db: Session = Depends(get_db)) -> AskResponse:
    hits = search(db, req.query, req.top_k, req.mode, req.document_ids)
    try:
        answer, model = generate_answer(req.query, hits)
    except LLMUnavailable as exc:
        raise HTTPException(status.HTTP_503_SERVICE_UNAVAILABLE, str(exc)) from exc
    return AskResponse(question=req.query, answer=answer, model=model, sources=hits)
