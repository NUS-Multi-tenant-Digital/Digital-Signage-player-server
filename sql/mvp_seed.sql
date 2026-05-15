USE player_application_server;

SET NAMES utf8mb4;

-- Fixed sample scope for MVP Day 1:
-- - 1 web device
-- - 1 player config
-- - 1 manifest
-- - 2 assets (main video + fallback image)
-- - 1 template only: TEMPLATE_VIDEO_WITH_BOTTOM_TEXT

DELETE FROM command_acks
WHERE device_id = 'device_web_lobby_001';

DELETE FROM commands
WHERE device_id = 'device_web_lobby_001';

DELETE FROM device_events
WHERE device_id = 'device_web_lobby_001';

DELETE FROM manifest_assets
WHERE manifest_id = 'manifest_web_lobby_v1';

DELETE FROM manifests
WHERE manifest_id = 'manifest_web_lobby_v1';

DELETE FROM player_configs
WHERE device_id = 'device_web_lobby_001';

DELETE FROM devices
WHERE device_id = 'device_web_lobby_001';

DELETE FROM assets
WHERE asset_id IN ('asset_video_lobby_001', 'asset_fallback_image_001');

INSERT INTO devices (
  device_id,
  tenant_id,
  location_id,
  group_id,
  device_sn,
  activation_code,
  device_name,
  platform,
  app_version,
  os_version,
  firmware_version,
  screen_resolution,
  timezone,
  mac_address,
  ip_address,
  capabilities_json,
  access_token,
  token_expire_at,
  status
) VALUES (
  'device_web_lobby_001',
  'tenant_demo',
  'location_lobby_001',
  'group_lobby',
  'SN_WEB_LOBBY_001',
  'ACTIVATE_WEB_001',
  'Lobby Web Player',
  'PLATFORM_WEB',
  '0.1.0',
  'Chrome 136',
  NULL,
  '1920x1080',
  'Asia/Shanghai',
  '02:00:00:00:10:01',
  '10.0.0.21',
  JSON_OBJECT(
    'video', true,
    'image', true,
    'text', true,
    'offline_cache', true,
    'screenshot', false
  ),
  'mvp-access-token-device-web-lobby-001',
  1893456000000,
  'active'
);

INSERT INTO player_configs (
  device_id,
  tenant_id,
  location_id,
  heartbeat_interval_sec,
  manifest_sync_interval_sec,
  event_flush_interval_sec,
  max_cache_size_mb,
  asset_download_concurrency,
  enable_offline_mode,
  enable_watchdog,
  enable_screenshot,
  log_level,
  supported_asset_types_json
) VALUES (
  'device_web_lobby_001',
  'tenant_demo',
  'location_lobby_001',
  30,
  60,
  30,
  2048,
  3,
  1,
  1,
  0,
  'info',
  JSON_ARRAY('ASSET_VIDEO', 'ASSET_IMAGE')
);

INSERT INTO assets (
  asset_id,
  tenant_id,
  asset_type,
  file_name,
  asset_ref,
  oss_path,
  cdn_path,
  mime_type,
  size_bytes,
  sha256,
  duration_ms,
  width,
  height,
  expire_at,
  metadata_json
) VALUES
(
  'asset_video_lobby_001',
  'tenant_demo',
  'ASSET_VIDEO',
  'lobby_promo_main.mp4',
  'cdn://mvp/lobby_promo_main.mp4',
  'oss://player-assets/mvp/lobby_promo_main.mp4',
  'https://cdn.example.com/mvp/lobby_promo_main.mp4',
  'video/mp4',
  15728640,
  '1111111111111111111111111111111111111111111111111111111111111111',
  15000,
  1920,
  1080,
  NULL,
  JSON_OBJECT('scene', 'lobby', 'usage', 'main_video')
),
(
  'asset_fallback_image_001',
  'tenant_demo',
  'ASSET_IMAGE',
  'fallback_poster.png',
  'cdn://mvp/fallback_poster.png',
  'oss://player-assets/mvp/fallback_poster.png',
  'https://cdn.example.com/mvp/fallback_poster.png',
  'image/png',
  204800,
  '2222222222222222222222222222222222222222222222222222222222222222',
  0,
  1920,
  1080,
  NULL,
  JSON_OBJECT('scene', 'lobby', 'usage', 'fallback')
);

INSERT INTO manifests (
  manifest_id,
  version,
  tenant_id,
  device_id,
  location_id,
  group_id,
  valid_from,
  valid_to,
  ttl_sec,
  template_id,
  template_version,
  design_width,
  design_height,
  play_mode,
  template_config_json,
  playback_plan_json,
  cache_policy_json,
  fallback_policy_json,
  checksum,
  generated_at,
  is_active
) VALUES (
  'manifest_web_lobby_v1',
  1,
  'tenant_demo',
  'device_web_lobby_001',
  'location_lobby_001',
  'group_lobby',
  1779000000000,
  0,
  3600,
  'TEMPLATE_VIDEO_WITH_BOTTOM_TEXT',
  '1.0.0',
  1920,
  1080,
  'PLAY_MODE_LOOP',
  JSON_OBJECT(
    'template_id', 'TEMPLATE_VIDEO_WITH_BOTTOM_TEXT',
    'template_version', '1.0.0',
    'design_width', 1920,
    'design_height', 1080,
    'slots', JSON_ARRAY(
      JSON_OBJECT(
        'slot_id', 'main_video',
        'slot_type', 'SLOT_VIDEO',
        'required', true
      ),
      JSON_OBJECT(
        'slot_id', 'bottom_text',
        'slot_type', 'SLOT_TEXT',
        'required', false
      )
    )
  ),
  JSON_OBJECT(
    'plan_id', 'plan_lobby_default',
    'play_mode', 'PLAY_MODE_LOOP',
    'scenes', JSON_ARRAY(
      JSON_OBJECT(
        'scene_id', 'scene_lobby_main',
        'scene_name', 'Lobby Main Scene',
        'duration_ms', 15000,
        'order', 1,
        'slot_bindings', JSON_ARRAY(
          JSON_OBJECT(
            'slot_id', 'main_video',
            'content_type', 'CONTENT_ASSET',
            'asset_id', 'asset_video_lobby_001',
            'text', '',
            'display_policy', JSON_OBJECT(
              'object_fit', 'cover',
              'muted', true,
              'loop', true
            )
          ),
          JSON_OBJECT(
            'slot_id', 'bottom_text',
            'content_type', 'CONTENT_TEXT',
            'asset_id', '',
            'text', 'Welcome to our store',
            'display_policy', JSON_OBJECT(
              'object_fit', 'fill',
              'muted', false,
              'loop', false
            )
          )
        )
      )
    )
  ),
  JSON_OBJECT(
    'max_cache_size_mb', 2048,
    'min_free_storage_mb', 512,
    'allow_delete_unused_assets', true
  ),
  JSON_OBJECT(
    'fallback_asset_id', 'asset_fallback_image_001',
    'fallback_text', 'Content temporarily unavailable',
    'max_retry_count', 3,
    'retry_interval_sec', 10,
    'loop_last_good_manifest', true,
    'show_black_screen_allowed', false
  ),
  'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa',
  1779000000000,
  1
);

INSERT INTO manifest_assets (
  manifest_id,
  asset_id,
  required,
  priority
) VALUES
(
  'manifest_web_lobby_v1',
  'asset_video_lobby_001',
  1,
  10
),
(
  'manifest_web_lobby_v1',
  'asset_fallback_image_001',
  0,
  1
);
