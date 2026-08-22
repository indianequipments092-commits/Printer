export type JobKind = "scan" | "print";
export type OutputFormat = "jpeg" | "png" | "pdf";
export type ColorMode = "color" | "grayscale" | "bw";

export interface ScanSettings {
  dpi: number;
  brightness: number;
  contrast: number;
  colorMode: ColorMode;
  outputFormat: OutputFormat;
  paperSize: string;
  autoCrop: boolean;
  deskew: boolean;
  rotateDegrees: 0 | 90 | 180 | 270;
  duplex: boolean;
}

export interface JobError {
  code: string;
  message: string;
  retryable: boolean;
}

export interface JobState {
  id: string;
  kind: JobKind;
  status: "queued" | "running" | "completed" | "failed" | "cancelled";
  progress: number;
  error?: JobError;
}

export const DEFAULT_SCAN_SETTINGS: ScanSettings = {
  dpi: 300,
  brightness: 0,
  contrast: 0,
  colorMode: "color",
  outputFormat: "pdf",
  paperSize: "A4",
  autoCrop: true,
  deskew: true,
  rotateDegrees: 0,
  duplex: false,
};

export function validateScanSettings(settings: ScanSettings): JobError | null {
  if (!Number.isInteger(settings.dpi) || settings.dpi < 75 || settings.dpi > 1200) {
    return { code: "INVALID_DPI", message: "DPI must be between 75 and 1200.", retryable: false };
  }
  if (settings.brightness < -100 || settings.brightness > 100) {
    return { code: "INVALID_BRIGHTNESS", message: "Brightness must be between -100 and 100.", retryable: false };
  }
  if (settings.contrast < -100 || settings.contrast > 100) {
    return { code: "INVALID_CONTRAST", message: "Contrast must be between -100 and 100.", retryable: false };
  }
  return null;
}

export function createJob(kind: JobKind, id: string): JobState {
  return { id, kind, status: "queued", progress: 0 };
}
