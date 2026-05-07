export interface RuntimeCapabilities {
  [key: string]: unknown;
}

export interface DeviceRecord {
  deviceId: string;
  tenantId: string;
  locationId: string;
  groupId?: string;
  deviceSn: string;
  activationCode: string;
  deviceName: string;
  platform: string;
  appVersion: string;
  osVersion: string;
  firmwareVersion?: string;
  screenResolution: string;
  timezone: string;
  macAddress: string;
  ipAddress: string;
  capabilities: RuntimeCapabilities;
  accessToken: string;
  tokenExpireAt: number;
  status: 'active' | 'inactive' | 'offline';
  createdAt: number;
  updatedAt: number;
}

export interface PlayerConfigRecord {
  deviceId: string;
  tenantId: string;
  locationId: string;
  heartbeatIntervalSec: number;
  manifestSyncIntervalSec: number;
  eventFlushIntervalSec: number;
  maxCacheSizeMb: number;
  assetDownloadConcurrency: number;
  enableOfflineMode: boolean;
  enableWatchdog: boolean;
  enableScreenshot: boolean;
  logLevel: string;
  supportedAssetTypes: string[];
}
