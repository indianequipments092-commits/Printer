import type { DeviceCapabilityProfile, ScanSettings } from '../core/contracts';

export interface HttpTransport {
  request(url: string, init?: { method?: string; headers?: Record<string, string>; body?: string }): Promise<{ status: number; headers: Record<string, string>; body: Uint8Array }>;
}

/** eSCL/AirScan adapter boundary. It supports the standard network scanner workflow
 * without pretending that a vendor-specific USB protocol is universal. */
export class EsclScannerAdapter {
  constructor(private readonly http: HttpTransport, private readonly baseUrl: string) {}

  async capabilities(): Promise<DeviceCapabilityProfile> {
    const response = await this.http.request(`${this.baseUrl}/eSCL/ScannerCapabilities`);
    if (response.status < 200 || response.status >= 300) throw new Error(`Scanner capabilities failed: HTTP ${response.status}`);
    const xml = new TextDecoder().decode(response.body);
    const resolutionsDpi = [...xml.matchAll(/<p:DiscreteResolution>(\d+)<\/p:DiscreteResolution>/g)].map(m => Number(m[1]));
    const colorModes = xml.includes('Color') ? ['color', 'grayscale', 'blackwhite'] as const : ['grayscale'] as const;
    const paperSizes = ['a4', 'a5', 'letter'] as const;
    return { scan: { resolutionsDpi: resolutionsDpi.length ? resolutionsDpi : [300], colorModes: [...colorModes], paperSizes: [...paperSizes], outputFormats: ['jpeg', 'png', 'pdf'], duplex: xml.includes('Duplex'), autoCrop: true, deskew: true } };
  }

  async scan(settings: ScanSettings): Promise<Uint8Array> {
    const xml = `<?xml version="1.0" encoding="UTF-8"?><scan:ScanSettings xmlns:scan="http://schemas.hp.com/imaging/escl/2011/05/03"><p:Version xmlns:p="http://schemas.hp.com/imaging/escl/2011/05/03">2.1</p:Version><p:Intent xmlns:p="http://schemas.hp.com/imaging/escl/2011/05/03">Document</p:Intent><p:DocumentFormatExt xmlns:p="http://schemas.hp.com/imaging/escl/2011/05/03">${settings.outputFormat === 'pdf' ? 'application/pdf' : settings.outputFormat === 'png' ? 'image/png' : 'image/jpeg'}</p:DocumentFormatExt><p:ColorMode xmlns:p="http://schemas.hp.com/imaging/escl/2011/05/03">${settings.colorMode === 'color' ? 'RGB24' : settings.colorMode === 'blackwhite' ? 'BlackAndWhite1' : 'Grayscale8'}</p:ColorMode><p:XResolution xmlns:p="http://schemas.hp.com/imaging/escl/2011/05/03">${settings.resolutionDpi}</p:XResolution><p:YResolution xmlns:p="http://schemas.hp.com/imaging/escl/2011/05/03">${settings.resolutionDpi}</p:YResolution></scan:ScanSettings>`;
    const response = await this.http.request(`${this.baseUrl}/eSCL/ScanJobs`, { method: 'POST', headers: { 'Content-Type': 'application/xml' }, body: xml });
    if (response.status < 200 || response.status >= 300) throw new Error(`Scan job failed: HTTP ${response.status}`);
    return response.body;
  }
}
