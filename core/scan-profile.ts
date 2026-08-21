import type { DeviceCapabilityProfile, ScanSettings } from './contracts';

export const DEFAULT_SCAN_SETTINGS: ScanSettings = {
  resolutionDpi: 300,
  colorMode: 'color',
  paperSize: 'a4',
  outputFormat: 'pdf',
  brightness: 0,
  contrast: 0,
  quality: 90,
  duplex: false,
  autoCrop: true,
  deskew: true,
  orientation: 'auto',
};

export function normalizeScanSettings(
  requested: Partial<ScanSettings>,
  capability: DeviceCapabilityProfile,
): ScanSettings {
  if (!capability.scan) throw new Error('Device does not support scanning');
  const scan = capability.scan;
  const resolutionDpi = scan.resolutionsDpi.includes(requested.resolutionDpi ?? DEFAULT_SCAN_SETTINGS.resolutionDpi)
    ? requested.resolutionDpi ?? DEFAULT_SCAN_SETTINGS.resolutionDpi
    : scan.resolutionsDpi[0];
  const colorMode = scan.colorModes.includes(requested.colorMode ?? DEFAULT_SCAN_SETTINGS.colorMode)
    ? requested.colorMode ?? DEFAULT_SCAN_SETTINGS.colorMode
    : scan.colorModes[0];
  const outputFormat = scan.outputFormats.includes(requested.outputFormat ?? DEFAULT_SCAN_SETTINGS.outputFormat)
    ? requested.outputFormat ?? DEFAULT_SCAN_SETTINGS.outputFormat
    : scan.outputFormats[0];
  const paperSize = scan.paperSizes.includes(requested.paperSize ?? DEFAULT_SCAN_SETTINGS.paperSize)
    ? requested.paperSize ?? DEFAULT_SCAN_SETTINGS.paperSize
    : scan.paperSizes[0];

  return {
    ...DEFAULT_SCAN_SETTINGS,
    ...requested,
    resolutionDpi,
    colorMode,
    outputFormat,
    paperSize,
    duplex: Boolean(requested.duplex && scan.duplex),
    autoCrop: Boolean(requested.autoCrop !== false && scan.autoCrop),
    deskew: Boolean(requested.deskew !== false && scan.deskew),
    brightness: Math.max(-100, Math.min(100, requested.brightness ?? 0)),
    contrast: Math.max(-100, Math.min(100, requested.contrast ?? 0)),
    quality: Math.max(1, Math.min(100, requested.quality ?? 90)),
  };
}
