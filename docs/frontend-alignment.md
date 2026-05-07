# Frontend Alignment Notes

本文档用于和前端同学对齐当前 `Player Application Server` 已完成的工作，以及需要共同确认的点。

## 服务端已完成内容

- 已确认播放器核心接口路径：
  - `POST /api/v1/player/register`
  - `POST /api/v1/player/events`
  - `POST /api/v1/player/manifest/pull`
  - `POST /api/v1/assets/batch-url`
  - `POST /api/v1/player/heartbeat`
  - `POST /api/v1/player/commands/ack`
- 已整理并固定 `manifest` 返回结构
- 已补齐 `template_config`、`playback_plan`、`assets`、`cache_policy`、`fallback_policy`
- 已补齐 DSL 规则、固定枚举、字段必填约束
- 已完成建表 SQL、ER 图和仓库基础骨架
- 已将接口 schema 接入路由层

## 需要前端对齐的重点

- 是否接受当前 `template_id` 枚举和模板职责边界
- 是否接受当前 `slot_type`、`content_type`、`play_mode` 固定枚举
- 不同 `slot_type` 在前端渲染层的处理方式是否一致
- `CONTENT_TEXT`、`CONTENT_ASSET` 在前端侧的字段消费规则是否一致
- 模板升级时，前后端如何保持兼容

## 当前 DSL 固定枚举

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
- `ContentType`
  - `CONTENT_ASSET`
  - `CONTENT_TEXT`
- `PlayMode`
  - `PLAY_MODE_SEQUENCE`
  - `PLAY_MODE_LOOP`

## 建议对齐顺序

1. 先确认 `template_config` 和模板枚举
2. 再确认 `slot_bindings` 的字段约束
3. 再确认前端需要直接消费哪些 `assets` 字段
4. 最后确认新增模板和内容类型时的兼容方式
