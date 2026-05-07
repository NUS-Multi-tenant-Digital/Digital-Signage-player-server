import type { DeviceRecord, PlayerConfigRecord } from '../../domain/device.js';
import { store } from '../../lib/app-context.js';

export interface RegisterPlayerRequest {
  device_sn: string;
  activation_code: string;
  device_name: string;
  platform: string;
  app_version: string;
  os_version: string;
  firmware_version?: string;
  screen_resolution: string;
  timezone: string;
  mac_address: string;
  ip_address: string;
  capabilities: Record<string, unknown>;
}

function buildDeviceRecord(input: RegisterPlayerRequest, now: number): DeviceRecord {
  return {
    deviceId: `device_${input.device_sn}`,
    tenantId: 'tenant_demo',
    locationId: 'location_demo',
    groupId: undefined,
    deviceSn: input.device_sn,
    activationCode: input.activation_code,
    deviceName: input.device_name,
    platform: input.platform,
    appVersion: input.app_version,
    osVersion: input.os_version,
    firmwareVersion: input.firmware_version,
    screenResolution: input.screen_resolution,
    timezone: input.timezone,
    macAddress: input.mac_address,
    ipAddress: input.ip_address,
    capabilities: input.capabilities,
    accessToken: 'mock-access-token',
    tokenExpireAt: now + 24 * 60 * 60 * 1000,
    status: 'active',
    createdAt: now,
    updatedAt: now
  };
}

function buildPlayerConfig(device: DeviceRecord): PlayerConfigRecord {
  return {
    deviceId: device.deviceId,
    tenantId: device.tenantId,
    locationId: device.locationId,
    heartbeatIntervalSec: 30,
    manifestSyncIntervalSec: 60,
    eventFlushIntervalSec: 30,
    maxCacheSizeMb: 2048,
    assetDownloadConcurrency: 3,
    enableOfflineMode: true,
    enableWatchdog: true,
    enableScreenshot: false,
    logLevel: 'info',
    supportedAssetTypes: ['ASSET_VIDEO', 'ASSET_IMAGE', 'ASSET_TEXT']
  };
}

export async function registerPlayer(input: RegisterPlayerRequest) {
  const now = Date.now();
  const device = buildDeviceRecord(input, now);
  const config = buildPlayerConfig(device);

  await store.saveDevice(device, config);
  await store.saveSyncState({
    deviceId: device.deviceId,
    manifestVersion: 0,
    lastOnlineAt: now
  });

  return {
    device_id: device.deviceId,
    tenant_id: device.tenantId,
    location_id: device.locationId,
    access_token: device.accessToken,
    token_expire_at: device.tokenExpireAt,
    config: {
      device_id: config.deviceId,
      tenant_id: config.tenantId,
      location_id: config.locationId,
      heartbeat_interval_sec: config.heartbeatIntervalSec,
      manifest_sync_interval_sec: config.manifestSyncIntervalSec,
      event_flush_interval_sec: config.eventFlushIntervalSec,
      max_cache_size_mb: config.maxCacheSizeMb,
      asset_download_concurrency: config.assetDownloadConcurrency,
      enable_offline_mode: config.enableOfflineMode,
      enable_watchdog: config.enableWatchdog,
      enable_screenshot: config.enableScreenshot,
      log_level: config.logLevel,
      supported_asset_types: config.supportedAssetTypes
    }
  };
}
