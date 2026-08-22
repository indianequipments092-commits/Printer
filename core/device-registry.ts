import type { DeviceAdapter, DeviceCapabilityProfile, TransportKind } from './contracts';

export interface DiscoveredDevice {
  id: string;
  name: string;
  kind: 'printer' | 'scanner' | 'multifunction';
  transports: TransportKind[];
  adapterId: string;
  online: boolean;
  capabilities?: DeviceCapabilityProfile;
}

export interface DiscoveryProvider {
  id: string;
  transport: TransportKind;
  discover(): Promise<DiscoveredDevice[]>;
}

export class DeviceRegistry {
  private readonly adapters = new Map<string, DeviceAdapter>();
  private readonly providers = new Map<string, DiscoveryProvider>();

  registerAdapter(id: string, adapter: DeviceAdapter): void {
    this.adapters.set(id, adapter);
  }

  registerProvider(provider: DiscoveryProvider): void {
    this.providers.set(provider.id, provider);
  }

  async discover(): Promise<DiscoveredDevice[]> {
    const results = await Promise.allSettled(
      [...this.providers.values()].map((provider) => provider.discover()),
    );
    const devices = results.flatMap((result) =>
      result.status === 'fulfilled' ? result.value : [],
    );
    return [...new Map(devices.map((device) => [device.id, device])).values()];
  }

  async capabilities(device: DiscoveredDevice): Promise<DeviceCapabilityProfile> {
    const adapter = this.adapters.get(device.adapterId);
    if (!adapter) throw new Error(`Adapter not registered: ${device.adapterId}`);
    return adapter.getCapabilities(device.id);
  }
}
