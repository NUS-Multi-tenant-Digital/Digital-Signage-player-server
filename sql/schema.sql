-- ============================================================
-- Player Application Server - Schema
-- Only player-server-specific tables.
-- Tables `screen`, `media`, `organization` are owned by the
-- admin backend (Digital-Signage-admin-backend).
-- ============================================================

-- Database: digital_signage
-- When running locally: CREATE DATABASE IF NOT EXISTS digital_signage;
-- CI passes the database name via connection URL.

-- --------------------------------------------------------
-- Admin-backend owned tables (for reference / local dev)
-- --------------------------------------------------------

CREATE TABLE IF NOT EXISTS screen (
  id BIGINT NOT NULL AUTO_INCREMENT,
  device_code VARCHAR(64) NOT NULL,
  device_token VARCHAR(512) NULL,
  organization_id BIGINT NOT NULL,
  name VARCHAR(255) NULL,
  activation_code VARCHAR(128) NULL,
  activation_status VARCHAR(32) NULL DEFAULT 'pending',
  status VARCHAR(32) NULL DEFAULT 'offline',
  last_heartbeat_at DATETIME(6) NULL,
  app_version VARCHAR(64) NULL,
  resolution_width INT NULL,
  resolution_height INT NULL,
  screen_group_id BIGINT NULL,
  ws_status VARCHAR(32) NULL,
  last_ws_connected_at DATETIME(6) NULL,
  last_ws_message_at DATETIME(6) NULL,
  probe_fail_count INT NULL DEFAULT 0,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_screen_device_code (device_code),
  KEY idx_screen_organization_id (organization_id),
  KEY idx_screen_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS media (
  id BIGINT NOT NULL AUTO_INCREMENT,
  organization_id BIGINT NOT NULL,
  media_type VARCHAR(32) NOT NULL,
  name VARCHAR(255) NOT NULL,
  object_key VARCHAR(512) NOT NULL,
  file_url VARCHAR(1024) NULL,
  thumbnail_url VARCHAR(1024) NULL,
  file_size_bytes BIGINT NULL,
  duration_seconds INT NULL DEFAULT 0,
  checksum_sha256 VARCHAR(64) NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_media_object_key (object_key),
  KEY idx_media_organization_type (organization_id, media_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------
-- Player-server owned tables
-- --------------------------------------------------------

CREATE TABLE IF NOT EXISTS player_configs (
  id BIGINT NOT NULL AUTO_INCREMENT,
  screen_id BIGINT NOT NULL,
  heartbeat_interval_sec INT NOT NULL DEFAULT 30,
  manifest_sync_interval_sec INT NOT NULL DEFAULT 60,
  event_flush_interval_sec INT NOT NULL DEFAULT 30,
  max_cache_size_mb BIGINT NOT NULL DEFAULT 2048,
  asset_download_concurrency INT NOT NULL DEFAULT 3,
  enable_offline_mode TINYINT(1) NOT NULL DEFAULT 1,
  enable_watchdog TINYINT(1) NOT NULL DEFAULT 1,
  enable_screenshot TINYINT(1) NOT NULL DEFAULT 0,
  log_level VARCHAR(16) NOT NULL DEFAULT 'info',
  supported_asset_types_json JSON NOT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_player_configs_screen_id (screen_id),
  CONSTRAINT fk_player_configs_screen_id
    FOREIGN KEY (screen_id) REFERENCES screen(id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS manifests (
  id BIGINT NOT NULL AUTO_INCREMENT,
  manifest_id VARCHAR(64) NOT NULL,
  version BIGINT NOT NULL,
  screen_id BIGINT NOT NULL,
  organization_id BIGINT NOT NULL,
  layout_id BIGINT NULL,
  valid_from BIGINT NOT NULL,
  valid_to BIGINT NOT NULL DEFAULT 0,
  ttl_sec INT NOT NULL DEFAULT 3600,
  template_config_json JSON NOT NULL,
  playback_plan_json JSON NOT NULL,
  cache_policy_json JSON NOT NULL,
  fallback_policy_json JSON NOT NULL,
  checksum VARCHAR(64) NOT NULL,
  generated_at BIGINT NOT NULL,
  is_active TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_manifests_manifest_id (manifest_id),
  UNIQUE KEY uk_manifests_screen_version (screen_id, version),
  KEY idx_manifests_screen_active (screen_id, is_active),
  KEY idx_manifests_organization_id (organization_id),
  CONSTRAINT fk_manifests_screen_id
    FOREIGN KEY (screen_id) REFERENCES screen(id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS manifest_media (
  id BIGINT NOT NULL AUTO_INCREMENT,
  manifest_id VARCHAR(64) NOT NULL,
  media_id BIGINT NOT NULL,
  `required` TINYINT(1) NOT NULL DEFAULT 1,
  priority INT NOT NULL DEFAULT 0,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_manifest_media_manifest_media (manifest_id, media_id),
  KEY idx_manifest_media_media_id (media_id),
  CONSTRAINT fk_manifest_media_manifest_id
    FOREIGN KEY (manifest_id) REFERENCES manifests(manifest_id)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT fk_manifest_media_media_id
    FOREIGN KEY (media_id) REFERENCES media(id)
    ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS commands (
  id BIGINT NOT NULL AUTO_INCREMENT,
  command_id VARCHAR(64) NOT NULL,
  screen_id BIGINT NOT NULL,
  type VARCHAR(64) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'pending',
  payload_json JSON NOT NULL,
  issued_at BIGINT NOT NULL,
  expire_at BIGINT NOT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_commands_command_id (command_id),
  KEY idx_commands_screen_status (screen_id, status),
  KEY idx_commands_expire_at (expire_at),
  CONSTRAINT fk_commands_screen_id
    FOREIGN KEY (screen_id) REFERENCES screen(id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS command_acks (
  id BIGINT NOT NULL AUTO_INCREMENT,
  command_id VARCHAR(64) NOT NULL,
  screen_id BIGINT NOT NULL,
  type VARCHAR(64) NOT NULL,
  success TINYINT(1) NOT NULL,
  error_code VARCHAR(64) NULL,
  error_message VARCHAR(512) NULL,
  executed_at BIGINT NOT NULL,
  received_at BIGINT NOT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_command_acks_command_id (command_id),
  KEY idx_command_acks_screen_id (screen_id),
  CONSTRAINT fk_command_acks_command_id
    FOREIGN KEY (command_id) REFERENCES commands(command_id)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT fk_command_acks_screen_id
    FOREIGN KEY (screen_id) REFERENCES screen(id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS device_events (
  id BIGINT NOT NULL AUTO_INCREMENT,
  event_id VARCHAR(64) NOT NULL,
  screen_id BIGINT NOT NULL,
  event_type VARCHAR(64) NOT NULL,
  manifest_id VARCHAR(64) NULL,
  manifest_version BIGINT NULL,
  media_id BIGINT NULL,
  playlist_item_id VARCHAR(64) NULL,
  error_code VARCHAR(64) NULL,
  error_message VARCHAR(512) NULL,
  extra_json JSON NULL,
  event_timestamp BIGINT NOT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_device_events_event_id (event_id),
  KEY idx_device_events_screen_timestamp (screen_id, event_timestamp),
  KEY idx_device_events_event_type (event_type),
  CONSTRAINT fk_device_events_screen_id
    FOREIGN KEY (screen_id) REFERENCES screen(id)
    ON DELETE CASCADE,
  CONSTRAINT fk_device_events_manifest_id
    FOREIGN KEY (manifest_id) REFERENCES manifests(manifest_id)
    ON DELETE SET NULL
    ON UPDATE CASCADE,
  CONSTRAINT fk_device_events_media_id
    FOREIGN KEY (media_id) REFERENCES media(id)
    ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
