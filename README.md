# ULTRON MOBILE

A native Android assistant prototype built with Kotlin and Jetpack Compose.

## Version 1.0.0-alpha.2
- Dark crimson HUD and four-tab navigation
- Gemini 2.5 Flash text chat through Google's Gemini API
- User-supplied API key stored locally with Android Keystore AES-GCM encryption
- Session-only chat context; no persistent conversation storage yet

## Build and install
Open **Actions → Android Debug APK** and download the artifact from the latest successful run. Extract the ZIP and install the APK. This is a debug build, not a signed production release.

## Setup
1. Obtain a Gemini API key from Google AI Studio.
2. Open ULTRON → SETTINGS, enter the key, and tap SAVE KEY.
3. Open CHAT to send a message.

Gemini requires an internet connection. Never commit API keys to the repository. An on-device key is encrypted at rest but a client-side API key can still be extracted on a compromised device; apply appropriate API restrictions and quotas.

## Upcoming
- Robotic voice and push-to-talk
- Persistent Cerebro memory
- Permission-gated Android tools

Independent Android adaptation inspired by AbuZar-Ansarii/Ultron; no original project source is included.
