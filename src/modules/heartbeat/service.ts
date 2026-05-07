import { store } from '../../lib/app-context.js';

export interface HeartbeatRequest {
  device_id: string;
  app_version: string;
  manifest_id: string;
  manifest_version: number;
  timestamp: number;
  playback: Record<string, unknown>;
  health: Record<string, unknown>;
  cache: Record<string, unknown>;
  network: Record<string, unknown>;
}

export async function reportHeartbeat(input: HeartbeatRequest) {
  const now = Date.now();

  await store.saveHeartbeat({
    deviceId: input.device_id,
    lastHeartbeatAt: input.timestamp,
    appVersion: input.app_version,
    manifestId: input.manifest_id,
    manifestVersion: input.manifest_version,
    playback: input.playback,
    health: input.health,
    cache: input.cache,
    network: input.network,
    updatedAt: now
  });

  const currentSyncState = await store.findSyncState(input.device_id);
  await store.saveSyncState({
    deviceId: input.device_id,
    layoutVersion: currentSyncState?.layoutVersion,
    scheduleVersion: currentSyncState?.scheduleVersion,
    manifestVersion: input.manifest_version,
    lastSyncAt: currentSyncState?.lastSyncAt,
    lastOnlineAt: input.timestamp
  });

  const pendingCommands = await store.listPendingCommands(input.device_id, now);

  if (input.device_id.endsWith('demo') && pendingCommands.length === 0) {
    await store.enqueueCommand({
      commandId: 'cmd_sync_now',
      deviceId: input.device_id,
      type: 'COMMAND_SYNC_NOW',
      issuedAt: now,
      expireAt: now + 60 * 1000,
      payloadJson: '{}'
    });
  }

  return {
    success: true,
    next_interval_sec: 30,
    commands: (await store.listPendingCommands(input.device_id, now)).map((command) => ({
      command_id: command.commandId,
      type: command.type,
      issued_at: command.issuedAt,
      expire_at: command.expireAt,
      payload_json: command.payloadJson
    }))
  };
}
