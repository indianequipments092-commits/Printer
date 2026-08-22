import { createJob, DEFAULT_SCAN_SETTINGS, validateScanSettings } from "../src/core/final-pipeline";

describe("PrintBridge final pipeline", () => {
  it("creates a queued scan job", () => {
    expect(createJob("scan", "scan-1")).toEqual({
      id: "scan-1",
      kind: "scan",
      status: "queued",
      progress: 0,
    });
  });

  it("provides safe default scan settings", () => {
    expect(DEFAULT_SCAN_SETTINGS.dpi).toBe(300);
    expect(DEFAULT_SCAN_SETTINGS.outputFormat).toBe("pdf");
    expect(validateScanSettings(DEFAULT_SCAN_SETTINGS)).toBeNull();
  });

  it("rejects invalid scan settings", () => {
    expect(validateScanSettings({ ...DEFAULT_SCAN_SETTINGS, dpi: 1 })?.code).toBe("INVALID_DPI");
    expect(validateScanSettings({ ...DEFAULT_SCAN_SETTINGS, brightness: 101 })?.code).toBe("INVALID_BRIGHTNESS");
  });
});
