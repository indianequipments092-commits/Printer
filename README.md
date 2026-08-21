# PrintBridge

PrintBridge is a high-performance, extensible print-and-scan bridge designed to connect mobile, desktop, USB and network printers/scanners through a unified workflow.

## Stage 1 — Foundation

Stage 1 establishes the architecture and product contract for:

- USB and wireless/network printer discovery and communication
- Mobile-initiated scanning with preview and scan profiles
- Desktop/CLI initiated scanning with the same normalized job model
- Brightness, contrast, DPI/resolution, color mode, paper size, orientation and quality controls
- Automatic crop, deskew and image enhancement as capability permits
- Single-page and multi-page scanning
- JPEG, PNG and PDF output
- Print job submission, queueing, status, cancellation and retry
- Capability detection so unsupported device features are never falsely exposed
- Device adapters/drivers isolated behind stable interfaces
- Local-first operation with optional cloud features kept outside the device core
- Security boundaries, permissions, diagnostics and structured logging

## Product principle

PrintBridge should behave consistently whether a job starts from a mobile Scan button, a desktop command, or a network-connected workflow. Device-specific protocols belong in adapters; the application layer consumes normalized capabilities and job results.

## Planned architecture

`clients/` → mobile/desktop user experiences

`core/` → normalized device, scan and print domain contracts

`adapters/` → USB/network/vendor-specific integrations

`processing/` → image/PDF post-processing pipeline

`transport/` → discovery, connection and job transport

`diagnostics/` → logs, health checks and troubleshooting

`docs/` → architecture, compatibility and implementation decisions

## Important compatibility note

USB and wireless scanning are possible, but exact capabilities depend on the scanner/printer model and protocol. PrintBridge therefore uses capability negotiation and adapter-based integrations rather than assuming every device supports every feature.
