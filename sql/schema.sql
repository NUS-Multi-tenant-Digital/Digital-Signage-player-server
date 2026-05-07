CREATE DATABASE IF NOT EXISTS player_application_server
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE player_application_server;

CREATE TABLE IF NOT EXISTS devices (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  device_id VARCHAR(64) NOT NULL,
  tenant_id VARCHAR(64) NOT NULL,
  location_id VARCHAR(64) NOT NULL,
  group_id VARCHAR(64) NULL,
  device_sn VARCHAR(128) NOT NULL,
  activation_code VARCHAR(128) NOT NULL,
  device_name VARCHAR(128) NOT NULL,
  platform VARCHAR(64) NOT NULL,
  app_version VARCHAR(64) NOT NULL,
  os_version VARCHAR(64) NOT NULL,
  firmware_version VARCHAR(64) NULL,
  screen_resolution VARCHAR(32) NOT NULL,
  timezone VARCHAR(64) NOT NULL,
  mac_address VARCHAR(64) NOT NULL,
  ip_address VARCHAR(64) NOT NULL,
  capabilities_json JSON NOT NULL,
  access_token VARCHAR(255) NOT NULL,
  token_expire_at BIGINT NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'active',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_devices_device_id (device_id),
  UNIQUE KEY uk_devices_device_sn (device_sn),
  KEY idx_devices_tenant_location (tenant_id, location_id),
  KEY idx_devices_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS player_configs (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  device_id VARCHAR(64) NOT NULL,
  tenant_id VARCHAR(64) NOT NULL,
  location_id VARCHAR(64) NOT NULL,
  heartbeat_interval_sec INT NOT NULL,
  manifest_sync_interval_sec INT NOT NULL,
  event_flush_interval_sec INT NOT NULL,
  max_cache_size_mb BIGINT NOT NULL,
  asset_download_concurrency INT NOT NULL,
  enable_offline_mode TINYINT(1) NOT NULL DEFAULT 1,
  enable_watchdog TINYINT(1) NOT NULL DEFAULT 1,
  enable_screenshot TINYINT(1) NOT NULL DEFAULT 0,
  log_level VARCHAR(32) NOT NULL DEFAULT 'info',
  supported_asset_types_json JSON NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_player_configs_device_id (device_id),
  CONSTRAINT fk_player_configs_device_id
    FOREIGN KEY (device_id) REFERENCES devices(device_id)
    ON DELETE CASCADE
    ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS assets (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  asset_id VARCHAR(64) NOT NULL,
  tenant_id VARCHAR(64) NOT NULL,
  asset_type VARCHAR(32) NOT NULL,
  file_name VARCHAR(255) NOT NULL,
  asset_ref VARCHAR(255) NOT NULL,
  oss_path VARCHAR(255) NOT NULL,
  cdn_path VARCHAR(255) NOT NULL,
  mime_type VARCHAR(128) NOT NULL,
  size_bytes BIGINT NOT NULL,
  sha256 CHAR(64) NOT NULL,
  duration_ms BIGINT NOT NULL DEFAULT 0,
  width INT NULL,
  height INT NULL,
  expire_at BIGINT NULL,
  metadata_json JSON NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_assets_asset_id (asset_id),
  KEY idx_assets_tenant_type (tenant_id, asset_type),
  KEY idx_assets_sha256 (sha256)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS manifests (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  manifest_id VARCHAR(64) NOT NULL,
  version BIGINT NOT NULL,
  tenant_id VARCHAR(64) NOT NULL,
  device_id VARCHAR(64) NOT NULL,
  location_id VARCHAR(64) NOT NULL,
  group_id VARCHAR(64) NULL,
  valid_from BIGINT NOT NULL,
  valid_to BIGINT NOT NULL DEFAULT 0,
  ttl_sec INT NOT NULL,
  template_id VARCHAR(64) NOT NULL,
  template_version VARCHAR(64) NOT NULL,
  design_width INT NOT NULL,
  design_height INT NOT NULL,
  play_mode VARCHAR(32) NOT NULL,
  template_config_json JSON NOT NULL,
  playback_plan_json JSON NOT NULL,
  cache_policy_json JSON NOT NULL,
  fallback_policy_json JSON NOT NULL,
  checksum CHAR(64) NOT NULL,
  generated_at BIGINT NOT NULL,
  is_active TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_manifests_manifest_id (manifest_id),
  UNIQUE KEY uk_manifests_device_version (device_id, version),
  KEY idx_manifests_tenant_location (tenant_id, location_id),
  KEY idx_manifests_device_active (device_id, is_active),
  CONSTRAINT fk_manifests_device_id
    FOREIGN KEY (device_id) REFERENCES devices(device_id)
    ON DELETE CASCADE
    ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS manifest_assets (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  manifest_id VARCHAR(64) NOT NULL,
  asset_id VARCHAR(64) NOT NULL,
  required TINYINT(1) NOT NULL DEFAULT 1,
  priority INT NOT NULL DEFAULT 0,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_manifest_assets_manifest_asset (manifest_id, asset_id),
  KEY idx_manifest_assets_asset_id (asset_id),
  CONSTRAINT fk_manifest_assets_manifest_id
    FOREIGN KEY (manifest_id) REFERENCES manifests(manifest_id)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT fk_manifest_assets_asset_id
    FOREIGN KEY (asset_id) REFERENCES assets(asset_id)
    ON DELETE RESTRICT
    ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS commands (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  command_id VARCHAR(64) NOT NULL,
  device_id VARCHAR(64) NOT NULL,
  type VARCHAR(64) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'pending',
  payload_json JSON NOT NULL,
  issued_at BIGINT NOT NULL,
  expire_at BIGINT NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_commands_command_id (command_id),
  KEY idx_commands_device_status (device_id, status),
  KEY idx_commands_expire_at (expire_at),
  CONSTRAINT fk_commands_device_id
    FOREIGN KEY (device_id) REFERENCES devices(device_id)
    ON DELETE CASCADE
    ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS command_acks (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  command_id VARCHAR(64) NOT NULL,
  device_id VARCHAR(64) NOT NULL,
  type VARCHAR(64) NOT NULL,
  success TINYINT(1) NOT NULL,
  error_code VARCHAR(64) NULL,
  error_message VARCHAR(255) NULL,
  executed_at BIGINT NOT NULL,
  received_at BIGINT NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_command_acks_command_id (command_id),
  KEY idx_command_acks_device_id (device_id),
  CONSTRAINT fk_command_acks_command_id
    FOREIGN KEY (command_id) REFERENCES commands(command_id)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT fk_command_acks_device_id
    FOREIGN KEY (device_id) REFERENCES devices(device_id)
    ON DELETE CASCADE
    ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS device_events (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  event_id VARCHAR(64) NOT NULL,
  device_id VARCHAR(64) NOT NULL,
  event_type VARCHAR(64) NOT NULL,
  manifest_id VARCHAR(64) NULL,
  manifest_version BIGINT NULL,
  asset_id VARCHAR(64) NULL,
  playlist_item_id VARCHAR(64) NULL,
  error_code VARCHAR(64) NULL,
  error_message VARCHAR(255) NULL,
  extra_json JSON NULL,
  event_timestamp BIGINT NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_device_events_event_id (event_id),
  KEY idx_device_events_device_timestamp (device_id, event_timestamp),
  KEY idx_device_events_event_type (event_type),
  CONSTRAINT fk_device_events_device_id
    FOREIGN KEY (device_id) REFERENCES devices(device_id)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT fk_device_events_manifest_id
    FOREIGN KEY (manifest_id) REFERENCES manifests(manifest_id)
    ON DELETE SET NULL
    ON UPDATE CASCADE,
  CONSTRAINT fk_device_events_asset_id
    FOREIGN KEY (asset_id) REFERENCES assets(asset_id)
    ON DELETE SET NULL
    ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
