# PrintBridge Stage 2–4 hardware completion

## Implemented
- Standards-based eSCL/AirScan network scanner adapter boundary.
- Standards-based IPP network printer adapter boundary.
- Android USB Host discovery/permission/connect boundary.
- Shared capability-driven scan settings and mobile workflow.

## Hardware validation rule

There is no universal USB scanner protocol. A USB device can be discovered and permissioned generically, but actual scanning requires a protocol/vendor adapter for that scanner. Network scanning is interoperable where the device exposes eSCL/AirScan; printing is interoperable where the device exposes IPP.

PrintBridge therefore reports **supported** only when the matching adapter is present and capability negotiation succeeds. It must never claim that every printer/scanner works merely because it is connected over USB or Wi-Fi.

## Test matrix

| Capability | Automated/software validation | Physical device required |
|---|---|---|
| Scan settings normalization | Yes | No |
| USB discovery/permission state | Boundary covered | Yes for Android runtime |
| eSCL capabilities/job | Adapter implemented | Yes for protocol validation |
| IPP print job | Adapter implemented | Yes for printer validation |
| JPEG/PNG/PDF processing | Core boundary | Yes for end-to-end validation |
| Brightness/contrast | Profile handling | Yes to verify device semantics |
| Duplex/auto-crop/deskew | Capability handling | Yes to verify device behavior |

This document closes the previously missing implementation boundaries without fabricating universal hardware support.
