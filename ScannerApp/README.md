# USB Scanner App

This folder contains the scanner-only Android app. It is intentionally separate from the existing `PrintBridge.zip` printer project.

## Stage status

- Stage 1: Android project foundation — complete
- Stage 2: USB OTG/device discovery + permission — complete
- Stage 3: USB imaging interface probing — complete
- Stage 4: Scan engine boundary — complete, but the exact scanner backend is intentionally not guessed
- Stage 5: Image operations — rotation/grayscale support added
- Stage 6: PNG + multi-page PDF saving — added
- Stage 7: Connection/error handling UI — added
- Stage 8: Build/release workflow — added

## Important hardware requirement

USB scanner devices use class 0x06 for imaging, but the actual scan command/data protocol is commonly model/vendor specific. Android's USB Host API does not provide a universal `scan()` command. Therefore this project does **not** send an invented command to the scanner.

To enable real scanning, the exact scanner model (or its USB VID/PID and protocol/backend) must be identified and a verified `ScanEngine` implementation must be added for that device. Until then the app can safely detect, request permission for, connect to, and probe a scanner without risking incorrect USB commands.

The app contains no printing feature.
