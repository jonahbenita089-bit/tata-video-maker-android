from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse
from pydantic import BaseModel
from pathlib import Path
import os

from app.services.hf_video_service import HuggingFaceVideoService
from app.services.local_video_service import LocalVideoService

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
local_video_service = LocalVideoService(output_dir=Path(__file__).resolve().parent.parent / "generated_videos")


class VideoGenerationRequest(BaseModel):
    prompt: str
    duration_seconds: int = 15
    style: str = "cinematic"
    aspect_ratio: str = "16:9"


@app.get("/health")
def health_check():
    return {"status": "ok", "service": "tata-video-maker"}


@app.get("/generated/{filename}")
def get_generated_video(filename: str):
    file_path = local_video_service.output_dir / filename
    if not file_path.exists():
        return {"success": False, "error": "Video not found."}
    return FileResponse(file_path)


@app.post("/api/generate-video")
async def generate_video(request: VideoGenerationRequest):
    if not request.prompt or not request.prompt.strip():
        return {"success": False, "error": "Prompt cannot be empty."}

    # Prefer real Hugging Face model if configured.
    if hf_service.token and hf_service.model_name:
        result = await hf_service.generate_video(
            prompt=request.prompt,
            duration_seconds=request.duration_seconds,
            style=request.style,
            aspect_ratio=request.aspect_ratio,
        )
        if result.get("success"):
            return result

    # Fallback: create a real local MP4 video without payment.
    local_result = local_video_service.generate_video(
        prompt=request.prompt,
        duration_seconds=request.duration_seconds,
        style=request.style,
        aspect_ratio=request.aspect_ratio,
    )

    if not local_result.get("success"):
        return local_result

    return {
        "success": True,
        "is_mock": False,
        "is_local_fallback": True,
        "message": "Video generated successfully using local fallback renderer.",
        "prompt": request.prompt,
        "duration_seconds": request.duration_seconds,
        "style": request.style,
        "aspect_ratio": request.aspect_ratio,
        "video_url": f"http://localhost:8000/generated/{local_result['file_name']}",
        "file_name": local_result["file_name"],
        "notes": "HF model path is available when HF_TOKEN and HF_TEXT_TO_VIDEO_MODEL are set; otherwise this app creates a real MP4 locally using ffmpeg."
    }
