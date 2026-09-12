import uuid
from pathlib import PurePosixPath
from urllib.parse import quote

from fastapi import APIRouter, BackgroundTasks, Depends, HTTPException, Response, UploadFile, status
from sqlalchemy import select
from sqlalchemy.orm import Session

from ..config import get_settings
from ..db import get_db
from ..ingestion.loaders import SUPPORTED_EXTENSIONS
from ..models import Document
from ..schemas import DocumentOut, UploadResponse
from ..services import indexing

router = APIRouter(prefix="/api/documents", tags=["documents"])


def _safe_filename(name: str | None) -> str:
    base = PurePosixPath((name or "").replace("\\", "/")).name.strip()
    if not base or base in {".", ".."}:
        raise HTTPException(status.HTTP_400_BAD_REQUEST, "Missing file name")
    return base[:512]


@router.post("", response_model=UploadResponse, status_code=status.HTTP_202_ACCEPTED)
async def upload_document(
    file: UploadFile,
    background: BackgroundTasks,
    db: Session = Depends(get_db),
) -> UploadResponse:
    settings = get_settings()
    filename = _safe_filename(file.filename)
    if PurePosixPath(filename).suffix.lower() not in SUPPORTED_EXTENSIONS:
        raise HTTPException(
            status.HTTP_415_UNSUPPORTED_MEDIA_TYPE,
            f"Unsupported file type. Allowed: {', '.join(sorted(SUPPORTED_EXTENSIONS))}",
        )

    limit = settings.max_upload_mb * 1024 * 1024
    data = await file.read(limit + 1)
    if len(data) > limit:
        raise HTTPException(
            status.HTTP_413_REQUEST_ENTITY_TOO_LARGE, f"File exceeds {settings.max_upload_mb} MB"
        )
    if not data:
        raise HTTPException(status.HTTP_400_BAD_REQUEST, "Empty file")

    existing = indexing.find_by_hash(db, indexing.sha256_hex(data))
    if existing is not None:
        if not existing.content_data:
            existing.content_data = data
            db.commit()
        return UploadResponse(document=DocumentOut.model_validate(existing), duplicate=True)

    doc = indexing.register_document(db, filename, file.content_type, data)
    background.add_task(indexing.index_document, doc.id, filename, data)
    return UploadResponse(document=DocumentOut.model_validate(doc))


@router.get("", response_model=list[DocumentOut])
def list_documents(db: Session = Depends(get_db)) -> list[Document]:
    return list(db.scalars(select(Document).order_by(Document.created_at.desc())).all())


@router.get("/{document_id}", response_model=DocumentOut)
def get_document(document_id: uuid.UUID, db: Session = Depends(get_db)) -> Document:
    doc = db.get(Document, document_id)
    if doc is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Document not found")
    return doc


@router.get("/{document_id}/download")
def download_document(document_id: uuid.UUID, db: Session = Depends(get_db)) -> Response:
    doc = db.get(Document, document_id)
    if doc is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Document not found")
    if not doc.content_data:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Original file is not available")
    return Response(
        content=doc.content_data,
        media_type=doc.content_type or "application/octet-stream",
        headers={"Content-Disposition": f"attachment; filename*=UTF-8''{quote(doc.filename)}"},
    )


@router.delete("/{document_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_document(document_id: uuid.UUID, db: Session = Depends(get_db)) -> None:
    doc = db.get(Document, document_id)
    if doc is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Document not found")
    db.delete(doc)
    db.commit()
