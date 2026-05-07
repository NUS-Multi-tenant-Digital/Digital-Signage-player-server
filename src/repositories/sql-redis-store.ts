import mysql, { type Pool, type RowDataPacket } from 'mysql2/promise';
import { createClient } from 'redis';
import type { AppConfig } from '../config/env.js';
import type { DeviceRecord, PlayerConfigRecord } from '../domain/device.js';
import type { ManifestRecord } from '../domain/manifest.js';
import type {
  CommandAckRecord,
  DeviceHeartbeatState,
  DeviceSyncState,
  PendingCommandRecord,
  PlayerEventRecord
} from '../domain/state.js';
import type { AssetRepository, DeviceRepository, ManifestRepository, RuntimeStateRepository } from './store.js';

type MysqlRow = RowDataPacket & Record<string, unknown>;

function heartbeatKey(deviceId: string): string {
  return `player:device:${deviceId}:heartbeat`;
}

function syncKey(deviceId: string): string {
  return `player:device:${deviceId}:sync`;
}

function pendingCommandsKey(deviceId: string): string {
  return `player:device:${deviceId}:commands:pending`;
}

function parseJson<T>(value: unknown, fallback: T): T {
  if (typeof value !== 'string') {
    return fallback;
  }

  try {
    return JSON.parse(value) as T;
  } catch {
    return fallback;
  }
}

function serializeJson(value: unknown): string {
  return JSON.stringify(value ?? {});
}

function mapDevice(row: MysqlRow): DeviceRecord {
  return {
    deviceId: String(row.device_id),
    tenantId: String(row.tenant_id),
    locationId: String(row.location_id),
    groupId: row.group_id ? String(row.group_id) : undefined,
    deviceSn: String(row.device_sn),
    activationCode: String(row.activation_code),
    deviceName: String(row.device_name),
    platform: String(row.platform),
    appVersion: String(row.app_version),
    osVersion: String(row.os_version),
    firmwareVersion: row.firmware_version ? String(row.firmware_version) : undefined,
    screenResolution: String(row.screen_resolution),
    timezone: String(row.timezone),
    macAddress: String(row.mac_address),
    ipAddress: String(row.ip_address),
    capabilities: parseJson(row.capabilities_json, {}),
    accessToken: String(row.access_token),
    tokenExpireAt: Number(row.token_expire_at),
    status: String(row.status) as DeviceRecord['status'],
    createdAt: new Date(String(row.created_at)).getTime(),
    updatedAt: new Date(String(row.updated_at)).getTime()
  };
}

function mapConfig(row: MysqlRow): PlayerConfigRecord {
  return {
    deviceId: String(row.device_id),
    tenantId: String(row.tenant_id ?? ''),
    locationId: String(row.location_id ?? ''),
    heartbeatIntervalSec: Number(row.heartbeat_interval_sec),
    manifestSyncIntervalSec: Number(row.manifest_sync_interval_sec),
    eventFlushIntervalSec: Number(row.event_flush_interval_sec),
    maxCacheSizeMb: Number(row.max_cache_size_mb),
    assetDownloadConcurrency: Number(row.asset_download_concurrency),
    enableOfflineMode: Boolean(row.enable_offline_mode),
    enableWatchdog: Boolean(row.enable_watchdog),
    enableScreenshot: Boolean(row.enable_screenshot),
    logLevel: String(row.log_level),
    supportedAssetTypes: parseJson<string[]>(row.supported_asset_types_json, [])
  };
}

function mapManifest(row: MysqlRow): ManifestRecord {
  const templateConfigJson = parseJson(row.template_config_json, {
    templateId: '',
    templateVersion: '',
    designWidth: 0,
    designHeight: 0,
    slots: []
  });
  const playbackPlanJson = parseJson(row.playback_plan_json, {
    planId: '',
    playMode: '',
    scenes: []
  });
  const cachePolicyJson = parseJson(row.cache_policy_json, {
    maxCacheSizeMb: 0,
    minFreeStorageMb: 0,
    allowDeleteUnusedAssets: false
  });
  const fallbackPolicyJson = parseJson(row.fallback_policy_json, {
    fallbackAssetId: '',
    fallbackText: '',
    maxRetryCount: 0,
    retryIntervalSec: 0,
    loopLastGoodManifest: false,
    showBlackScreenAllowed: false
  });

  return {
    manifestId: String(row.manifest_id),
    version: Number(row.version),
    tenantId: String(row.tenant_id),
    deviceId: String(row.device_id),
    locationId: String(row.location_id),
    groupId: row.group_id ? String(row.group_id) : undefined,
    validFrom: Number(row.valid_from),
    validTo: Number(row.valid_to),
    ttlSec: Number(row.ttl_sec),
    templateId: String(row.template_id),
    templateVersion: String(row.template_version),
    designWidth: Number(row.design_width),
    designHeight: Number(row.design_height),
    playMode: String(row.play_mode),
    templateConfigJson,
    playbackPlanJson,
    assets: [],
    cachePolicy: cachePolicyJson,
    fallbackPolicy: fallbackPolicyJson,
    checksum: String(row.checksum),
    generatedAt: Number(row.generated_at)
  };
}

function mapAssetRow(row: MysqlRow): ManifestRecord['assets'][number] {
  return {
    assetId: String(row.asset_id),
    assetType: String(row.asset_type),
    fileName: String(row.file_name),
    assetRef: String(row.asset_ref),
    ossPath: String(row.oss_path),
    cdnPath: String(row.cdn_path),
    mimeType: String(row.mime_type),
    sizeBytes: Number(row.size_bytes),
    sha256: String(row.sha256),
    durationMs: row.duration_ms === null ? 0 : Number(row.duration_ms),
    required: row.required === null ? false : Boolean(row.required),
    priority: row.priority === null ? 0 : Number(row.priority),
    width: row.width === null ? undefined : Number(row.width),
    height: row.height === null ? undefined : Number(row.height),
    expireAt: row.expire_at === null ? undefined : Number(row.expire_at)
  };
}

export class SqlRedisStore implements DeviceRepository, ManifestRepository, AssetRepository, RuntimeStateRepository {
  constructor(
    private readonly pool: Pool,
    private readonly redis: ReturnType<typeof createClient>
  ) {}

  static async create(config: AppConfig): Promise<SqlRedisStore> {
    const pool = mysql.createPool({
      uri: config.mysqlUrl,
      connectionLimit: 10,
      namedPlaceholders: false
    });
    const redis = createClient({ url: config.redisUrl });
    await redis.connect();
    return new SqlRedisStore(pool, redis);
  }

  async close(): Promise<void> {
    await this.redis.quit();
    await this.pool.end();
  }

  async saveDevice(device: DeviceRecord, config: PlayerConfigRecord): Promise<void> {
    await this.pool.execute(
      `INSERT INTO devices (
        device_id, tenant_id, location_id, group_id, device_sn, activation_code, device_name,
        platform, app_version, os_version, firmware_version, screen_resolution, timezone,
        mac_address, ip_address, capabilities_json, access_token, token_expire_at, status
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      ON DUPLICATE KEY UPDATE
        tenant_id = VALUES(tenant_id),
        location_id = VALUES(location_id),
        group_id = VALUES(group_id),
        activation_code = VALUES(activation_code),
        device_name = VALUES(device_name),
        platform = VALUES(platform),
        app_version = VALUES(app_version),
        os_version = VALUES(os_version),
        firmware_version = VALUES(firmware_version),
        screen_resolution = VALUES(screen_resolution),
        timezone = VALUES(timezone),
        mac_address = VALUES(mac_address),
        ip_address = VALUES(ip_address),
        capabilities_json = VALUES(capabilities_json),
        access_token = VALUES(access_token),
        token_expire_at = VALUES(token_expire_at),
        status = VALUES(status),
        updated_at = CURRENT_TIMESTAMP(3)`,
      [
        device.deviceId,
        device.tenantId,
        device.locationId,
        device.groupId ?? null,
        device.deviceSn,
        device.activationCode,
        device.deviceName,
        device.platform,
        device.appVersion,
        device.osVersion,
        device.firmwareVersion ?? null,
        device.screenResolution,
        device.timezone,
        device.macAddress,
        device.ipAddress,
        serializeJson(device.capabilities),
        device.accessToken,
        device.tokenExpireAt,
        device.status
      ]
    );

    await this.pool.execute(
      `INSERT INTO player_configs (
        device_id, heartbeat_interval_sec, manifest_sync_interval_sec, event_flush_interval_sec,
        max_cache_size_mb, asset_download_concurrency, enable_offline_mode, enable_watchdog,
        enable_screenshot, log_level, supported_asset_types_json
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      ON DUPLICATE KEY UPDATE
        heartbeat_interval_sec = VALUES(heartbeat_interval_sec),
        manifest_sync_interval_sec = VALUES(manifest_sync_interval_sec),
        event_flush_interval_sec = VALUES(event_flush_interval_sec),
        max_cache_size_mb = VALUES(max_cache_size_mb),
        asset_download_concurrency = VALUES(asset_download_concurrency),
        enable_offline_mode = VALUES(enable_offline_mode),
        enable_watchdog = VALUES(enable_watchdog),
        enable_screenshot = VALUES(enable_screenshot),
        log_level = VALUES(log_level),
        supported_asset_types_json = VALUES(supported_asset_types_json),
        updated_at = CURRENT_TIMESTAMP(3)`,
      [
        config.deviceId,
        config.heartbeatIntervalSec,
        config.manifestSyncIntervalSec,
        config.eventFlushIntervalSec,
        config.maxCacheSizeMb,
        config.assetDownloadConcurrency,
        config.enableOfflineMode,
        config.enableWatchdog,
        config.enableScreenshot,
        config.logLevel,
        serializeJson(config.supportedAssetTypes)
      ]
    );
  }

  async findById(deviceId: string): Promise<DeviceRecord | undefined> {
    const [rows] = await this.pool.query<MysqlRow[]>('SELECT * FROM devices WHERE device_id = ? LIMIT 1', [deviceId]);
    const row = rows[0];
    return row ? mapDevice(row) : undefined;
  }

  async findConfigByDeviceId(deviceId: string): Promise<PlayerConfigRecord | undefined> {
    const [rows] = await this.pool.query<MysqlRow[]>(
      `SELECT pc.*, d.tenant_id, d.location_id
       FROM player_configs pc
       JOIN devices d ON d.device_id = pc.device_id
       WHERE pc.device_id = ?
       LIMIT 1`,
      [deviceId]
    );
    const row = rows[0];
    return row ? mapConfig(row) : undefined;
  }

  async saveManifest(manifest: ManifestRecord): Promise<void> {
    await this.pool.execute(
      `INSERT INTO manifests (
        manifest_id, version, tenant_id, device_id, location_id, group_id, valid_from, valid_to,
        ttl_sec, template_id, template_version, design_width, design_height, play_mode,
        template_config_json, playback_plan_json, cache_policy_json, fallback_policy_json,
        checksum, generated_at, is_active
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      ON DUPLICATE KEY UPDATE
        version = VALUES(version),
        tenant_id = VALUES(tenant_id),
        device_id = VALUES(device_id),
        location_id = VALUES(location_id),
        group_id = VALUES(group_id),
        valid_from = VALUES(valid_from),
        valid_to = VALUES(valid_to),
        ttl_sec = VALUES(ttl_sec),
        template_id = VALUES(template_id),
        template_version = VALUES(template_version),
        design_width = VALUES(design_width),
        design_height = VALUES(design_height),
        play_mode = VALUES(play_mode),
        template_config_json = VALUES(template_config_json),
        playback_plan_json = VALUES(playback_plan_json),
        cache_policy_json = VALUES(cache_policy_json),
        fallback_policy_json = VALUES(fallback_policy_json),
        checksum = VALUES(checksum),
        generated_at = VALUES(generated_at),
        is_active = VALUES(is_active),
        updated_at = CURRENT_TIMESTAMP(3)`,
      [
        manifest.manifestId,
        manifest.version,
        manifest.tenantId,
        manifest.deviceId,
        manifest.locationId,
        manifest.groupId ?? null,
        manifest.validFrom,
        manifest.validTo,
        manifest.ttlSec,
        manifest.templateId,
        manifest.templateVersion,
        manifest.designWidth,
        manifest.designHeight,
        manifest.playMode,
        serializeJson(manifest.templateConfigJson),
        serializeJson(manifest.playbackPlanJson),
        serializeJson(manifest.cachePolicy),
        serializeJson(manifest.fallbackPolicy),
        manifest.checksum,
        manifest.generatedAt,
        1
      ]
    );

    await this.pool.execute('UPDATE manifests SET is_active = 0 WHERE device_id = ? AND manifest_id <> ?', [
      manifest.deviceId,
      manifest.manifestId
    ]);

    for (const asset of manifest.assets) {
      await this.pool.execute(
        `INSERT INTO assets (
          asset_id, tenant_id, asset_type, file_name, asset_ref, oss_path, cdn_path, mime_type,
          size_bytes, sha256, duration_ms, width, height, expire_at, metadata_json
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON DUPLICATE KEY UPDATE
          tenant_id = VALUES(tenant_id),
          asset_type = VALUES(asset_type),
          file_name = VALUES(file_name),
          asset_ref = VALUES(asset_ref),
          oss_path = VALUES(oss_path),
          cdn_path = VALUES(cdn_path),
          mime_type = VALUES(mime_type),
          size_bytes = VALUES(size_bytes),
          sha256 = VALUES(sha256),
          duration_ms = VALUES(duration_ms),
          width = VALUES(width),
          height = VALUES(height),
          expire_at = VALUES(expire_at),
          metadata_json = VALUES(metadata_json),
          updated_at = CURRENT_TIMESTAMP(3)`,
        [
          asset.assetId,
          manifest.tenantId,
          asset.assetType,
          asset.fileName,
          asset.assetRef,
          asset.ossPath,
          asset.cdnPath,
          asset.mimeType,
          asset.sizeBytes,
          asset.sha256,
          asset.durationMs,
          asset.width ?? null,
          asset.height ?? null,
          asset.expireAt ?? null,
          serializeJson({})
        ]
      );

      await this.pool.execute(
        `INSERT INTO manifest_assets (manifest_id, asset_id, required, priority)
         VALUES (?, ?, ?, ?)
         ON DUPLICATE KEY UPDATE
           required = VALUES(required),
           priority = VALUES(priority),
           updated_at = CURRENT_TIMESTAMP(3)`,
        [manifest.manifestId, asset.assetId, asset.required, asset.priority]
      );
    }
  }

  async findLatestByDevice(deviceId: string): Promise<ManifestRecord | undefined> {
    const [rows] = await this.pool.query<MysqlRow[]>(
      `SELECT * FROM manifests WHERE device_id = ? ORDER BY is_active DESC, version DESC LIMIT 1`,
      [deviceId]
    );
    const row = rows[0];
    if (!row) {
      return undefined;
    }
    const manifest = mapManifest(row);
    manifest.assets = await this.findAssetsByIds(deviceId, []);
    return manifest;
  }

  async findAssetsByIds(deviceId: string, assetIds: string[]): Promise<ManifestRecord['assets']> {
    const latest = await this.pool.query<MysqlRow[]>(
      `SELECT m.manifest_id
       FROM manifests m
       WHERE m.device_id = ?
       ORDER BY m.is_active DESC, m.version DESC
       LIMIT 1`,
      [deviceId]
    );
    const manifestId = latest[0][0]?.manifest_id;
    if (!manifestId) {
      return [];
    }

    let sql =
      `SELECT a.*, ma.required, ma.priority
       FROM manifest_assets ma
       JOIN assets a ON a.asset_id = ma.asset_id
       WHERE ma.manifest_id = ?`;
    const params: Array<string | number> = [String(manifestId)];
    if (assetIds.length > 0) {
      sql += ` AND ma.asset_id IN (${assetIds.map(() => '?').join(', ')})`;
      params.push(...assetIds);
    }

    const [rows] = await this.pool.query<MysqlRow[]>(sql, params);
    return rows.map(mapAssetRow);
  }

  async saveHeartbeat(state: DeviceHeartbeatState): Promise<void> {
    await this.redis.set(
      heartbeatKey(state.deviceId),
      serializeJson(state)
    );
  }

  async findHeartbeat(deviceId: string): Promise<DeviceHeartbeatState | undefined> {
    const raw = await this.redis.get(heartbeatKey(deviceId));
    return raw ? parseJson<DeviceHeartbeatState>(raw, undefined as never) : undefined;
  }

  async saveSyncState(state: DeviceSyncState): Promise<void> {
    await this.redis.set(syncKey(state.deviceId), serializeJson(state));
  }

  async findSyncState(deviceId: string): Promise<DeviceSyncState | undefined> {
    const raw = await this.redis.get(syncKey(deviceId));
    return raw ? parseJson<DeviceSyncState>(raw, undefined as never) : undefined;
  }

  async appendEvents(events: PlayerEventRecord[]): Promise<void> {
    if (events.length === 0) {
      return;
    }

    const values = events.flatMap((event) => [
      event.deviceId,
      event.eventId,
      event.eventType,
      event.timestamp,
      event.manifestId ?? null,
      event.manifestVersion ?? null,
      event.assetId ?? null,
      event.playlistItemId ?? null,
      event.errorCode ?? null,
      event.errorMessage ?? null,
      event.extraJson ?? null
    ]);

    const placeholders = events
      .map(() => '(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)')
      .join(', ');

    await this.pool.execute(
      `INSERT INTO device_events (
        device_id, event_id, event_type, timestamp, manifest_id, manifest_version,
        asset_id, playlist_item_id, error_code, error_message, extra_json
      ) VALUES ${placeholders}`,
      values
    );
  }

  async listEventsByDevice(deviceId: string): Promise<PlayerEventRecord[]> {
    const [rows] = await this.pool.query<MysqlRow[]>(
      `SELECT * FROM device_events WHERE device_id = ? ORDER BY timestamp DESC LIMIT 100`,
      [deviceId]
    );

    return rows.map((row) => ({
      deviceId: String(row.device_id),
      eventId: String(row.event_id),
      eventType: String(row.event_type),
      timestamp: Number(row.timestamp),
      manifestId: row.manifest_id ? String(row.manifest_id) : undefined,
      manifestVersion: row.manifest_version === null ? undefined : Number(row.manifest_version),
      assetId: row.asset_id ? String(row.asset_id) : undefined,
      playlistItemId: row.playlist_item_id ? String(row.playlist_item_id) : undefined,
      errorCode: row.error_code ? String(row.error_code) : undefined,
      errorMessage: row.error_message ? String(row.error_message) : undefined,
      extraJson: row.extra_json ? String(row.extra_json) : undefined
    }));
  }

  async enqueueCommand(command: PendingCommandRecord): Promise<void> {
    await this.pool.execute(
      `INSERT INTO commands (command_id, device_id, type, payload_json, status, issued_at, expire_at)
       VALUES (?, ?, ?, ?, ?, ?, ?)
       ON DUPLICATE KEY UPDATE
         type = VALUES(type),
         payload_json = VALUES(payload_json),
         status = VALUES(status),
         issued_at = VALUES(issued_at),
         expire_at = VALUES(expire_at),
         updated_at = CURRENT_TIMESTAMP(3)`,
      [command.commandId, command.deviceId, command.type, command.payloadJson, 'pending', command.issuedAt, command.expireAt]
    );
    await this.redis.rPush(pendingCommandsKey(command.deviceId), serializeJson(command));
  }

  async listPendingCommands(deviceId: string, now: number): Promise<PendingCommandRecord[]> {
    const raw = await this.redis.lRange(pendingCommandsKey(deviceId), 0, -1);
    if (raw.length > 0) {
      return raw
        .map((item) => parseJson<PendingCommandRecord>(item, undefined as never))
        .filter((command) => command && command.expireAt > now);
    }

    const [rows] = await this.pool.query<MysqlRow[]>(
      `SELECT command_id, device_id, type, issued_at, expire_at, payload_json
       FROM commands
       WHERE device_id = ? AND status = 'pending' AND expire_at > ?
       ORDER BY issued_at ASC`,
      [deviceId, now]
    );

    return rows.map((row) => ({
      commandId: String(row.command_id),
      deviceId: String(row.device_id),
      type: String(row.type),
      issuedAt: Number(row.issued_at),
      expireAt: Number(row.expire_at),
      payloadJson: String(row.payload_json)
    }));
  }

  async ackCommand(record: CommandAckRecord): Promise<void> {
    await this.pool.execute(
      `INSERT INTO command_acks (
        command_id, device_id, type, success, error_code, error_message, executed_at, received_at
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
      ON DUPLICATE KEY UPDATE
        success = VALUES(success),
        error_code = VALUES(error_code),
        error_message = VALUES(error_message),
        executed_at = VALUES(executed_at),
        received_at = VALUES(received_at)`,
      [
        record.commandId,
        record.deviceId,
        record.type,
        record.success,
        record.errorCode ?? null,
        record.errorMessage ?? null,
        record.executedAt,
        record.receivedAt
      ]
    );

    await this.pool.execute(`UPDATE commands SET status = ? WHERE command_id = ?`, [
      record.success ? 'acked' : 'failed',
      record.commandId
    ]);

    const commands = await this.redis.lRange(pendingCommandsKey(record.deviceId), 0, -1);
    if (commands.length > 0) {
      const filtered = commands.filter((item) => parseJson<PendingCommandRecord>(item, undefined as never)?.commandId !== record.commandId);
      await this.redis.del(pendingCommandsKey(record.deviceId));
      if (filtered.length > 0) {
        await this.redis.rPush(pendingCommandsKey(record.deviceId), filtered);
      }
    }
  }

  async listCommandAcks(deviceId: string): Promise<CommandAckRecord[]> {
    const [rows] = await this.pool.query<MysqlRow[]>(
      `SELECT * FROM command_acks WHERE device_id = ? ORDER BY received_at DESC LIMIT 100`,
      [deviceId]
    );

    return rows.map((row) => ({
      deviceId: String(row.device_id),
      commandId: String(row.command_id),
      type: String(row.type),
      success: Boolean(row.success),
      errorCode: row.error_code ? String(row.error_code) : undefined,
      errorMessage: row.error_message ? String(row.error_message) : undefined,
      executedAt: Number(row.executed_at),
      receivedAt: Number(row.received_at)
    }));
  }
}
