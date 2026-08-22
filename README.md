# PrintBridge

PrintBridge is a local-first, extensible print-and-scan bridge designed to connect mobile, desktop, USB and network printers/scanners through a unified workflow.

## Stage 1 — Foundation

Stage 1 establishes the architecture and product contract for USB/wireless transport, mobile Scan workflows, desktop/CLI jobs, capability negotiation, scan settings, multi-page output, print/scan job state, adapter boundaries and local-first security.

## Stage 2 + 3 + 4 — Device Engine + Wireless + Mobile

This combined stage adds the shared implementation foundation for:

- Device registry and discovery providers
- USB/wired transport boundary with explicit permission and disconnect states
- Wireless/network transport boundary
- Capability-aware scan profile normalization
- Brightness, contrast, DPI, color mode, paper size, orientation and quality controls
- Duplex, auto-crop and deskew capability handling
- Composable image-processing pipeline
- JPEG/PNG/PDF export boundary
- Multi-page scan page model
- Mobile one-tap Scan workflow, preview and export UX boundary
- Clear unsupported-device and recoverable-error states

## Hardware reality rule

The shared foundation is deliberately separated from native hardware code. Real USB scanning and wireless scanning require a compatible Android/native transport plus a scanner/printer protocol adapter. PrintBridge must never fake hardware support; a device is marked supported only after its protocol/capabilities are actually available and validated.

## Architecture

`clients/` → mobile/desktop experiences

`core/` → normalized device, scan and print domain contracts

`transports/` → USB/wired and wireless connection boundaries

`adapters/` → vendor/protocol integrations

`core/scan-pipeline.ts` → image processing and export orchestration

`docs/` → architecture, compatibility and stage checklists
