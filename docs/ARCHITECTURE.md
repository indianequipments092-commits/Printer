# PrintBridge Architecture — Stage 1

## 1. Core layers

### Client layer
- Mobile client: Scan, Print, device management, preview, scan settings, history.
- Desktop client/CLI: command-driven scan/print workflows and diagnostics.
- Future web/admin client: optional management surface; never required for local device operation.

### Application/domain layer
- Device registry and capability negotiation.
- Scan job orchestration.
- Print job orchestration.
- Queue, retry and cancellation state machines.
- Normalized job/result models independent of vendor protocol.

### Adapter layer
Adapters translate vendor/protocol-specific operations into the normalized domain contracts.

Planned adapter families include:
- USB device transport
- Network/Wi-Fi discovery and transport
- Vendor-specific scanner/printer protocols
- OS bridge integrations where native drivers are required

### Processing layer
A composable pipeline handles operations such as:
- crop
- deskew
- rotate
- brightness
- contrast
- grayscale / monochrome conversion
- sharpening / denoise where supported
- compression and quality selection
- multi-page PDF assembly

Processing must never modify the original scan unless explicitly requested.

## 2. Scan flow

`User → Scan profile → Device capability check → Transport → Scanner → Raw pages → Processing pipeline → Preview → Export → History`

The same normalized scan job can be initiated by a mobile button or a desktop command.

## 3. Print flow

`User → Print profile → Capability check → Document normalization → Transport → Printer queue → Status events → Completion/failure`

## 4. Capability negotiation

Every device exposes only the features it actually supports. For example, if a scanner does not support a requested DPI or duplex mode, the application must either offer a supported alternative or return a clear capability error.

## 5. Connectivity

USB and wireless are first-class transport categories. Connection discovery and job execution are separated so the rest of the application does not depend on a particular transport.

## 6. Reliability

Jobs receive stable IDs and explicit states. Retries are bounded and idempotency-aware. Temporary disconnects should be recoverable where the underlying device/protocol allows it.

## 7. Security

- Least-privilege permissions.
- No silent network exposure.
- Explicit USB/device permission handling.
- Sensitive diagnostic data must be redacted.
- Local files remain local unless the user explicitly enables a remote/cloud feature.

## 8. Extensibility

New printer/scanner models should require an adapter or capability mapping, not a rewrite of the client UI or job engine.
