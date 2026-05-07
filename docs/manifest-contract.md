# Manifest Contract

本文档定义 `POST /api/v1/player/manifest/pull` 返回的 `PlayerManifest` 最终 JSON 结构，用于统一前端模板消费、播放器播放逻辑和服务端组装方式。

## 目标

- 前端拿到 `template_config` 后能够明确模板槽位与布局约束
- 播放器拿到 `playback_plan` 和 `assets` 后能够直接进入资源校验与播放流程
- 服务端能够稳定判断 `NO_UPDATE` 和 `FULL_UPDATE`

## Pull Manifest 请求

路径：`POST /api/v1/player/manifest/pull`

请求字段：

- `device_id`
- `tenant_id`
- `location_id`
- `current_manifest_id`
- `current_manifest_version`
- `app_version`
- `platform`
- `screen_resolution`
- `last_success_sync_at`

标准请求样例：

```json
{
  "device_id": "device_tv_001",
  "tenant_id": "tenant_demo",
  "location_id": "location_lobby_001",
  "current_manifest_id": "manifest_v1",
  "current_manifest_version": 1,
  "app_version": "1.0.3",
  "platform": "PLATFORM_ANDROID_TV",
  "screen_resolution": "1920x1080",
  "last_success_sync_at": 1778069000000
}
```

## Pull Manifest 响应

响应外层字段：

- `update_type`
- `manifest`
- `next_poll_interval_sec`
- `server_time`
- `message`

### 1. 无更新响应

```json
{
  "update_type": "MANIFEST_NO_UPDATE",
  "manifest": null,
  "next_poll_interval_sec": 60,
  "server_time": 1778069005000,
  "message": "No manifest update"
}
```

### 2. 有更新响应

```json
{
  "update_type": "MANIFEST_FULL_UPDATE",
  "manifest": {
    "manifest_id": "manifest_v2",
    "version": 2,
    "tenant_id": "tenant_demo",
    "device_id": "device_tv_001",
    "location_id": "location_lobby_001",
    "group_id": null,
    "valid_from": 1778069005000,
    "valid_to": 0,
    "ttl_sec": 3600,
    "template_config": {
      "template_id": "TEMPLATE_VIDEO_WITH_SIDE_IMAGE",
      "template_version": "1.1.0",
      "design_width": 1920,
      "design_height": 1080,
      "slots": [
        {
          "slot_id": "main_video",
          "slot_type": "SLOT_VIDEO",
          "required": true
        },
        {
          "slot_id": "side_image",
          "slot_type": "SLOT_IMAGE",
          "required": false
        },
        {
          "slot_id": "bottom_text",
          "slot_type": "SLOT_TEXT",
          "required": false
        }
      ]
    },
    "playback_plan": {
      "plan_id": "plan_lobby_001",
      "play_mode": "PLAY_MODE_LOOP",
      "scenes": [
        {
          "scene_id": "scene_morning",
          "scene_name": "Morning Promotion",
          "duration_ms": 15000,
          "order": 1,
          "slot_bindings": [
            {
              "slot_id": "main_video",
              "content_type": "CONTENT_ASSET",
              "asset_id": "video_001",
              "text": "",
              "display_policy": {
                "object_fit": "cover",
                "muted": true,
                "loop": true
              }
            },
            {
              "slot_id": "side_image",
              "content_type": "CONTENT_ASSET",
              "asset_id": "image_002",
              "text": "",
              "display_policy": {
                "object_fit": "contain",
                "muted": false,
                "loop": false
              }
            },
            {
              "slot_id": "bottom_text",
              "content_type": "CONTENT_TEXT",
              "asset_id": "",
              "text": "Welcome to our store",
              "display_policy": {
                "object_fit": "fill",
                "muted": false,
                "loop": false
              }
            }
          ]
        }
      ]
    },
    "assets": [
      {
        "asset_id": "video_001",
        "asset_type": "ASSET_VIDEO",
        "file_name": "promo_001.mp4",
        "asset_ref": "cdn://promo_001.mp4",
        "oss_path": "oss://player-assets/promo_001.mp4",
        "cdn_path": "https://cdn.example.com/assets/promo_001.mp4",
        "mime_type": "video/mp4",
        "size_bytes": 12345678,
        "sha256": "sha256-video-001",
        "duration_ms": 15000,
        "required": true,
        "priority": 10
      },
      {
        "asset_id": "image_002",
        "asset_type": "ASSET_IMAGE",
        "file_name": "promo_side_002.png",
        "asset_ref": "cdn://promo_side_002.png",
        "oss_path": "oss://player-assets/promo_side_002.png",
        "cdn_path": "https://cdn.example.com/assets/promo_side_002.png",
        "mime_type": "image/png",
        "size_bytes": 204800,
        "sha256": "sha256-image-002",
        "duration_ms": 0,
        "required": false,
        "priority": 6
      }
    ],
    "cache_policy": {
      "max_cache_size_mb": 2048,
      "min_free_storage_mb": 512,
      "allow_delete_unused_assets": true
    },
    "fallback_policy": {
      "fallback_asset_id": "fallback_001",
      "fallback_text": "Content temporarily unavailable",
      "max_retry_count": 3,
      "retry_interval_sec": 10,
      "loop_last_good_manifest": true,
      "show_black_screen_allowed": false
    },
    "checksum": "sha256-manifest-v2",
    "generated_at": 1778069005000
  },
  "next_poll_interval_sec": 60,
  "server_time": 1778069005000,
  "message": "Manifest updated"
}
```

## 字段分层

### 1. `template_config`

职责：定义前端模板和槽位边界。

字段：

- `template_id`：前端内置模板标识
- `template_version`：模板版本
- `design_width` / `design_height`：设计稿尺寸
- `slots[]`：模板所需槽位定义

说明：

- 前端优先根据 `template_id + slots` 决定渲染容器
- `template_config` 不承载播放顺序，只定义页面结构与槽位能力

### 2. `playback_plan`

职责：定义播放计划与场景切换顺序。

字段：

- `plan_id`
- `play_mode`
- `scenes[]`

说明：

- `scenes[]` 是设备端执行的播放场景序列
- 每个 `scene` 通过 `slot_bindings[]` 把模板槽位和内容绑定起来

### 3. `assets`

职责：提供资源元数据和资源定位信息，不直接返回最终签名下载链接。

字段：

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
- `required`
- `priority`

说明：

- `cdn_path` 是静态资源基础路径，不等同于最终带时效的下载地址
- 真正下载地址通过 `POST /api/v1/assets/batch-url` 获取
- `assets` 用于播放器做本地索引、缓存比对和资源依赖判断

### 4. `cache_policy`

职责：定义本地缓存上限和清理约束。

### 5. `fallback_policy`

职责：定义下载失败、播放失败和离线场景下的兜底策略。

## 服务端组装原则

- 先确定设备当前生效的 Manifest 版本
- 再查该 Manifest 引用的资源元数据
- 若版本未变化，返回 `MANIFEST_NO_UPDATE`
- 若版本变化，返回完整 `PlayerManifest`
- `checksum` 必须覆盖整个 Manifest 逻辑内容，而不是只覆盖部分字段

## 前端与播放器消费边界

前端关注：

- `template_config`
- `slot` 能力定义
- 模板版本兼容性

播放器关注：

- `update_type`
- `playback_plan`
- `assets`
- `cache_policy`
- `fallback_policy`
- `checksum`

## DSL 设计规则

### 1. 职责边界

- `template_config` 只负责描述模板结构、槽位能力和设计尺寸，不负责播放顺序
- `playback_plan` 只负责描述场景编排、场景顺序和槽位内容绑定
- `assets` 只负责描述资源元数据和资源定位信息，不直接承担页面结构定义
- `cache_policy` 和 `fallback_policy` 只负责运行时策略，不参与模板结构解释

### 2. 槽位规则

- `slot_id` 在同一个 `template_config` 内必须唯一
- `slot_type` 用于声明槽位能承载的内容类型，当前按 `SLOT_VIDEO`、`SLOT_IMAGE`、`SLOT_TEXT` 约束
- `required = true` 表示该槽位在最终可播放场景中必须被满足，否则播放器应进入 fallback 或降级逻辑
- 前端渲染时应先根据 `template_id` 决定模板，再根据 `slots` 创建对应槽位容器

固定枚举：

- `TemplateId`
  - `TEMPLATE_FULL_SCREEN_VIDEO`
  - `TEMPLATE_FULL_SCREEN_IMAGE`
  - `TEMPLATE_VIDEO_WITH_BOTTOM_TEXT`
  - `TEMPLATE_VIDEO_WITH_SIDE_IMAGE`
  - `TEMPLATE_VIDEO_SIDE_IMAGE_BOTTOM_TEXT`
- `SlotType`
  - `SLOT_VIDEO`
  - `SLOT_IMAGE`
  - `SLOT_TEXT`

### 3. 内容绑定规则

- `slot_bindings` 负责把模板槽位和实际内容绑定起来
- `slot_id` 必须引用 `template_config.slots` 中已经声明的槽位
- `content_type = CONTENT_ASSET` 时，必须提供有效的 `asset_id`
- `content_type = CONTENT_TEXT` 时，文本内容放在 `text` 中，`asset_id` 应为空字符串或不使用
- 同一个场景内，同一个 `slot_id` 只应出现一次绑定

固定枚举：

- `ContentType`
  - `CONTENT_ASSET`
  - `CONTENT_TEXT`

字段约束：

- `slot_id`：必填
- `content_type`：必填
- `display_policy`：必填
- `asset_id`：当 `content_type = CONTENT_ASSET` 时必填
- `text`：当 `content_type = CONTENT_TEXT` 时必填
- `text`：当 `content_type = CONTENT_ASSET` 时应为空字符串或忽略

### 4. 资源规则

- `assets` 是 Manifest 依赖资源的全集，播放器可据此进行本地缓存校验
- `asset_id` 在同一个 Manifest 内必须唯一
- `required = true` 表示该资源缺失时会影响 Manifest 正常播放
- `cdn_path` 是静态资源基础路径，真正下载地址仍以 `assets/batch-url` 返回为准
- `sha256` 是资源完整性校验依据，播放器下载后必须校验

固定枚举：

- `AssetType`
  - `ASSET_VIDEO`
  - `ASSET_IMAGE`
  - `ASSET_FONT`

字段约束：

- `asset_id`：必填
- `asset_type`：必填
- `file_name`：必填
- `asset_ref`：必填
- `mime_type`：必填
- `size_bytes`：必填
- `sha256`：必填
- `required`：必填
- `priority`：必填
- `duration_ms`：视频资源建议填写，图片资源可为 `0`

### 5. 场景编排规则

- `scenes` 是播放器执行的场景序列
- `order` 用于定义场景播放顺序
- `duration_ms` 表示场景持续时间
- `play_mode` 决定场景整体播放方式，当前以循环播放为主

固定枚举：

- `PlayMode`
  - `PLAY_MODE_SEQUENCE`
  - `PLAY_MODE_LOOP`

字段约束：

- `plan_id`：必填
- `play_mode`：必填
- `scenes`：必填，至少包含一个场景
- `scene_id`：必填
- `scene_name`：必填
- `duration_ms`：必填
- `order`：必填，且在同一 `PlaybackPlan` 内应唯一

### 6. 前后端协作边界

- 服务端负责生成合法 DSL，并保证字段结构、引用关系和资源依赖完整
- 前端负责解析 `template_config` 和 `slot_bindings`，并按模板规则完成渲染
- 播放器负责消费 `playback_plan`、校验 `assets`、执行缓存策略并处理 fallback

### 7. 扩展原则

- 新增模板时，优先新增 `template_id` 和对应槽位定义，不直接破坏已有模板结构
- 新增内容类型时，优先扩展 `slot_type` 和 `content_type` 枚举，并保持旧字段兼容
- 新增运行时策略时，优先扩展 `cache_policy` 或 `fallback_policy`，不要混入模板结构

## 关键字段是否必填

### `TemplateConfig`

- `template_id`：必填
- `template_version`：必填
- `design_width`：必填
- `design_height`：必填
- `slots`：必填

### `PlaybackPlan`

- `plan_id`：必填
- `play_mode`：必填
- `scenes`：必填

### `PlaybackScene`

- `scene_id`：必填
- `scene_name`：必填
- `duration_ms`：必填
- `order`：必填
- `slot_bindings`：必填

### `AssetItem`

- `asset_id`：必填
- `asset_type`：必填
- `file_name`：必填
- `asset_ref`：必填
- `oss_path`：必填
- `cdn_path`：必填
- `mime_type`：必填
- `size_bytes`：必填
- `sha256`：必填
- `required`：必填
- `priority`：必填
- `duration_ms`：建议填写，默认允许为 `0`
