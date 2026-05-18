-- Database: digital_signage (passed via connection URL in CI)

SET NAMES utf8mb4;

-- ============================================================
-- MVP Seed Data
-- - 1 screen (web player)
-- - 1 player config
-- - 2 media items (main video + fallback image)
-- - 1 manifest
-- - 2 manifest_media entries
-- ============================================================

-- Clean up existing seed data
DELETE FROM command_acks WHERE screen_id = (SELECT id FROM screen WHERE device_code = 'device_web_lobby_001' LIMIT 1);
DELETE FROM commands WHERE screen_id = (SELECT id FROM screen WHERE device_code = 'device_web_lobby_001' LIMIT 1);
DELETE FROM device_event_log WHERE screen_id = (SELECT id FROM screen WHERE device_code = 'device_web_lobby_001' LIMIT 1);
DELETE FROM manifest_media WHERE manifest_id = 'manifest_web_lobby_v1';
DELETE FROM manifests WHERE manifest_id = 'manifest_web_lobby_v1';
DELETE FROM player_configs WHERE screen_id = (SELECT id FROM screen WHERE device_code = 'device_web_lobby_001' LIMIT 1);
DELETE FROM screen WHERE device_code = 'device_web_lobby_001';
DELETE FROM media WHERE object_key IN ('mvp/lobby_promo_main.mp4', 'mvp/fallback_poster.png');

-- Insert screen
INSERT INTO screen (
  device_code,
  device_token,
  organization_id,
  name,
  activation_code,
  activation_status,
  status,
  last_heartbeat_at,
  app_version,
  resolution_width,
  resolution_height
) VALUES (
  'device_web_lobby_001',
  'mvp-device-token-web-lobby-001',
  1,
  'Lobby Web Player',
  'ACTIVATE_WEB_001',
  'activated',
  'active',
  NOW(6),
  '0.1.0',
  1920,
  1080
);

SET @screen_id = LAST_INSERT_ID();

-- Insert player config
INSERT INTO player_configs (
  screen_id,
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
  @screen_id,
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

-- Insert media items
INSERT INTO media (
  organization_id,
  media_type,
  name,
  object_key,
  file_url,
  thumbnail_url,
  file_size_bytes,
  duration_seconds,
  checksum_sha256
) VALUES
(
  1,
  'ASSET_VIDEO',
  'lobby_promo_main.mp4',
  'mvp/lobby_promo_main.mp4',
  'https://cdn.example.com/mvp/lobby_promo_main.mp4',
  NULL,
  15728640,
  15,
  '1111111111111111111111111111111111111111111111111111111111111111'
),
(
  1,
  'ASSET_IMAGE',
  'fallback_poster.png',
  'mvp/fallback_poster.png',
  'https://cdn.example.com/mvp/fallback_poster.png',
  NULL,
  204800,
  0,
  '2222222222222222222222222222222222222222222222222222222222222222'
);

SET @media_video_id = LAST_INSERT_ID();
SET @media_image_id = @media_video_id + 1;

-- Insert manifest
INSERT INTO manifests (
  manifest_id,
  version,
  screen_id,
  organization_id,
  layout_id,
  valid_from,
  valid_to,
  ttl_sec,
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
  @screen_id,
  1,
  NULL,
  1779000000000,
  0,
  3600,
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
            'media_id', @media_video_id,
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
            'media_id', NULL,
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
    'fallback_media_id', @media_image_id,
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

-- Insert manifest_media entries
INSERT INTO manifest_media (
  manifest_id,
  media_id,
  `required`,
  priority
) VALUES
(
  'manifest_web_lobby_v1',
  @media_video_id,
  1,
  10
),
(
  'manifest_web_lobby_v1',
  @media_image_id,
  0,
  1
);
