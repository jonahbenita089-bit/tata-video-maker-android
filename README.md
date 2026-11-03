# Tata Motion Studio (Android)

An Android motion-graphics video maker prototype built with Kotlin and Jetpack Compose. Describe a video, attach an image or logo, generate an editable scene-plan script with the assistant UI, and render an MP4 locally.

## Current features

- Compose-based mobile creation screen with prompt, visual styles, duration, and assistant chat.
- Image picker for a graphic/logo; the selected image is composited into the video poster.
- Editable constrained scene-plan code with a simple revise-by-chat interaction.
- On-device MP4 rendering through FFmpegKit, with a slow zoom effect and Android text-to-speech narration.
- Preview and save-to-gallery actions using the existing project helpers.

## Build and run

Open `android-app` in Android Studio and run the app on an Android device or emulator. The Android SDK and configured Gradle dependencies are required. FFmpegKit and device TTS availability can vary by device/Android version.

## Important prototype note

The current assistant screen produces a structured starter scene plan locally; it is not connected to a hosted language model yet. It does not execute arbitrary generated code. The local renderer currently creates a graphic-backed title video with a subtle zoom and narration; the scene plan is editable but is not yet a full timeline renderer. A model provider and scene-to-render compiler can be added behind the assistant API in a subsequent step.
