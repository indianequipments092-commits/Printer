# PrintBridge Mobile — Stage 4 foundation

The mobile client is designed around one user action: **Scan**.

## Flow

1. Discover USB and wireless devices.
2. Show only capabilities supported by the selected device.
3. Choose a scan profile (DPI, color mode, paper size, brightness, contrast, quality, duplex, auto-crop, deskew and orientation).
4. Tap **Scan**.
5. Stream job progress: connecting → scanning → processing → preview.
6. Preview pages and reorder/remove pages.
7. Export as PDF/JPEG/PNG and save/share locally.

## Native boundary

Android USB Host, Wi-Fi discovery and vendor protocols belong behind native adapters. The UI must never contain vendor-specific USB packet/protocol code. Native adapters report normalized events and capability profiles to the shared core.

## UX states

- No device found
- USB permission required
- Wireless permission/network unavailable
- Ready
- Scanning page N
- Processing
- Preview ready
- Exporting
- Completed
- Recoverable disconnect
- Unsupported feature
- Fatal device error

The UI should make a real scan impossible to start until a compatible device and valid capability-aware profile exist.
