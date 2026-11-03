import os
from typing import Literal

import httpx
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field

from app.services.hf_video_service import HuggingFaceVideoService

app = FastAPI(title="Tata Motion Studio API", version="0.3.0")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

hf_service = HuggingFaceVideoService(
    token=os.getenv("HF_TOKEN", ""),
    model_name=os.getenv("HF_TEXT_TO_VIDEO_MODEL", ""),
)
GEMINI_API_KEY = os.getenv("GEMINI_API_KEY", "")
GEMINI_MODEL = os.getenv("GEMINI_MODEL", "gemini-2.5-flash")
GEMINI_API_BASE = os.getenv("GEMINI_API_BASE", "https://generativelanguage.googleapis.com/v1beta")


class ChatMessage(BaseModel):
    role: Literal["user", "model"]
    text: str = Field(..., min_length=1, max_length=6000)


class AssistantChatRequest(BaseModel):
    messages: list[ChatMessage] = Field(..., min_length=1, max_length=24)


class VideoGenerationRequest(BaseModel):
    prompt: str = Field(..., min_length=1, max_length=2000)
    duration_seconds: int = Field(default=15, ge=3, le=30)
    style: str = "cinematic"
    aspect_ratio: str = "16:9"


SYSTEM_INSTRUCTION = """You are Tata Motion Studio's AI assistant, an expert motion-graphics director and scene-script author. Help users design videos from their graphics, improve animation ideas, and revise safe structured scene plans. Be practical and concise. When asked for a scene plan, return readable JSON data or a clearly formatted scene list; never provide executable code, shell commands, or instructions to run arbitrary code. Explain that the app's local renderer currently supports a graphic-backed title card with zoom and narration, and do not claim unsupported timeline actions have rendered."""


@app.get("/health")
def health_check():
    return {
        "status": "ok",
        "service": "tata-motion-studio",
        "assistant_configured": bool(GEMINI_API_KEY),
        "assistant_provider": "gemini" if GEMINI_API_KEY else "not-configured",
    }


@app.post("/api/assistant/chat")
async def assistant_chat(request: AssistantChatRequest):
    if not GEMINI_API_KEY:
        raise HTTPException(
            status_code=503,
            detail="AI assistant is not configured. Set GEMINI_API_KEY in the backend environment and restart the API.",
        )

    contents = [
        {"role": message.role, "parts": [{"text": message.text.strip()}]}
        for message in request.messages
    ]
    # Gemini conversation contents should start with a user turn.
    while contents and contents[0]["role"] == "model":
        contents.pop(0)
    if not contents:
        raise HTTPException(status_code=422, detail="A user message is required.")

    url = f"{GEMINI_API_BASE.rstrip('/')}/models/{GEMINI_MODEL}:generateContent"
    payload = {
        "system_instruction": {"parts": [{"text": SYSTEM_INSTRUCTION}]},
        "contents": contents,
        "generationConfig": {"temperature": 0.7, "maxOutputTokens": 1800},
    }
    try:
        async with httpx.AsyncClient(timeout=45.0) as client:
            response = await client.post(
                url,
                headers={"x-goog-api-key": GEMINI_API_KEY, "Content-Type": "application/json"},
                json=payload,
            )
            response.raise_for_status()
            data = response.json()
        candidates = data.get("candidates", [])
        parts = candidates[0].get("content", {}).get("parts", []) if candidates else []
        answer = "\n".join(part.get("text", "") for part in parts if part.get("text"))
        if not answer.strip():
            raise HTTPException(status_code=502, detail="Gemini returned an empty response. Please try again.")
        return {"success": True, "reply": answer.strip(), "model": GEMINI_MODEL}
    except HTTPException:
        raise
    except httpx.HTTPStatusError as exc:
        # Do not return upstream request details, which may contain sensitive metadata.
        raise HTTPException(status_code=502, detail=f"Gemini request failed with HTTP {exc.response.status_code}.") from exc
    except (httpx.HTTPError, ValueError, KeyError, IndexError, TypeError) as exc:
        raise HTTPException(status_code=502, detail="Could not reach Gemini. Check backend connectivity and try again.") from exc


@app.post("/api/generate-video")
async def generate_video(request: VideoGenerationRequest):
    if not request.prompt.strip():
        raise HTTPException(status_code=400, detail="Prompt cannot be empty.")
    if request.aspect_ratio not in {"16:9", "9:16", "1:1"}:
        raise HTTPException(status_code=422, detail="aspect_ratio must be 16:9, 9:16, or 1:1")
    return await hf_service.generate_video(
        prompt=request.prompt,
        duration_seconds=request.duration_seconds,
        style=request.style,
        aspect_ratio=request.aspect_ratio,
    )
