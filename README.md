import re
import shutil
import subprocess
from pathlib import Path
from typing import Dict, Any


class LocalVideoService:
    def __init__(self, output_dir: Path):
        self.output_dir = output_dir
        self.output_dir.mkdir(parents=True, exist_ok=True)

    @staticmethod
    def _sanitize_filename(text: str) -> str:
        text = text.strip().lower()
        text = re.sub(r"[^a-z0-9]+", "-", text)
        text = text.strip("-")
        return text or "video"

    def generate_video(
        self,
        prompt: str,
        duration_seconds: int = 15,
        style: str = "cinematic",
        aspect_ratio: str = "16:9",
    ) -> Dict[str, Any]:
        ffmpeg_path = shutil.which("ffmpeg")
        if not ffmpeg_path:
            return {
                "success": False,
                "error": "ffmpeg not installed. Please install ffmpeg to generate local MP4 output.",
            }

        prompt_clean = self._sanitize_filename(prompt)
        file_name = f"{prompt_clean}-{duration_seconds}s.mp4"
        output_path = self.output_dir / file_name

        # Generate a real MP4 using ffmpeg with a color background and text overlay.
        # This creates a valid MP4 file immediately and works without any paid API.
        ffmpeg_cmd = [
            ffmpeg_path,
            "-y",
            "-f",
            "lavfi",
            "-i",
            f"color=c=#050505:s=1280x720:d={duration_seconds}",
            "-vf",
            f"drawtext=fontcolor=white:fontsize=42:text='{style.upper()} | {prompt[:60]}':x=(w-text_w)/2:y=(h-text_h)/2",
            "-c:v",
            "libx264",
            "-pix_fmt",
            "yuv420p",
            "-t",
            str(duration_seconds),
            str(output_path),
        ]

        try:
            subprocess.run(ffmpeg_cmd, check=True, capture_output=True, text=True)
            return {
                "success": True,
                "file_name": file_name,
                "output_path": str(output_path),
                "message": "MP4 generated locally successfully.",
            }
        except subprocess.CalledProcessError as exc:
            return {
                "success": False,
                "error": "FFmpeg failed to generate output.",
                "stderr": exc.stderr,
            }
