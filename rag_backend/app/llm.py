"""Answer generation over retrieved passages (Ollama, or disabled)."""

import httpx

from .config import get_settings
from .schemas import SearchHit

SYSTEM_PROMPT = (
    "Jsi asistent, který odpovídá výhradně na základě poskytnutých úryvků dokumentů. "
    "Odpovídej ve stejném jazyce, v jakém je položena otázka. "
    "Pokud odpověď v úryvcích není, řekni, že ji v dokumentech nelze najít. "
    "U tvrzení uváděj odkaz na zdroj ve tvaru [n] podle čísla úryvku."
)


class LLMUnavailable(RuntimeError):
    pass


def build_prompt(question: str, hits: list[SearchHit]) -> str:
    context = "\n\n".join(
        f"[{i}] ({h.filename}" + (f", strana {h.page}" if h.page else "") + f")\n{h.content}"
        for i, h in enumerate(hits, start=1)
    )
    return f"Úryvky dokumentů:\n\n{context}\n\nOtázka: {question}\n\nOdpověď:"


def generate_answer(question: str, hits: list[SearchHit]) -> tuple[str, str | None]:
    s = get_settings()
    if s.llm_provider.lower() == "none":
        return _fallback_answer(hits), None
    if s.llm_provider.lower() != "ollama":
        raise LLMUnavailable(f"Unknown LLM_PROVIDER '{s.llm_provider}'")

    payload = {
        "model": s.ollama_model,
        "stream": False,
        "messages": [
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user", "content": build_prompt(question, hits)},
        ],
        "options": {"temperature": 0.1},
    }
    try:
        with httpx.Client(timeout=s.ollama_timeout_seconds) as client:
            resp = client.post(f"{s.ollama_url.rstrip('/')}/api/chat", json=payload)
            resp.raise_for_status()
    except httpx.HTTPError as exc:
        raise LLMUnavailable(f"Ollama request failed: {exc}") from exc

    data = resp.json()
    return data.get("message", {}).get("content", "").strip(), s.ollama_model


def _fallback_answer(hits: list[SearchHit]) -> str:
    if not hits:
        return "V indexovaných dokumentech nebyly nalezeny žádné relevantní pasáže."
    return (
        "Generování odpovědi (LLM) není nakonfigurováno. Nejrelevantnější nalezené pasáže:\n\n"
        + "\n\n".join(f"[{i}] {h.content}" for i, h in enumerate(hits, start=1))
    )
