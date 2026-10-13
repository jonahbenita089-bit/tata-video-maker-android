# Tata Video Maker

Tata Video Maker is a dark-themed Android + backend app for creating AI video from text prompts.

This repo uses the recommended architecture:
- Android app for UI and project management
- FastAPI backend for generation logic
- Hugging Face text-to-video model when configured
- Local ffmpeg fallback to produce a real MP4 even with no paid service

## What works now

The backend can generate a real local MP4 file without any paid model when no HF token/model is configured. This is useful for local development and a free MVP.

It also supports a real Hugging Face text-to-video model path if you set:
- HF_TOKEN
- HF_TEXT_TO_VIDEO_MODEL

## Architecture

- Android UI screens: Projects, Menu, Settings, Create, New Chat
- Backend API: POST /api/generate-video
- Local fallback video generator: ffmpeg
- AI model path: Hugging Face text-to-video

## Run backend

```bash
cd backend
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

Optional real model environment:

```bash
export HF_TOKEN=your_huggingface_token
export HF_TEXT_TO_VIDEO_MODEL=your-text-to-video-model
```

## Test generation

```bash
curl -X POST http://localhost:8000/api/generate-video \
  -H "Content-Type: application/json" \
  -d '{"prompt":"A cinematic skyline at sunset","duration_seconds":10,"style":"cinematic","aspect_ratio":"16:9"}'
```

You can then open the returned `video_url` in a browser.

## Android app

Open the `android-app` project in Android Studio and run it on an emulator or connected device.

The app posts to:

```text
http://10.0.2.2:8000/api/generate-video
```

On a real device, replace with your machine IP.

## Notes

- This is a free/no-payment starter project.
- Local fallback generation works without billing.
- If you want stronger AI video models, configure a valid HF model and token.
- The result is intended to be a real MP4 video, not only a mock response.

