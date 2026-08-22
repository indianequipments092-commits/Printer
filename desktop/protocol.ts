import type { DeviceCapabilityProfile, ScanSettings } from '../core/contracts';

export interface DesktopDevice {
  id: string;
  name: string;
  transport: 'usb' | 'network';
  capabilities: DeviceCapabilityProfile;
}

export interface DesktopScanRequest {
  deviceId: string;
  settings: ScanSettings;
}

export interface DesktopScanResult {
  pages: Array<{ id: string; mimeType: string; dataRef: string }>;
  outputMimeType: 'application/pdf' | 'image/jpeg' | 'image/png';
}

export interface DesktopDeviceProvider {
  listDevices(signal?: AbortSignal): Promise<DesktopDevice[]>;
  scan(request: DesktopScanRequest, onProgress?: (percent: number) => void, signal?: AbortSignal): Promise<DesktopScanResult>;
  print(deviceId: string, sourceRef: string, signal?: AbortSignal): Promise<{ accepted: boolean; jobRef?: string }>;
}
