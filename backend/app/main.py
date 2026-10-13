import os
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel

from app.services.hf_video_service import HuggingFaceVideoService

app = FastAPI(title="Tata Video Maker API", version="0.1.0")

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


class VideoGenerationRequest(BaseModel):
    prompt: str
    duration_seconds: int = 15
    style: str = "cinematic"
    aspect_ratio: str = "16:9"


@app.get("/health")
def health_check():
    return {"status": "ok", "service": "tata-video-maker"}


@app.post("/api/generate-video")
async def generate_video(request: VideoGenerationRequest):
    if not request.prompt or not request.prompt.strip():
        return {"success": False, "error": "Prompt cannot be empty."}

    result = await hf_service.generate_video(
        prompt=request.prompt,
        duration_seconds=request.duration_seconds,
        style=request.style,
        aspect_ratio=request.aspect_ratio,
    )

    return result
