# MVP Day 1 Baseline

本文档用于固定 MVP 第一阶段的联调基线，避免后续开发和联调过程中每个人使用不同的设备数据、模板和资源口径。

## 1. Day 1 要完成什么

Day 1 的目标不是继续补设计，而是固定一套最小可跑数据，作为后续 `manifest/pull`、`assets/batch-url`、`heartbeat` 和 Web player 联调的共同起点。

Day 1 完成后，应该具备：

- 1 台固定 Web player 设备
- 1 套固定播放器配置
- 1 份固定 manifest
- 2 个固定资源
- 1 套固定模板

## 2. 固定范围

本周先只支持以下范围：

- 设备平台：`PLATFORM_WEB`
- 屏幕分辨率：`1920x1080`
- 模板：`TEMPLATE_VIDEO_WITH_BOTTOM_TEXT`
- 播放模式：`PLAY_MODE_LOOP`
- 资源范围：
  - 1 个主视频资源
  - 1 个 fallback 图片资源
- 文字内容直接放在 `slot_bindings.text`

本周不扩展：

- 多模板联调
- 多设备并行联调
- 复杂分组命中逻辑
- 多场景编排

## 3. 固定样例数据

### 3.1 设备

- `device_id`: `device_web_lobby_001`
- `device_sn`: `SN_WEB_LOBBY_001`
- `activation_code`: `ACTIVATE_WEB_001`
- `tenant_id`: `tenant_demo`
- `location_id`: `location_lobby_001`
- `group_id`: `group_lobby`
- `platform`: `PLATFORM_WEB`
- `screen_resolution`: `1920x1080`

### 3.2 Player Config

- `heartbeat_interval_sec`: `30`
- `manifest_sync_interval_sec`: `60`
- `event_flush_interval_sec`: `30`
- `max_cache_size_mb`: `2048`
- `asset_download_concurrency`: `3`
- `enable_offline_mode`: `true`

### 3.3 Manifest

- `manifest_id`: `manifest_web_lobby_v1`
- `version`: `1`
- `template_id`: `TEMPLATE_VIDEO_WITH_BOTTOM_TEXT`
- `template_version`: `1.0.0`
- `plan_id`: `plan_lobby_default`

### 3.4 Assets

- 主视频资源：`asset_video_lobby_001`
- fallback 图片资源：`asset_fallback_image_001`

## 4. 固定模板口径

本周只围绕这一套模板联调：

- `template_id`: `TEMPLATE_VIDEO_WITH_BOTTOM_TEXT`

模板槽位：

- `main_video`
  - `slot_type = SLOT_VIDEO`
  - `required = true`
- `bottom_text`
  - `slot_type = SLOT_TEXT`
  - `required = false`

播放内容：

- `main_video` 绑定 `asset_video_lobby_001`
- `bottom_text` 绑定固定文案：`Welcome to our store`

Fallback：

- `fallback_asset_id = asset_fallback_image_001`
- `fallback_text = Content temporarily unavailable`

## 5. 导入步骤

先导入表结构：

```bash
# 任选一种方式执行
# 方式 1：本地 mysql 客户端
mysql -u <user> -p < /path/to/sql/schema.sql

# 方式 2：使用 DataGrip / TablePlus / Navicat 执行 schema.sql
```

再导入 Day 1 种子数据：

```bash
mysql -u <user> -p player_application_server < /path/to/sql/mvp_seed.sql
```

对应文件：

- 表结构：`sql/schema.sql`
- Day 1 种子数据：`sql/mvp_seed.sql`

如果本机没有 `mysql` 命令行客户端，也可以直接用 GUI 工具执行以上两个 SQL 文件。

## 6. 导入后检查

导入完成后，至少确认以下内容存在：

- `devices` 中存在 `device_web_lobby_001`
- `player_configs` 中存在 `device_web_lobby_001` 对应配置
- `manifests` 中存在 `manifest_web_lobby_v1`
- `assets` 中存在：
  - `asset_video_lobby_001`
  - `asset_fallback_image_001`
- `manifest_assets` 中存在 `manifest_web_lobby_v1` 对这两个资源的引用关系

## 7. 联调时统一使用的请求样例

### 7.1 Register

如果前端需要先走注册流程，可以统一使用：

```json
{
  "device_sn": "SN_WEB_LOBBY_001",
  "activation_code": "ACTIVATE_WEB_001",
  "device_name": "Lobby Web Player",
  "platform": "PLATFORM_WEB",
  "app_version": "0.1.0",
  "os_version": "Chrome 136",
  "screen_resolution": "1920x1080",
  "timezone": "Asia/Shanghai",
  "mac_address": "02:00:00:00:10:01",
  "ip_address": "10.0.0.21",
  "capabilities": {
    "video": true,
    "image": true,
    "text": true,
    "offline_cache": true,
    "screenshot": false
  }
}
```

### 7.2 Manifest Pull

```json
{
  "device_id": "device_web_lobby_001",
  "tenant_id": "tenant_demo",
  "location_id": "location_lobby_001",
  "current_manifest_id": null,
  "current_manifest_version": null,
  "app_version": "0.1.0",
  "platform": "PLATFORM_WEB",
  "screen_resolution": "1920x1080",
  "last_success_sync_at": null
}
```

### 7.3 Asset Batch URL

```json
{
  "device_id": "device_web_lobby_001",
  "manifest_id": "manifest_web_lobby_v1",
  "manifest_version": 1,
  "asset_ids": [
    "asset_video_lobby_001",
    "asset_fallback_image_001"
  ]
}
```

## 8. Day 1 完成标准

满足以下条件就算 Day 1 完成：

- 表结构已经可以初始化
- 有一套固定、可重复导入的 MVP 种子数据
- 模板已经固定为 `TEMPLATE_VIDEO_WITH_BOTTOM_TEXT`
- 后续开发和联调不再更换设备 ID、manifest ID、asset ID
- 所有人都基于同一套数据推进 Day 2 到 Day 7

## 9. 下一步

Day 1 完成后，下一步直接进入：

- `Day 2`: 跑通 `POST /api/v1/player/manifest/pull`
- 核心目标：让 `manifest/pull` 从真实存储返回固定样例数据，而不是继续依赖 demo 构造逻辑
