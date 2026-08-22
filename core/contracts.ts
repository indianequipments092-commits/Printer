/**
 * PrintBridge Stage 1 domain contracts.
 * These contracts intentionally contain no vendor-specific implementation.
 */

export type TransportKind = 'usb' | 'wifi' | 'ethernet' | 'bluetooth' | 'os-bridge';
export type DeviceKind = 'printer' | 'scanner' | 'multifunction';
export type ScanColorMode = 'color' | 'grayscale' | 'black-white';
export type ScanOutputFormat = 'jpeg' | 'png' | 'pdf';
export type PaperSize = 'a4' | 'a5' | 'a6' | 'letter' | 'legal' | 'custom';

export interface DeviceCapabilityProfile {
  deviceKind: DeviceKind;
  transports: TransportKind[];
  scan?: {
    resolutionsDpi: number[];
    colorModes: ScanColorMode[];
    outputFormats: ScanOutputFormat[];
    paperSizes: PaperSize[];
    duplex: boolean;
    feeder: boolean;
    maxPagesPerJob?: number;
    brightness: boolean;
    contrast: boolean;
    autoCrop: boolean;
    deskew: boolean;
  };
  print?: {
    color: boolean;
    duplex: boolean;
    paperSizes: PaperSize[];
    borderless: boolean;
  };
}

export interface ScanSettings {
  resolutionDpi: number;
  colorMode: ScanColorMode;
  paperSize: PaperSize;
  outputFormat: ScanOutputFormat;
  brightness: number;
  contrast: number;
  quality: number;
  duplex: boolean;
  autoCrop: boolean;
  deskew: boolean;
  orientation: 'portrait' | 'landscape' | 'auto';
}

export interface ScanJob {
  id: string;
  deviceId: string;
  settings: ScanSettings;
  requestedAt: string;
}

export type JobState =
  | 'queued'
  | 'discovering'
  | 'connecting'
  | 'scanning'
  | 'processing'
  | 'preview-ready'
  | 'exporting'
  | 'printing'
  | 'completed'
  | 'cancelled'
  | 'failed';

export interface JobResult {
  jobId: string;
  state: JobState;
  pages: Array<{
    index: number;
    uri: string;
    width?: number;
    height?: number;
  }>;
  outputUri?: string;
  errorCode?: string;
  errorMessage?: string;
}

export interface DeviceAdapter {
  discover(): Promise<string[]>;
  getCapabilities(deviceId: string): Promise<DeviceCapabilityProfile>;
  scan(job: ScanJob): Promise<JobResult>;
  cancel(jobId: string): Promise<void>;
}
