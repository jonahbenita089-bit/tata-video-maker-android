# Tata Motion Studio

Android motion-graphics video maker built with Kotlin/Jetpack Compose and a FastAPI backend. Describe a video, attach a graphic, chat with the Gemini assistant, and render an MP4 locally.

## Enable Gemini assistant

1. In `backend`, install dependencies and start the API:

   ```bash
   python -m venv .venv
   source .venv/bin/activate  # Windows: .venv\\Scripts\\activate
   pip install -r requirements.txt
   ```

2. Set the key only in the backend environment (never in Android code or the repository):

   ```bash
   export GEMINI_API_KEY="your-key"
   export GEMINI_MODEL="gemini-2.5-flash"  # optional; this is the default
   uvicorn app.main:app --host 0.0.0.0 --port 8000
   ```

   On Windows PowerShell, use `$env:GEMINI_API_KEY="your-key"` before running Uvicorn. For deployment, configure `GEMINI_API_KEY` as a secret in the hosting provider's environment settings.

3. Open `android-app` in Android Studio and run on an emulator. The default development API URL is `http://10.0.2.2:8000`. For a physical device or deployed HTTPS backend, set the Gradle property `API_BASE_URL`, for example `-PAPI_BASE_URL=https://your-api.example.com`.

The app's `/api/assistant/chat` endpoint sends recent conversation turns to Gemini. If the key is missing, the backend reports that setup is required; it does not silently pretend a local reply is AI-generated. The Android app keeps the key on the server.

## Video generation

The existing Hugging Face video endpoint is retained. Configure `HF_TOKEN` and `HF_TEXT_TO_VIDEO_MODEL` in the backend if using it. Local FFmpeg rendering remains a separate on-device fallback.

## Security note

Do not commit `.env` files or API keys. Use HTTPS for any deployed backend. Cleartext HTTP is enabled in the Android manifest for local emulator development; remove it and use HTTPS for production builds.
