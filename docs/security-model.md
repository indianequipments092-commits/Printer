# PrintBridge Security & Reliability Model

- Local-first processing by default.
- Device permissions are explicit and revocable.
- Scan settings and job parameters are validated before execution.
- Temporary files must have bounded lifetime and be cleaned after completion/failure.
- Diagnostics must redact sensitive document data.
- Network/device failures are represented as structured recoverable errors.
- Retry must be bounded and cancellation must propagate to the underlying job.
- Unsupported device capabilities must produce explicit unsupported states rather than silent fallbacks.
