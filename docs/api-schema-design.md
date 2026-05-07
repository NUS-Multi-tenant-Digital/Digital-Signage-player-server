# API Schema Design

本文档定义当前各接口的 request / response schema 设计，用于后续接入参数校验和 OpenAPI。

## 通用响应封装

成功：

```json
{
  "success": true,
  "data": {},
  "message": "ok"
}
```

失败：

```json
{
  "success": false,
  "error": {
    "code": "PLAYER_BAD_REQUEST",
    "message": "Bad request",
    "details": {}
  }
}
```

## 1. POST /api/v1/player/register

### Request

必填字段：

- `device_sn`: string
- `activation_code`: string
- `device_name`: string
- `platform`: string
- `app_version`: string
- `os_version`: string
- `screen_resolution`: string
- `timezone`: string
- `mac_address`: string
- `ip_address`: string
- `capabilities`: object

可选字段：

- `firmware_version`: string

### Response

- `device_id`: string
- `tenant_id`: string
- `location_id`: string
- `access_token`: string
- `token_expire_at`: number
- `config`: object

`config` 字段：

- `device_id`: string
- `tenant_id`: string
- `location_id`: string
- `heartbeat_interval_sec`: number
- `manifest_sync_interval_sec`: number
- `event_flush_interval_sec`: number
- `max_cache_size_mb`: number
- `asset_download_concurrency`: number
- `enable_offline_mode`: boolean
- `enable_watchdog`: boolean
- `enable_screenshot`: boolean
- `log_level`: string
- `supported_asset_types`: string[]

## 2. POST /api/v1/player/events

### Request

- `device_id`: string
- `events`: array

`events[]` 字段：

- `event_id`: string
- `event_type`: string
- `timestamp`: number
- `manifest_id`: string | null
- `manifest_version`: number | null
- `asset_id`: string | null
- `playlist_item_id`: string | null
- `error_code`: string | null
- `error_message`: string | null
- `extra_json`: string | null

### Response

- `success`: boolean
- `accepted_count`: number
- `rejected_count`: number

## 3. POST /api/v1/player/manifest/pull

### Request

- `device_id`: string
- `tenant_id`: string
- `location_id`: string
- `current_manifest_id`: string | null
- `current_manifest_version`: number | null
- `app_version`: string
- `platform`: string
- `screen_resolution`: string
- `last_success_sync_at`: number | null

### Response

- `update_type`: string
- `manifest`: object | null
- `next_poll_interval_sec`: number
- `server_time`: number
- `message`: string

`manifest` 详细结构：见 `docs/manifest-contract.md`

## 4. POST /api/v1/assets/batch-url

### Request

- `device_id`: string
- `manifest_id`: string
- `manifest_version`: number
- `asset_ids`: string[]

### Response

- `assets`: array

`assets[]` 字段：

- `asset_id`: string
- `download_url`: string
- `expire_at`: number
- `sha256`: string
- `size_bytes`: number

## 5. POST /api/v1/player/heartbeat

### Request

- `device_id`: string
- `app_version`: string
- `manifest_id`: string
- `manifest_version`: number
- `timestamp`: number
- `playback`: object
- `health`: object
- `cache`: object
- `network`: object

### Response

- `success`: boolean
- `next_interval_sec`: number
- `commands`: array

`commands[]` 字段：

- `command_id`: string
- `type`: string
- `issued_at`: number
- `expire_at`: number
- `payload_json`: string

## 6. POST /api/v1/player/commands/ack

### Request

- `device_id`: string
- `command_id`: string
- `type`: string
- `success`: boolean
- `error_code`: string | null
- `error_message`: string | null
- `executed_at`: number

### Response

- `success`: boolean

## 校验建议

- 所有 `*_id` 字段统一使用非空字符串
- 所有时间字段统一使用 Unix 毫秒时间戳
- `asset_ids`、`events` 这类数组字段必须至少包含一项
- `extra_json`、`payload_json` 在进入业务层前建议做 JSON 合法性校验
