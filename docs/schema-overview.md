# Schema Overview

本文档说明 `sql/schema.sql` 中各张表的职责、关键字段和相互关系。

## 设计原则

- `MySQL` 保存结构化主数据和可追溯历史数据
- `Redis` 继续承担高频运行态数据，例如最新心跳、待执行命令和最近同步状态
- `schema.sql` 采用混合版方案：高确定性对象结构化，Manifest 内部复杂编排先放 JSON

## 表清单

### 1. `devices`

职责：保存播放器设备的主数据与接入信息。

关键字段：

- `device_id`：设备业务主键
- `tenant_id` / `location_id`：租户与点位维度
- `group_id`：分组标识，可空，用于兼容未来批量下发或分组策略
- `device_sn`：设备序列号
- `activation_code`：注册使用的激活码
- `platform` / `app_version` / `os_version`：运行环境信息
- `capabilities_json`：设备能力集
- `access_token` / `token_expire_at`：设备访问凭证
- `status`：设备状态，例如 `active` / `inactive` / `offline`

索引建议：

- `device_id` 唯一
- `device_sn` 唯一
- `tenant_id + location_id` 用于按点位查询设备

### 2. `player_configs`

职责：保存设备级基础配置。

关键字段：

- `heartbeat_interval_sec`
- `manifest_sync_interval_sec`
- `event_flush_interval_sec`
- `max_cache_size_mb`
- `asset_download_concurrency`
- `enable_offline_mode`
- `enable_watchdog`
- `enable_screenshot`
- `supported_asset_types_json`

关系：

- 与 `devices` 为一对一关系

### 3. `assets`

职责：保存资源元数据。

关键字段：

- `asset_id`
- `asset_type`
- `file_name`
- `asset_ref`
- `oss_path`
- `cdn_path`
- `mime_type`
- `size_bytes`
- `sha256`
- `duration_ms`
- `width` / `height`
- `expire_at`

说明：

- 这里保存的是资源元数据，不直接保存最终签名下载地址
- `oss_path` 表示对象存储原始路径
- `cdn_path` 表示静态 CDN 基础路径
- 真正下载地址由 `assets/batch-url` 接口在运行时基于资源信息生成

### 4. `manifests`

职责：保存 Manifest 主记录。

关键字段：

- `manifest_id`
- `version`
- `device_id`
- `tenant_id`
- `location_id`
- `group_id`
- `valid_from` / `valid_to`
- `ttl_sec`
- `template_id` / `template_version`
- `design_width` / `design_height`
- `play_mode`
- `template_config_json`
- `playback_plan_json`
- `cache_policy_json`
- `fallback_policy_json`
- `checksum`
- `generated_at`
- `is_active`

说明：

- 该表保存 Manifest 主头信息和核心编排 JSON
- `template_config_json` 保存模板槽位和布局约束
- `playback_plan_json` 保存播放场景、场景顺序和槽位绑定
- 这样能减少 Manifest 演进阶段的频繁改表成本

### 5. `manifest_assets`

职责：保存 Manifest 与资源的引用关系。

关键字段：

- `manifest_id`
- `asset_id`
- `required`
- `priority`

说明：

- 用于快速判断某个 Manifest 依赖了哪些资源
- 支撑 `assets/batch-url` 和缓存校验流程

### 6. `commands`

职责：保存下发给设备的远程命令。

关键字段：

- `command_id`
- `device_id`
- `type`
- `status`
- `payload_json`
- `issued_at`
- `expire_at`

说明：

- `status` 可取 `pending` / `acked` / `expired` / `failed`
- 运行时可从 Redis 做快速读取，MySQL 用于持久化与审计

### 7. `command_acks`

职责：保存设备对命令的执行回执。

关键字段：

- `command_id`
- `device_id`
- `type`
- `success`
- `error_code`
- `error_message`
- `executed_at`
- `received_at`

关系：

- 与 `commands` 基本是一对一关系

### 8. `device_events`

职责：保存设备关键事件历史。

关键字段：

- `event_id`
- `device_id`
- `event_type`
- `manifest_id`
- `manifest_version`
- `asset_id`
- `playlist_item_id`
- `error_code`
- `error_message`
- `extra_json`
- `event_timestamp`

说明：

- 虽然高频运行态建议优先走 Redis，但历史事件仍建议落 MySQL 便于查询和审计

## 表关系摘要

- `devices` 1:1 `player_configs`
- `devices` 1:N `manifests`
- `manifests` N:N `assets`，通过 `manifest_assets` 关联
- `devices` 1:N `commands`
- `commands` 1:1 `command_acks`
- `devices` 1:N `device_events`

## 当前不放进 MySQL 的内容

以下数据更适合继续先放 `Redis`：

- 最新心跳状态
- 最近同步状态
- 最近关键事件缓存
- 待返回 heartbeat 的命令列表

对应设计可继续参考：`docs/persistence-design.md`
