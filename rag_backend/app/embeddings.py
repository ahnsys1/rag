"""Local embedding model (ONNX via fastembed) shared across requests."""

import threading
from collections.abc import Sequence

from fastembed import TextEmbedding

from .config import get_settings

_lock = threading.Lock()
_model: TextEmbedding | None = None


def get_model() -> TextEmbedding:
    global _model
    if _model is None:
        with _lock:
            if _model is None:
                s = get_settings()
                _model = TextEmbedding(model_name=s.embedding_model, cache_dir=s.embedding_cache_dir)
    return _model


def embed_passages(texts: Sequence[str], batch_size: int = 32) -> list[list[float]]:
    if not texts:
        return []
    return [vec.tolist() for vec in get_model().embed(list(texts), batch_size=batch_size)]


def embed_query(text: str) -> list[float]:
    return next(iter(get_model().query_embed(text))).tolist()
