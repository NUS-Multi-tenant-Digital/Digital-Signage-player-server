-- ============================================================
-- MVP Seed Data - v2 manifest (演示 Case 6：内容更新后自动刷新)
--
-- 前置：先执行 sql/mvp_seed.sql（已建好 screen / media / v1 manifest）。
-- 执行本脚本后，同一台设备(device_code = 'LOCAL-DEMO-SN-001')会存在
-- version=2 的新 manifest。播放器下次 PullManifest 时，服务端比较
-- 客户端 version(1) < 服务端最新 version(2)，返回 MANIFEST_FULL_UPDATE，
-- 从而触发前端的“内容更新后自动刷新”流程。
-- ============================================================

SET NAMES utf8mb4;

SET @screen_id = (SELECT id FROM screen WHERE device_code = 'LOCAL-DEMO-SN-001' LIMIT 1);
SET @media_video_id = (SELECT id FROM media WHERE object_key = 'mvp/lobby_promo_main.mp4' LIMIT 1);
SET @media_image_id = (SELECT id FROM media WHERE object_key = 'mvp/fallback_poster.png' LIMIT 1);

-- 清理可能存在的旧 v2 数据，便于重复执行
DELETE FROM manifest_media WHERE manifest_id = 'manifest_web_lobby_v2';
DELETE FROM manifests WHERE manifest_id = 'manifest_web_lobby_v2';

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
  'manifest_web_lobby_v2',
  2,
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
      JSON_OBJECT('slot_id', 'main_video', 'slot_type', 'SLOT_VIDEO', 'required', true),
      JSON_OBJECT('slot_id', 'bottom_text', 'slot_type', 'SLOT_TEXT', 'required', false)
    )
  ),
  JSON_OBJECT(
    'plan_id', 'plan_lobby_v2',
    'play_mode', 'PLAY_MODE_LOOP',
    'scenes', JSON_ARRAY(
      JSON_OBJECT(
        'scene_id', 'scene_lobby_main_v2',
        'scene_name', 'Lobby Main Scene v2',
        'duration_ms', 12000,
        'order', 1,
        'slot_bindings', JSON_ARRAY(
          JSON_OBJECT(
            'slot_id', 'main_video',
            'content_type', 'CONTENT_ASSET',
            'media_id', @media_video_id,
            'text', '',
            'display_policy', JSON_OBJECT('object_fit', 'cover', 'muted', true, 'loop', true)
          ),
          JSON_OBJECT(
            'slot_id', 'bottom_text',
            'content_type', 'CONTENT_TEXT',
            'media_id', NULL,
            'text', 'New content pushed - manifest v2 is live',
            'display_policy', JSON_OBJECT('object_fit', 'fill', 'muted', false, 'loop', false)
          )
        )
      )
    )
  ),
  JSON_OBJECT('max_cache_size_mb', 2048, 'min_free_storage_mb', 512, 'allow_delete_unused_assets', true),
  JSON_OBJECT(
    'fallback_media_id', @media_image_id,
    'fallback_text', 'Content temporarily unavailable',
    'max_retry_count', 3,
    'retry_interval_sec', 10,
    'loop_last_good_manifest', true,
    'show_black_screen_allowed', false
  ),
  'bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb',
  1779000000001,
  1
);

INSERT INTO manifest_media (manifest_id, media_id, `required`, priority) VALUES
('manifest_web_lobby_v2', @media_video_id, 1, 10),
('manifest_web_lobby_v2', @media_image_id, 0, 1);
