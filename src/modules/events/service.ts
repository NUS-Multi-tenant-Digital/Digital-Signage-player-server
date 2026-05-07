import { store } from '../../lib/app-context.js';

export interface PlayerEventItem {
  event_id: string;
  event_type: string;
  timestamp: number;
  manifest_id?: string;
  manifest_version?: number;
  asset_id?: string;
  playlist_item_id?: string;
  error_code?: string;
  error_message?: string;
  extra_json?: string;
}

export interface ReportPlayerEventsRequest {
  device_id: string;
  events: PlayerEventItem[];
}

export async function reportPlayerEvents(input: ReportPlayerEventsRequest) {
  const records = input.events.map((event) => ({
    deviceId: input.device_id,
    eventId: event.event_id,
    eventType: event.event_type,
    timestamp: event.timestamp,
    manifestId: event.manifest_id,
    manifestVersion: event.manifest_version,
    assetId: event.asset_id,
    playlistItemId: event.playlist_item_id,
    errorCode: event.error_code,
    errorMessage: event.error_message,
    extraJson: event.extra_json
  }));

  await store.appendEvents(records);

  const latestEvent = records.at(-1);
  if (latestEvent?.manifestVersion !== undefined) {
    const current = await store.findSyncState(input.device_id);
    await store.saveSyncState({
      deviceId: input.device_id,
      layoutVersion: current?.layoutVersion,
      scheduleVersion: current?.scheduleVersion,
      manifestVersion: latestEvent.manifestVersion,
      lastSyncAt: latestEvent.timestamp,
      lastOnlineAt: current?.lastOnlineAt ?? latestEvent.timestamp
    });
  }

  return {
    success: true,
    accepted_count: records.length,
    rejected_count: 0
  };
}
