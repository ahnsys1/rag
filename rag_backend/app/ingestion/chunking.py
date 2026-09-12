"""Split extracted text into overlapping chunks suited for embedding."""

import re
from dataclasses import dataclass

from .loaders import ExtractedDocument

_SEPARATORS = ["\n\n", "\n", ". ", "! ", "? ", "; ", ", ", " "]


@dataclass
class TextChunk:
    index: int
    page: int | None
    content: str


def _split_recursive(text: str, chunk_size: int, separators: list[str]) -> list[str]:
    if len(text) <= chunk_size:
        return [text]
    if not separators:
        return [text[i : i + chunk_size] for i in range(0, len(text), chunk_size)]

    sep, rest = separators[0], separators[1:]
    parts = [p for p in text.split(sep) if p]
    if len(parts) <= 1:
        return _split_recursive(text, chunk_size, rest)

    pieces: list[str] = []
    for part in parts:
        candidate = part + sep
        if len(candidate) > chunk_size:
            pieces.extend(_split_recursive(candidate, chunk_size, rest))
        else:
            pieces.append(candidate)
    return pieces


def _merge(pieces: list[str], chunk_size: int, overlap: int) -> list[str]:
    chunks: list[str] = []
    current = ""
    for piece in pieces:
        if len(current) + len(piece) <= chunk_size:
            current += piece
            continue
        if current.strip():
            chunks.append(current.strip())
        # Carry the tail of the previous chunk so context spans boundaries.
        tail = current[-overlap:] if overlap and current else ""
        tail = tail[tail.find(" ") + 1 :] if " " in tail else tail
        current = tail + piece
    if current.strip():
        chunks.append(current.strip())
    return chunks


def split_text(text: str, chunk_size: int = 900, overlap: int = 150) -> list[str]:
    if chunk_size <= 0:
        raise ValueError("chunk_size must be positive")
    if overlap < 0 or overlap >= chunk_size:
        raise ValueError("overlap must be in [0, chunk_size)")
    text = re.sub(r"[ \t]+\n", "\n", text).strip()
    if not text:
        return []
    pieces = _split_recursive(text, chunk_size, _SEPARATORS)
    return _merge(pieces, chunk_size, overlap)


def chunk_document(doc: ExtractedDocument, chunk_size: int, overlap: int) -> list[TextChunk]:
    chunks: list[TextChunk] = []
    for page in doc.pages:
        for content in split_text(page.text, chunk_size, overlap):
            chunks.append(TextChunk(index=len(chunks), page=page.number, content=content))
    return chunks
