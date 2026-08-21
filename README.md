# PrintBridge

PrintBridge is a local-first, extensible print-and-scan bridge designed to connect mobile, desktop, USB and network printers/scanners through a unified workflow.

## Stage 1 — Foundation

Stage 1 establishes the architecture and product contract for USB/wireless transport, mobile Scan workflows, desktop/CLI jobs, capability negotiation, scan settings, multi-page output, print/scan job state, adapter boundaries and local-first security.

## Stage 2 + 3 + 4 — Device Engine + Wireless + Mobile

This combined stage adds the shared implementation foundation for device registry/discovery, USB/wired permission states, wireless/network boundaries, capability-aware scan profiles, brightness/contrast/DPI/color/paper/orientation/quality controls, duplex, auto-crop, deskew, image processing, JPEG/PNG/PDF export, multi-page scanning and mobile Scan/preview/export flows.

## Stage 5 — Desktop / PC Engine

Adds desktop job orchestration, queue lifecycle, cancellation, CLI command contracts and desktop provider boundaries.

## Stage 6–8 — Final Mega Stage

Adds advanced print/scan job contracts, scan settings validation, processing/output contracts, reliability and security boundaries, structured errors, retry/cancellation rules, compatibility matrices, deterministic validation tests, release documentation and final acceptance checklists.

## Hardware reality rule

The shared foundation is deliberately separated from native hardware code. Real USB scanning and wireless scanning require compatible native transports and scanner/printer protocol adapters. PrintBridge must never fake hardware support; a device is marked supported only after its protocol/capabilities are actually available and validated.

## Core workflows

**Mobile:** Scan → discover USB/wireless device → negotiate capabilities → apply scan settings → preview/process → export image/PDF.

**PC:** command → discover/select device → scan/print job → progress → result/history.

## Architecture

`clients/` → mobile/desktop experiences

`core/` → normalized device, scan and print domain contracts

`transports/` → USB/wired and wireless connection boundaries

`adapters/` → vendor/protocol integrations

`docs/` → architecture, compatibility and stage checklists

'tests/' → deterministic validation tests
