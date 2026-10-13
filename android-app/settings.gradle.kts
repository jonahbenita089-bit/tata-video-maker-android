import os
from typing import Any, Dict

import httpx


class HuggingFaceVideoService:
    def __init__(self, token: str, model_name: str):
        self.token = token or os.getenv("HF_TOKEN", "")
        self.model_name = model_name or os.getenv("HF_TEXT_TO_VIDEO_MODEL", "")
        self.base_url = "https://api-inference.huggingface.co/models"

    async def generate_video(
        self,
        prompt: str,
        duration_seconds: int,
        style: str,
        aspect_ratio: str,
    ) -> Dict[str, Any]:
        if not self.token or not self.model_name:
            return {
                "success": False,
                "is_mock": True,
                "message": "No Hugging Face token or model configured. Backend is in mock mode for local development.",
                "prompt": prompt,
                "duration_seconds": duration_seconds,
                "style": style,
                "aspect_ratio": aspect_ratio,
                "video_url": None,
            }

        headers = {
            "Authorization": f"Bearer {self.token}",
            "Content-Type": "application/json",
        }

        payload = {
            "inputs": prompt,
            "parameters": {
                "num_frames": max(8, int(duration_seconds * 4)),
                "num_inference_steps": 30,
                "guidance_scale": 7.5,
                "width": 768,
                "height": 432,
            },
            "options": {
                "wait_for_model": True,
            },
        }

        try:
            async with httpx.AsyncClient(timeout=300) as client:
                response = await client.post(
                    f"{self.base_url}/{self.model_name}",
                    headers=headers,
                    json=payload,
                )
                response.raise_for_status()
                data = response.json()

                if isinstance(data, dict) and "error" in data:
                    return {"success": False, "error": data["error"]}

                return {
                    "success": True,
                    "is_mock": False,
                    "message": "Video generation request accepted.",
                    "prompt": prompt,
                    "duration_seconds": duration_seconds,
                    "style": style,
                    "aspect_ratio": aspect_ratio,
                    "result": data,
                }
        except Exception as exc:  # pragma: no cover - defensive branch
            return {
                "success": False,
                "is_mock": True,
                "error": f"Model request failed: {str(exc)}",
                "message": "Model request failed. Check your Hugging Face token, model name, and inference endpoint.",
            }
