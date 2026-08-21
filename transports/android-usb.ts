export interface AndroidUsbDevice { deviceId: number; vendorId: number; productId: number; productName?: string }

export interface AndroidUsbHost {
  listDevices(): Promise<AndroidUsbDevice[]>;
  requestPermission(deviceId: number): Promise<boolean>;
  open(deviceId: number): Promise<{ close(): void }>;
}

/** Android USB Host integration point. USB scanners do not share one universal
 * scan protocol, so a device/protocol adapter must be selected after descriptors
 * and capabilities are inspected. */
export class AndroidUsbTransport {
  constructor(private readonly host: AndroidUsbHost) {}

  async discover() { return this.host.listDevices(); }

  async connect(deviceId: number) {
    const granted = await this.host.requestPermission(deviceId);
    if (!granted) throw new Error('USB permission denied');
    return this.host.open(deviceId);
  }
}
