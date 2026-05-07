# Server API Summary

本文档整理 `Player Application Server` 当前确认的 HTTP 接口与数据边界。

补充文档：

- 接口 Schema：`docs/api-schema-design.md`
- 错误码：`docs/error-codes.md`
- Manifest 结构：`docs/manifest-contract.md`

## 1. Register

路径：`POST /api/v1/player/register`

职责：

- 设备首次激活
- 绑定租户与点位
- 返回 `device_id`、`access_token` 与基础 `PlayerConfig`

核心字段：

- `device_sn`
- `activation_code`
- `platform`
- `app_version`
- `screen_resolution`
- `capabilities`

## 2. Events

路径：`POST /api/v1/player/events`

职责：

- 接收启动事件
- 接收播放事件
- 接收同步事件
- 接收离线恢复补报事件
- 接收播放失败事件

关键事件类型：

- `EVENT_APP_STARTED`
- `EVENT_MANIFEST_SYNC_SUCCESS`
- `EVENT_MANIFEST_SYNC_FAILED`
- `EVENT_ASSET_DOWNLOAD_SUCCESS`
- `EVENT_ASSET_DOWNLOAD_FAILED`
- `EVENT_OFFLINE_ENTERED`
- `EVENT_OFFLINE_EXITED`
- `EVENT_PLAYBACK_ERROR`

## 3. Manifest Pull

路径：`POST /api/v1/player/manifest/pull`

职责：

- 按设备与版本拉取最新 `PlayerManifest`
- 返回 `MANIFEST_NO_UPDATE` 或新的 `PlayerManifest`

核心模型：

- `PlayerManifest`
- `TemplateConfig`
- `PlaybackPlan`
- `PlaybackScene`
- `AssetItem`
- `CachePolicy`
- `FallbackPolicy`

流程说明：

- 设备上报 `device_id`、本地 `current_manifest_version`、分辨率和能力信息
- 服务端查询设备对应的最新 Manifest 与关联资源元数据
- 若无更新，返回 `MANIFEST_NO_UPDATE`
- 若有更新，返回 `MANIFEST_FULL_UPDATE + PlayerManifest`

详细结构：`docs/manifest-contract.md`

## 4. Asset Batch URL

路径：`POST /api/v1/assets/batch-url`

职责：

- 给设备下发资源下载地址
- 配合 CDN / 签名 URL / 过期控制
- 提供 `sha256` 和 `size_bytes` 用于校验

## 5. Heartbeat

路径：`POST /api/v1/player/heartbeat`

职责：

- 上报播放状态
- 上报设备健康与网络状态
- 上报缓存状态
- 顺带返回待执行远程命令

流程说明：

- Player 按 `heartbeat_interval_sec` 周期收集 `playback / health / cache / network`
- 服务端保存最新健康状态
- 服务端查询当前设备待执行命令
- Heartbeat 响应返回 `commands`

## 6. Command ACK

路径：`POST /api/v1/player/commands/ack`

职责：

- 上报远程命令执行结果
- 记录成功或失败原因

## 7. 补充信息

以下内容用于补充当前仓库的后续实现方向。

基础设施建议：

- `MySQL`：保存设备、布局、排期、媒体元数据等结构化数据
- `Redis`：保存设备在线状态、最后心跳、关键事件、最近同步状态
- `OSS`：保存原始媒体文件
- `CDN`：向播放器分发图片、视频等静态资源
- `RMQ`：可选，用于后续异步日志、告警、通知扩展

服务端职责补充：

- 服务端负责返回播放器可直接消费的运行时配置，而不是后台内部编辑对象
- 服务端负责计算设备维度的配置命中结果，例如设备绑定、分组匹配、优先级与 fallback
- 离线播放逻辑主要在播放器端，服务端主要负责保存最近一次成功同步状态

运行时状态建议：

- 健康心跳优先写入 `Redis`
- 关键事件优先写入 `Redis`
- 最近一次同步版本可保存 `layout_version`、`schedule_version`、`manifest_version`、`last_sync_at`、`last_online_at`

## 8. 离线恢复

职责：

- 网络异常时继续依赖本地 `last-good Manifest` 播放
- 将离线期间事件写入本地队列
- 网络恢复后补发离线事件并重新同步 Manifest

服务端侧关注点：

- 接收离线补发事件，仍走 `POST /api/v1/player/events`
- 保留设备最近一次成功同步状态，支撑恢复后的版本判断
- 重新进入 Manifest 同步流程后，继续沿用 `POST /api/v1/player/manifest/pull`
