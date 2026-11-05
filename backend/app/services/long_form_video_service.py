import os

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel

from app.services.long_form_video_service import LongFormVideoService

app = FastAPI(title="Tata Video Maker API", version="0.1.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

long_form_service = LongFormVideoService()


class VideoGenerationRequest(BaseModel):
    prompt: str
    duration_seconds: int = 15
    style: str = "cinematic"
    aspect_ratio: str = "16:9"


class LongFormProjectRequest(BaseModel):
    prompt: str
    total_duration_minutes: int = 100
    scene_count: int = 1000
    language: str = "English"
    voice_tone: str = "cinematic"
    style: str = "cinematic"
    aspect_ratio: str = "16:9"


@app.get("/health")
def health_check():
    return {"status": "ok", "service": "tata-video-maker"}


@app.post("/api/projects/long-form")
async def generate_long_form_project(request: LongFormProjectRequest):
    if not request.prompt or not request.prompt.strip():
        return {"success": False, "error": "Prompt cannot be empty."}

    try:
        project = long_form_service.generate_project(
            prompt=request.prompt,
            total_duration_minutes=request.total_duration_minutes,
            scene_count=request.scene_count,
            language=request.language,
            voice_tone=request.voice_tone,
            style=request.style,
            aspect_ratio=request.aspect_ratio,
        )
        return {"success": True, "project": project}
    except ValueError as exc:
        return {"success": False, "error": str(exc)}


@app.post("/api/generate-video")
async def generate_video(request: VideoGenerationRequest):
    if not request.prompt or not request.prompt.strip():
        return {"success": False, "error": "Prompt cannot be empty."}

    return {
        "success": True,
        "message": "Long-form project mode is active. Use /api/projects/long-form to generate a 100-minute storyboard with up to 1000 scenes.",
        "prompt": request.prompt,
        "duration_seconds": request.duration_seconds,
        "style": request.style,
        "aspect_ratio": request.aspect_ratio,
        "mode": "scene-planning",
    }
