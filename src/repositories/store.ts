import type { DeviceRecord, PlayerConfigRecord } from '../domain/device.js';
import type { ManifestRecord } from '../domain/manifest.js';
import type {
  CommandAckRecord,
  DeviceHeartbeatState,
  DeviceSyncState,
  PendingCommandRecord,
  PlayerEventRecord
} from '../domain/state.js';

export interface DeviceRepository {
  saveDevice(device: DeviceRecord, config: PlayerConfigRecord): Promise<void>;
  findById(deviceId: string): Promise<DeviceRecord | undefined>;
  findConfigByDeviceId(deviceId: string): Promise<PlayerConfigRecord | undefined>;
}

export interface ManifestRepository {
  saveManifest(manifest: ManifestRecord): Promise<void>;
  findLatestByDevice(deviceId: string): Promise<ManifestRecord | undefined>;
}

export interface AssetRepository {
  findAssetsByIds(deviceId: string, assetIds: string[]): Promise<ManifestRecord['assets']>;
}

export interface RuntimeStateRepository {
  saveHeartbeat(state: DeviceHeartbeatState): Promise<void>;
  findHeartbeat(deviceId: string): Promise<DeviceHeartbeatState | undefined>;
  saveSyncState(state: DeviceSyncState): Promise<void>;
  findSyncState(deviceId: string): Promise<DeviceSyncState | undefined>;
  appendEvents(events: PlayerEventRecord[]): Promise<void>;
  listEventsByDevice(deviceId: string): Promise<PlayerEventRecord[]>;
  enqueueCommand(command: PendingCommandRecord): Promise<void>;
  listPendingCommands(deviceId: string, now: number): Promise<PendingCommandRecord[]>;
  ackCommand(record: CommandAckRecord): Promise<void>;
  listCommandAcks(deviceId: string): Promise<CommandAckRecord[]>;
}

export class InMemoryStore implements DeviceRepository, ManifestRepository, AssetRepository, RuntimeStateRepository {
  private readonly devices = new Map<string, DeviceRecord>();
  private readonly configs = new Map<string, PlayerConfigRecord>();
  private readonly manifests = new Map<string, ManifestRecord>();
  private readonly events = new Map<string, PlayerEventRecord[]>();
  private readonly heartbeats = new Map<string, DeviceHeartbeatState>();
  private readonly syncStates = new Map<string, DeviceSyncState>();
  private readonly pendingCommands = new Map<string, PendingCommandRecord[]>();
  private readonly commandAcks = new Map<string, CommandAckRecord[]>();

  async saveDevice(device: DeviceRecord, config: PlayerConfigRecord): Promise<void> {
    this.devices.set(device.deviceId, device);
    this.configs.set(device.deviceId, config);
  }

  async findById(deviceId: string): Promise<DeviceRecord | undefined> {
    return this.devices.get(deviceId);
  }

  async findConfigByDeviceId(deviceId: string): Promise<PlayerConfigRecord | undefined> {
    return this.configs.get(deviceId);
  }

  async saveManifest(manifest: ManifestRecord): Promise<void> {
    this.manifests.set(manifest.deviceId, manifest);
  }

  async findLatestByDevice(deviceId: string): Promise<ManifestRecord | undefined> {
    return this.manifests.get(deviceId);
  }

  async findAssetsByIds(deviceId: string, assetIds: string[]): Promise<ManifestRecord['assets']> {
    const manifest = await this.findLatestByDevice(deviceId);
    if (!manifest) {
      return [];
    }

    const wanted = new Set(assetIds);
    return manifest.assets.filter((asset) => wanted.has(asset.assetId));
  }

  async saveHeartbeat(state: DeviceHeartbeatState): Promise<void> {
    this.heartbeats.set(state.deviceId, state);
  }

  async findHeartbeat(deviceId: string): Promise<DeviceHeartbeatState | undefined> {
    return this.heartbeats.get(deviceId);
  }

  async saveSyncState(state: DeviceSyncState): Promise<void> {
    this.syncStates.set(state.deviceId, state);
  }

  async findSyncState(deviceId: string): Promise<DeviceSyncState | undefined> {
    return this.syncStates.get(deviceId);
  }

  async appendEvents(events: PlayerEventRecord[]): Promise<void> {
    for (const event of events) {
      const current = this.events.get(event.deviceId) ?? [];
      current.push(event);
      this.events.set(event.deviceId, current);
    }
  }

  async listEventsByDevice(deviceId: string): Promise<PlayerEventRecord[]> {
    return this.events.get(deviceId) ?? [];
  }

  async enqueueCommand(command: PendingCommandRecord): Promise<void> {
    const current = this.pendingCommands.get(command.deviceId) ?? [];
    current.push(command);
    this.pendingCommands.set(command.deviceId, current);
  }

  async listPendingCommands(deviceId: string, now: number): Promise<PendingCommandRecord[]> {
    return (this.pendingCommands.get(deviceId) ?? []).filter((command) => command.expireAt > now);
  }

  async ackCommand(record: CommandAckRecord): Promise<void> {
    const current = this.commandAcks.get(record.deviceId) ?? [];
    current.push(record);
    this.commandAcks.set(record.deviceId, current);

    const pending = this.pendingCommands.get(record.deviceId) ?? [];
    this.pendingCommands.set(
      record.deviceId,
      pending.filter((command) => command.commandId !== record.commandId)
    );
  }

  async listCommandAcks(deviceId: string): Promise<CommandAckRecord[]> {
    return this.commandAcks.get(deviceId) ?? [];
  }
}
