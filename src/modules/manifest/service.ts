import type { ManifestRecord } from '../../domain/manifest.js';
import { store } from '../../lib/app-context.js';

export interface PullManifestRequest {
  device_id: string;
  tenant_id: string;
  location_id: string;
  current_manifest_id?: string;
  current_manifest_version?: number;
  app_version: string;
  platform: string;
  screen_resolution: string;
  last_success_sync_at?: number;
}

function buildManifest(input: PullManifestRequest, now: number): ManifestRecord {
  const templateConfig = {
    templateId: 'TEMPLATE_VIDEO_WITH_BOTTOM_TEXT',
    templateVersion: '1.0.0',
    designWidth: 1920,
    designHeight: 1080,
    slots: [
      { slotId: 'main_video', slotType: 'SLOT_VIDEO', required: true },
      { slotId: 'bottom_text', slotType: 'SLOT_TEXT', required: false }
    ]
  };

  const playbackPlan = {
    planId: 'plan_demo',
    playMode: 'PLAY_MODE_LOOP',
    scenes: [
      {
        sceneId: 'scene_1',
        sceneName: 'Default Scene',
        durationMs: 15000,
        order: 1,
        slotBindings: [
          {
            slotId: 'main_video',
            contentType: 'CONTENT_ASSET',
            assetId: 'video_001',
            text: '',
            displayPolicy: { objectFit: 'cover', muted: true, loop: true }
          }
        ]
      }
    ]
  };

  return {
    manifestId: 'manifest_v1',
    version: 1,
    tenantId: input.tenant_id,
    deviceId: input.device_id,
    locationId: input.location_id,
    groupId: undefined,
    validFrom: now,
    validTo: 0,
    ttlSec: 3600,
    templateId: templateConfig.templateId,
    templateVersion: templateConfig.templateVersion,
    designWidth: templateConfig.designWidth,
    designHeight: templateConfig.designHeight,
    playMode: playbackPlan.playMode,
    templateConfigJson: templateConfig,
    playbackPlanJson: playbackPlan,
    assets: [
      {
        assetId: 'video_001',
        assetType: 'ASSET_VIDEO',
        fileName: 'promo_001.mp4',
        assetRef: 'cdn://promo_001.mp4',
        ossPath: 'oss://player-assets/promo_001.mp4',
        cdnPath: 'https://cdn.example.com/assets/promo_001.mp4',
        mimeType: 'video/mp4',
        sizeBytes: 12345678,
        sha256: 'demo-sha256',
        durationMs: 15000,
        required: true,
        priority: 10
      }
    ],
    cachePolicy: {
      maxCacheSizeMb: 2048,
      minFreeStorageMb: 512,
      allowDeleteUnusedAssets: true
    },
    fallbackPolicy: {
      fallbackAssetId: 'fallback_001',
      fallbackText: 'Content temporarily unavailable',
      maxRetryCount: 3,
      retryIntervalSec: 10,
      loopLastGoodManifest: true,
      showBlackScreenAllowed: false
    },
    checksum: 'manifest-checksum',
    generatedAt: now
  };
}

export async function pullManifest(input: PullManifestRequest) {
  const now = Date.now();
  const existing = await store.findLatestByDevice(input.device_id);
  const manifest = existing ?? buildManifest(input, now);

  if (!existing) {
    await store.saveManifest(manifest);
  }

  await store.saveSyncState({
    deviceId: input.device_id,
    manifestVersion: manifest.version,
    lastSyncAt: now,
    lastOnlineAt: now
  });

  return {
    update_type:
      input.current_manifest_version && input.current_manifest_version >= manifest.version
        ? 'MANIFEST_NO_UPDATE'
        : 'MANIFEST_FULL_UPDATE',
    manifest:
      input.current_manifest_version && input.current_manifest_version >= manifest.version
      ? null
      : {
          manifest_id: manifest.manifestId,
          version: manifest.version,
          tenant_id: manifest.tenantId,
          device_id: manifest.deviceId,
          location_id: manifest.locationId,
          group_id: manifest.groupId ?? null,
          valid_from: manifest.validFrom,
          valid_to: manifest.validTo,
          ttl_sec: manifest.ttlSec,
          template_config: {
            template_id: manifest.templateId,
            template_version: manifest.templateVersion,
            design_width: manifest.designWidth,
            design_height: manifest.designHeight,
            slots: manifest.templateConfigJson.slots.map((slot) => ({
              slot_id: slot.slotId,
              slot_type: slot.slotType,
              required: slot.required
            }))
          },
          playback_plan: {
            plan_id: manifest.playbackPlanJson.planId,
            play_mode: manifest.playMode,
            scenes: manifest.playbackPlanJson.scenes.map((scene) => ({
              scene_id: scene.sceneId,
              scene_name: scene.sceneName,
              duration_ms: scene.durationMs,
              order: scene.order,
              slot_bindings: scene.slotBindings.map((binding) => ({
                slot_id: binding.slotId,
                content_type: binding.contentType,
                asset_id: binding.assetId,
                text: binding.text,
                display_policy: {
                  object_fit: binding.displayPolicy.objectFit,
                  muted: binding.displayPolicy.muted,
                  loop: binding.displayPolicy.loop
                }
              }))
            }))
          },
          assets: manifest.assets.map((asset) => ({
            asset_id: asset.assetId,
            asset_type: asset.assetType,
            file_name: asset.fileName,
            asset_ref: asset.assetRef,
            oss_path: asset.ossPath,
            cdn_path: asset.cdnPath,
            mime_type: asset.mimeType,
            size_bytes: asset.sizeBytes,
            sha256: asset.sha256,
            duration_ms: asset.durationMs,
            required: asset.required,
            priority: asset.priority
          })),
          cache_policy: {
            max_cache_size_mb: manifest.cachePolicy.maxCacheSizeMb,
            min_free_storage_mb: manifest.cachePolicy.minFreeStorageMb,
            allow_delete_unused_assets: manifest.cachePolicy.allowDeleteUnusedAssets
          },
          fallback_policy: {
            fallback_asset_id: manifest.fallbackPolicy.fallbackAssetId,
            fallback_text: manifest.fallbackPolicy.fallbackText,
            max_retry_count: manifest.fallbackPolicy.maxRetryCount,
            retry_interval_sec: manifest.fallbackPolicy.retryIntervalSec,
            loop_last_good_manifest: manifest.fallbackPolicy.loopLastGoodManifest,
            show_black_screen_allowed: manifest.fallbackPolicy.showBlackScreenAllowed
          },
          checksum: manifest.checksum,
          generated_at: manifest.generatedAt
        },
    next_poll_interval_sec: 60,
    server_time: now,
    message: 'Manifest response generated from scaffold service'
  };
}
