# PrintBridge — Combined Stage 2 + 3 + 4

## Device engine
- [x] Device registry
- [x] Adapter lookup boundary
- [x] Capability-aware scan profile normalization
- [x] USB/wired transport boundary
- [x] Wireless/network transport boundary
- [x] Recoverable connection/error states

## Scan engine
- [x] Brightness and contrast controls
- [x] DPI/resolution negotiation
- [x] Color/grayscale/black-white negotiation
- [x] Paper size and orientation negotiation
- [x] Quality control
- [x] Duplex capability handling
- [x] Auto-crop and deskew capability handling
- [x] Composable image-processing pipeline
- [x] JPEG/PNG/PDF exporter boundary
- [x] Multi-page page model

## Mobile
- [x] One-tap Scan workflow defined
- [x] Capability-driven settings UI boundary
- [x] Preview/reorder/remove flow defined
- [x] Save/share output flow defined
- [x] USB permission state defined
- [x] Wireless/network error states defined

## Still requires native hardware implementation

The shared foundation is implemented, but real hardware communication cannot honestly be marked complete until native Android USB/network adapters and at least one real scanner/printer protocol are implemented and tested against physical hardware. The architecture intentionally keeps that work isolated from the mobile UI and domain layer.
