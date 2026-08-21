# PrintBridge transport layer — Stage 2/3

## USB / wired

The Android implementation must use the platform USB Host APIs and explicit user-granted device permission. Scanner/printer protocol handling stays in adapters; transport only owns discovery, connection lifecycle, I/O, timeouts and disconnect events.

## Wireless

Wireless discovery is separated from protocol execution. Providers may discover devices using supported local-network mechanisms, while protocol adapters negotiate capabilities and jobs.

## Important compatibility rule

PrintBridge does **not** assume that every USB or Wi-Fi printer/scanner speaks the same protocol. A device is marked supported only when a compatible adapter/protocol is available. Unsupported devices must produce a useful diagnostic instead of a fake successful scan.

## Recovery

Every transport should expose:
- permission denied
- unavailable/offline
- connecting
- connected
- timeout
- disconnected
- cancelled
- protocol error

The job engine can then retry only safe/recoverable operations.
