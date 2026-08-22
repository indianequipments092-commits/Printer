# Stage 5 — Desktop / PC Engine

## Implemented foundation

- [x] Desktop job model and deterministic lifecycle
- [x] Progress reporting and cancellation states
- [x] CLI command contract for devices, scan, print and jobs
- [x] Desktop device-provider boundary
- [x] USB and network transport types carried into desktop devices
- [x] Capability-aware scan request contract
- [x] Native/vendor adapter boundary
- [x] Recoverable error shape

## Validation required on a real PC

- [ ] Windows native USB scanner adapter test
- [ ] Windows network scanner test
- [ ] Windows print test
- [ ] macOS/Linux adapter tests
- [ ] Real scanner scan-to-image/PDF test
- [ ] Real printer print-job test

The unchecked items are hardware/platform validation, not simulated success. PrintBridge must not claim a native adapter works until it is tested against the target platform/device.