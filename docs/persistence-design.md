# Persistence Design

该文档用于把当前仓库中的内存 `store` 与后续真实存储方案对应起来。

## 总体原则

- `MySQL` 保存结构化主数据
- `Redis` 保存高频更新的运行态数据
- 当前代码中的 `InMemoryStore` 只是这两者的临时替身
- 设备、资源、命令、事件走结构化表
- Manifest 内部复杂编排先以 `JSON` 形式存主表，降低演进期改表成本

## MySQL 表设计

### 1. devices

最终字段：

- `device_id`
- `tenant_id`
- `location_id`
- `group_id`
- `device_sn`
- `activation_code`
- `device_name`
- `platform`
- `app_version`
- `os_version`
- `firmware_version`
- `screen_resolution`
- `timezone`
- `mac_address`
- `ip_address`
- `capabilities_json`
- `access_token`
- `token_expire_at`
- `status`
- `created_at`
- `updated_at`

### 2. player_configs

最终字段：

- `device_id`
- `heartbeat_interval_sec`
- `manifest_sync_interval_sec`
- `event_flush_interval_sec`
- `max_cache_size_mb`
- `asset_download_concurrency`
- `enable_offline_mode`
- `enable_watchdog`
- `enable_screenshot`
- `log_level`
- `supported_asset_types_json`

### 3. manifests

最终字段：

- `manifest_id`
- `device_id`
- `tenant_id`
- `location_id`
- `group_id`
- `version`
- `valid_from`
- `valid_to`
- `ttl_sec`
- `template_id`
- `template_version`
- `design_width`
- `design_height`
- `play_mode`
- `template_config_json`
- `playback_plan_json`
- `cache_policy_json`
- `fallback_policy_json`
- `checksum`
- `generated_at`

说明：

- `template_config_json` 保存模板和槽位定义
- `playback_plan_json` 保存场景、播放顺序和槽位绑定
- 资源引用关系不直接塞在 `manifests` 表中，而是由 `manifest_assets` 关联

### 4. assets

最终字段：

- `asset_id`
- `tenant_id`
- `asset_type`
- `file_name`
- `asset_ref`
- `oss_path`
- `cdn_path`
- `mime_type`
- `size_bytes`
- `sha256`
- `duration_ms`
- `width`
- `height`
- `expire_at`
- `metadata_json`

说明：

- `oss_path` 用于指向对象存储原始文件
- `cdn_path` 用于生成播放器最终下载地址

### 5. manifest_assets

最终字段：

- `manifest_id`
- `asset_id`
- `required`
- `priority`

### 6. command_acks

最终字段：

- `command_id`
- `device_id`
- `type`
- `success`
- `error_code`
- `error_message`
- `executed_at`
- `received_at`

## Redis Key 设计

### 1. 设备心跳状态

Key:

```text
player:device:{deviceId}:heartbeat
```

Value 字段：

- `lastHeartbeatAt`
- `appVersion`
- `manifestId`
- `manifestVersion`
- `playback`
- `health`
- `cache`
- `network`

### 2. 设备同步状态

Key:

```text
player:device:{deviceId}:sync
```

Value 字段：

- `layoutVersion`
- `scheduleVersion`
- `manifestVersion`
- `lastSyncAt`
- `lastOnlineAt`

### 3. 设备事件流

Key:

```text
player:device:{deviceId}:events
```

Value 结构：

- 使用 `Redis List` 或 `Stream`
- 保存最近 N 条关键事件

### 4. 待执行命令

Key:

```text
player:device:{deviceId}:commands:pending
```

Value 结构：

- 保存 heartbeat 待返回的命令列表
- 过期命令由 TTL 或消费逻辑清理

## 当前代码映射

当前 `InMemoryStore` 已覆盖以下能力：

- 设备主数据
- Player 配置
- 最新 Manifest
- Manifest 对应的资源列表
- 心跳状态
- 同步状态
- 事件列表
- 待执行命令
- 命令 ACK

后续替换顺序建议：

1. 先把 `DeviceRepository` 和 `ManifestRepository` 替换成 MySQL 实现
2. 再把 `RuntimeStateRepository` 替换成 Redis 实现
3. 最后补对象存储与 CDN 签名 URL 生成服务
