# Tata Video Maker

Tata Video Maker is a free Android + backend app concept for generating AI videos from text prompts using a Hugging Face text-to-video model. The recommended architecture keeps the Android app lightweight and moves the actual AI rendering to a backend service so real video generation can work reliably.

## Recommended architecture

- Android app: Kotlin + Jetpack Compose
- Backend: Python + FastAPI
- AI model: Hugging Face text-to-video model
- Output: MP4 or frames returned to the Android app
- Theme: black / dark UI
- Monetization: none, free forever

## Why this architecture

A commercial-quality text-to-video model is usually too large and too resource-heavy to run directly on a mobile device. The best approach is:

1. Android app handles the UI and user flow.
2. Backend receives the script or prompt.
3. Backend calls Hugging Face text-to-video model.
4. Backend returns generated video or rendered frames.
5. Android app shows project previews and exports the final video.

## Included in this repo

- `android-app/` — Jetpack Compose starter app with black theme and screens:
  - Projects
  - Menu
  - Settings
  - Create
  - New Chat
- `backend/` — FastAPI service that is ready to integrate with a Hugging Face text-to-video model.
- `README.md` — setup and usage guide

## App flow

- User opens the app.
- Creates or selects a project.
- Uses New Chat to write a prompt or script.
- Prefers assistant-generated storyboard scene list.
- Hits Create and sends data to backend.
- Backend calls the model.
- Output video is downloaded and shown in the project preview.
- User can export the video or continue editing.

## Backend setup

```bash
cd backend
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

Set environment variables:

```bash
export HF_TOKEN=your_huggingface_token
export HF_TEXT_TO_VIDEO_MODEL=your-text-to-video-model
```

The model name should be a valid Hugging Face text-to-video model from the list at:
https://huggingface.co/models?pipeline_tag=text-to-video

## Android app setup

Open the `android-app/` folder in Android Studio and sync Gradle. The app is a starter UI only; it connects to the backend through a service layer you can add later.

## Black theme design tokens

- Background: `#050505`
- Surface: `#111111`
- Card: `#181818`
- Accent: `#00B7FF`
- Secondary accent: `#00D5A3`
- Text: `#FFFFFF`, `#BDBDBD`

## No-payment approach

- No subscriptions
- No upgrade flow
- No billing screen
- Free usage model by design
- Actual AI generation depends on the selected Hugging Face model and backend hosting costs

## Notes

This repo is intentionally a strong starter foundation rather than a complete production app. The backend service includes a mock/fallback mode when no model token is configured, which lets the project run safely during local development without billing or lock-in.

## Next steps

- Connect the Android UI to the backend generation endpoint.
- Add start/render progress indicators.
- Store project metadata and generated outputs locally.
- Add a proper AI assistant prompt pipeline with scene generation.
- Replace the mock backend with real model inference for your chosen model.

