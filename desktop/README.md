# PrintBridge Desktop Engine — Stage 5

Stage 5 provides a platform-neutral desktop orchestration layer for PC print/scan workflows.

## Goals

- Discover registered printers/scanners through transport providers.
- Submit scan and print jobs with deterministic state transitions.
- Support CLI-friendly commands without binding the core to a specific shell.
- Stream progress and recoverable errors.
- Preserve the Stage 2–4 capability model and scan settings.
- Keep native Windows/macOS/Linux integrations behind adapters.

## Job lifecycle

`queued → preparing → running → completed`

Failure/cancellation may transition to `failed` or `cancelled`. Terminal states cannot be reused.

## Hardware boundary

This layer does not pretend to implement vendor drivers. Native device protocols belong in platform/vendor adapters and must report their actual capabilities.