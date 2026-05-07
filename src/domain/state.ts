export interface PlayerEventRecord {
  eventId: string;
  deviceId: string;
  eventType: string;
  timestamp: number;
  manifestId?: string;
  manifestVersion?: number;
  assetId?: string;
  playlistItemId?: string;
  errorCode?: string;
  errorMessage?: string;
  extraJson?: string;
}

export interface DeviceHeartbeatState {
  deviceId: string;
  lastHeartbeatAt: number;
  appVersion: string;
  manifestId: string;
  manifestVersion: number;
  playback: Record<string, unknown>;
  health: Record<string, unknown>;
  cache: Record<string, unknown>;
  network: Record<string, unknown>;
  updatedAt: number;
}

export interface DeviceSyncState {
  deviceId: string;
  layoutVersion?: number;
  scheduleVersion?: number;
  manifestVersion?: number;
  lastSyncAt?: number;
  lastOnlineAt?: number;
}

export interface CommandAckRecord {
  deviceId: string;
  commandId: string;
  type: string;
  success: boolean;
  errorCode?: string;
  errorMessage?: string;
  executedAt: number;
  receivedAt: number;
}

export interface PendingCommandRecord {
  commandId: string;
  deviceId: string;
  type: string;
  issuedAt: number;
  expireAt: number;
  payloadJson: string;
}
