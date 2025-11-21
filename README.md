# CaféTone

A premium audio enhancement app for Android, featuring Sony Premium Audio design language.

## Features

- Real-time audio processing using Android AudioEffect APIs
- Virtualizer for soundstage enhancement
- PresetReverb for ambience control
- Automatic audio session interception
- Beautiful dark UI with Sony Premium styling

## Requirements

- Android 8.0 (API 26) or higher
- Android Studio with Gradle 8.1.0+

## Building

1. Open the project in Android Studio
2. Sync Gradle files
3. Build and run on a device or emulator

## Permissions

The app requires the following permissions:
- `FOREGROUND_SERVICE` - For running the audio engine service
- `FOREGROUND_SERVICE_MEDIA_PLAYBACK` - For media playback service type
- `MODIFY_AUDIO_SETTINGS` - For applying audio effects
- `POST_NOTIFICATIONS` - For foreground service notification
- `VIBRATE` - For haptic feedback

## Architecture

- **MainActivity**: UI controller and service binding
- **CafeModeService**: Foreground service managing audio effects
- **AudioSessionReceiver**: BroadcastReceiver intercepting audio sessions

# cafem
