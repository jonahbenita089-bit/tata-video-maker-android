# Tata Motion Studio

Android motion-graphics video maker built with Kotlin/Jetpack Compose and a FastAPI backend. Chat is sent to Gemini from the backend; the API key is never stored in the Android app.

## Run the backend

```bash
cd backend
python -m venv .venv
source .venv/bin/activate  # Windows PowerShell: .venv\\Scripts\\Activate.ps1
pip install -r requirements.txt
export GEMINI_API_KEY="your-key" # PowerShell: $env:GEMINI_API_KEY="your-key"
uvicorn app.main:app --host 0.0.0.0 --port 8000
```

The backend defaults to `gemini-2.5-flash`; override with `GEMINI_MODEL` if needed. Keep the key in an environment variable or deployment secret; never commit it.

## Run Android

Open `android-app` in Android Studio and run on an emulator. The default backend URL `http://10.0.2.2:8000` reaches the host machine from the Android emulator. To configure another backend URL, pass `-PAPI_BASE_URL=https://your-backend.example` to Gradle. Physical devices need a reachable machine address or deployed HTTPS endpoint.

The app has a Gemini chat send action and a scene-plan action. The video renderer is still the existing local title-card/zoom/TTS prototype; Gemini chat replies do not yet drive a full per-scene animation renderer.
