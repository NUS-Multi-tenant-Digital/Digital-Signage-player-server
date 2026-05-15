# MVP Week Plan

本文档用于把 `Player Application Server` 下一周的 MVP 开发目标拆成可执行步骤，目标是让接手的人不需要补太多上下文，也能继续推进。

## 1. 本周目标

本周不是把播放器服务端做成最终版，而是把一条最小可运行链路跑通：

`register -> manifest/pull -> assets/batch-url -> 本地缓存 -> 播放 -> heartbeat -> events -> 断网继续播 -> 恢复后补同步`

如果这条链路能跑通，就说明：

- 服务端已经能提供基础设备接入能力
- 前端 / Web-based player 已经能从服务端拿到真实数据并渲染
- 缓存、心跳、事件、离线恢复已经形成最小闭环

## 2. MVP 范围

本周只围绕以下 4 块能力推进：

- `Fetch Content`
- `Media Cache`
- `Device Heart beat`
- `Offline Mode`

不在本周重点范围内的内容：

- 完整鉴权体系
- 完整对象存储签名 URL 平台化
- 完整命令中心
- 所有事件类型的极致细分
- 长期终态 DSL 扩展
- 后台 CMS 能力

## 3. 完成后的效果

本周结束时，至少应该能完成一个 Web-based player 联调 demo，表现为：

- Web player 可以注册设备
- Web player 可以拉取最新 `manifest`
- Web player 可以获取资源下载地址
- Web player 可以下载并缓存资源
- Web player 可以按照 `template_config + playback_plan` 渲染至少 1 套模板
- Web player 在播放过程中持续上报 `heartbeat`
- Web player 可以上报关键 `events`
- 网络断开后，Web player 可以继续依赖本地 last-good manifest 和本地资源播放
- 网络恢复后，可以补发事件并重新同步最新 `manifest`

## 4. 当前仓库基础

当前仓库已经具备以下基础：

- 仓库骨架和运行方式：`README.md`
- 接口总览：`docs/server-api-summary.md`
- 接口字段设计：`docs/api-schema-design.md`
- Manifest / DSL 协议：`docs/manifest-contract.md`
- 前端对齐说明：`docs/frontend-alignment.md`
- 错误码：`docs/error-codes.md`
- 表结构设计：`docs/schema-overview.md`
- 持久化设计：`docs/persistence-design.md`
- 建表 SQL：`sql/schema.sql`
- Day 1 基线：`docs/mvp-day1-baseline.md`
- Day 1 种子数据：`sql/mvp_seed.sql`

当前代码结构：

- 应用入口：`src/index.ts`、`src/app.ts`
- 路由注册：`src/routes/index.ts`
- 接口 schema：`src/lib/api-schemas.ts`
- 存储切换：`src/lib/app-context.ts`
- 内存 / 真实存储：`src/repositories/store.ts`、`src/repositories/sql-redis-store.ts`
- 业务模块：
  - `src/modules/register/`
  - `src/modules/manifest/`
  - `src/modules/assets/`
  - `src/modules/heartbeat/`
  - `src/modules/events/`
  - `src/modules/commands/`

## 5. 四块能力分别要做到什么程度

### 5.1 Fetch Content

对应接口：

- `POST /api/v1/player/manifest/pull`

目标：

- 根据 `device_id` 查到该设备当前应生效的 `manifest`
- 能正确返回 `MANIFEST_NO_UPDATE` 或 `MANIFEST_FULL_UPDATE`
- 返回结构与 `docs/manifest-contract.md` 保持一致

本周完成标准：

- `manifest/pull` 不再只是演示数据兜底
- 能从真实存储读取设备和 manifest 数据
- 同一设备连续请求时，能按本地版本判断是否需要更新
- 返回的 `template_config`、`playback_plan`、`assets`、`cache_policy`、`fallback_policy` 结构稳定

涉及文件：

- `src/modules/manifest/service.ts`
- `src/repositories/sql-redis-store.ts`
- `src/domain/manifest.ts`
- `docs/manifest-contract.md`

### 5.2 Media Cache

对应接口：

- `POST /api/v1/assets/batch-url`

目标：

- 根据 `asset_ids` 返回资源下载地址和校验信息
- 让播放器能够按返回结果完成下载、校验和缓存

本周完成标准：

- `assets/batch-url` 走真实查询逻辑
- `manifest` 中出现的资源都能通过 `assets/batch-url` 查到
- 返回以下字段：
  - `asset_id`
  - `download_url`
  - `expire_at`
  - `sha256`
  - `size_bytes`
- URL 可以先是固定 CDN 地址或 mock 签名 URL，不要求第一周完成完整签名体系

涉及文件：

- `src/modules/assets/service.ts`
- `src/repositories/sql-redis-store.ts`
- `src/domain/manifest.ts`
- `docs/api-schema-design.md`

### 5.3 Device Heart beat

对应接口：

- `POST /api/v1/player/heartbeat`

目标：

- 服务端能够知道设备是否在线
- 服务端能够知道设备当前播放状态、版本、缓存状态、网络状态
- 为后续命令下发预留响应出口

本周完成标准：

- 心跳数据写入 Redis
- 至少保存：
  - `device_id`
  - `app_version`
  - `manifest_id`
  - `manifest_version`
  - `timestamp`
  - `playback`
  - `health`
  - `cache`
  - `network`
- 心跳上报后可以更新设备最近在线时间或同步状态
- 响应中的 `commands` 可以先返回空数组或最小 mock 命令，但结构必须稳定

本周建议最小字段：

- `playback`
  - 当前状态
  - 当前场景
  - 当前资源
  - 当前播放进度
- `health`
  - CPU
  - memory
  - disk
- `cache`
  - 已缓存资源数
  - 已使用缓存大小
  - 最近是否命中
- `network`
  - 是否在线
  - 网络类型

涉及文件：

- `src/modules/heartbeat/service.ts`
- `src/repositories/sql-redis-store.ts`
- `src/domain/state.ts`
- `docs/api-schema-design.md`

### 5.4 Offline Mode

相关接口和结构：

- `POST /api/v1/player/events`
- `POST /api/v1/player/manifest/pull`
- `manifest.fallback_policy`

目标：

- 设备断网后仍能依赖本地 last-good manifest 和已缓存资源播放
- 恢复网络后，补发离线期间关键事件，并重新同步内容

本周完成标准：

- 服务端保存最近一次成功同步状态
- 服务端可以接收离线补发事件
- 恢复网络后重新请求 `manifest/pull` 时能正常工作
- `fallback_policy` 已经能随 manifest 下发

注意：

- 本周不做复杂离线策略编排
- 只做 MVP 级闭环：断网可播、恢复可续

涉及文件：

- `src/modules/events/service.ts`
- `src/modules/manifest/service.ts`
- `src/repositories/sql-redis-store.ts`
- `docs/server-api-summary.md`
- `docs/manifest-contract.md`

## 6. 一周执行计划

### Day 1：固定 MVP 样例数据和边界

目标：

- 固定一套最小可跑的设备、manifest、assets 数据
- 明确本周只支持 1 套模板先跑通

执行步骤：

1. 用 `sql/schema.sql` 初始化本地 MySQL
2. 准备一套最小数据：
   - 1 条 `devices`
   - 1 条 `player_configs`
   - 1 条 `manifests`
   - 2 到 3 条 `assets`
   - 若干条 `manifest_assets`
3. 模板先固定为 1 套，例如：
   - `TEMPLATE_VIDEO_WITH_BOTTOM_TEXT`
4. 明确联调资源：
   - 1 个视频
   - 1 个图片或 1 段文字
5. 确认 Web player 本周只围绕这一套模板联调

交付结果：

- 本地数据库可启动
- 有一套固定测试数据
- 后续接口都围绕这套数据调试
- Day 1 交付文件：
  - `sql/mvp_seed.sql`
  - `docs/mvp-day1-baseline.md`

### Day 2：跑通 Fetch Content

目标：

- 让 `manifest/pull` 返回真实可用 manifest

执行步骤：

1. 检查 `src/repositories/sql-redis-store.ts` 中以下能力是否补全：
   - `findById`
   - `findConfigByDeviceId`
   - `findLatestByDevice`
   - `findAssetsByIds`
2. 修改 `src/modules/manifest/service.ts`
   - 优先从真实 store 读 manifest
   - 没有命中时，明确返回策略，不继续长期依赖 demo 构造逻辑
3. 严格对齐 `docs/manifest-contract.md`
4. 验证以下两种情况：
   - 设备当前版本落后：返回 `MANIFEST_FULL_UPDATE`
   - 设备当前版本最新：返回 `MANIFEST_NO_UPDATE`

交付结果：

- `manifest/pull` 可以真实查出并返回 manifest

### Day 3：跑通 Media Cache

目标：

- 让 `assets/batch-url` 能根据资源列表返回可下载地址

执行步骤：

1. 确认 `manifest.assets` 和 `assets` 表数据一致
2. 完善 `src/modules/assets/service.ts`
   - 按 `device_id + asset_ids` 查询资源
   - 返回下载地址和校验字段
3. 下载地址策略先简单处理：
   - 优先返回 `cdn_path`
   - 如需带时效，可先拼 mock 参数
4. 验证：
   - `manifest` 中的资源都能通过 `assets/batch-url` 拉到下载地址
   - 返回的 `sha256`、`size_bytes` 与数据库一致

交付结果：

- Web player 可以开始拉资源并进入缓存逻辑

### Day 4：跑通 Device Heart beat

目标：

- 让服务端能稳定感知设备在线和播放状态

执行步骤：

1. 在 `src/modules/heartbeat/service.ts` 中收口最小 payload 结构
2. 在 `src/repositories/sql-redis-store.ts` 中补全：
   - `saveHeartbeat`
   - `findHeartbeat`
   - `saveSyncState`
   - `findSyncState`
   - `listPendingCommands`
3. heartbeat 至少写入 Redis：
   - 最近心跳时间
   - 当前 manifest 版本
   - 当前播放状态
   - 当前缓存状态
   - 当前网络状态
4. 响应中的 `commands` 保持稳定格式

交付结果：

- 服务端能够看到设备在线状态和基本运行信息

### Day 5：跑通 Offline Mode

目标：

- 形成离线播放和恢复同步的最小闭环

执行步骤：

1. 确认 `manifest` 返回中包含 `fallback_policy`
2. 完善 `src/modules/events/service.ts`
   - 接收离线期间补发事件
   - 继续更新最近同步状态
3. 确认恢复流程：
   - 设备断网期间继续播本地内容
   - 恢复后先补发事件
   - 再重新拉 `manifest`
4. 如果需要，补一份最小离线联调说明到本文档或单独文档

交付结果：

- 能演示“断网不黑屏，恢复后继续同步”

### Day 6：串全链路

目标：

- 从注册到播放到恢复，完整走一遍

执行步骤：

1. 顺序跑以下接口：
   - `POST /api/v1/player/register`
   - `POST /api/v1/player/manifest/pull`
   - `POST /api/v1/assets/batch-url`
   - `POST /api/v1/player/heartbeat`
   - `POST /api/v1/player/events`
2. 验证 Web player 是否可以：
   - 拉配置
   - 拉资源
   - 显示内容
   - 持续上报 heartbeat
3. 验证离线恢复：
   - 断网后继续播
   - 恢复后补发事件
   - 恢复后重新拉最新 manifest
4. 记录链路中的断点并修复

交付结果：

- 有一条完整可跑的 MVP 主链路

### Day 7：收尾和联调准备

目标：

- 把本周成果整理成可演示状态

执行步骤：

1. 固定本周 demo 数据
2. 整理一组联调用请求样例
3. 修掉阻塞联调的 bug
4. 确认 README 和相关文档里最关键入口可用
5. 准备一段口头说明：
   - 已实现什么
   - 还没实现什么
   - 当前 MVP 能演示什么

交付结果：

- 可以稳定给别人演示
- 可以交给前端继续联调

## 7. 代码级任务清单

本周优先修改这些文件：

- `src/repositories/sql-redis-store.ts`
  - 把 MVP 主链路所需查询和写入补完整
- `src/modules/manifest/service.ts`
  - 改成真实 manifest 查询和返回
- `src/modules/assets/service.ts`
  - 改成真实资源查询
- `src/modules/heartbeat/service.ts`
  - 改成真实 heartbeat 写入和最小命令返回
- `src/modules/events/service.ts`
  - 支持离线补发事件和同步状态更新
- `src/modules/register/service.ts`
  - 确保注册结果能写入真实 store

必要时同步更新：

- `src/lib/api-schemas.ts`
- `src/domain/manifest.ts`
- `src/domain/state.ts`
- `docs/api-schema-design.md`
- `docs/manifest-contract.md`

## 8. 最小验证清单

以下项目全部通过，才算本周目标完成：

- 可以成功注册设备
- 可以成功拉到 manifest
- 可以正确区分 `NO_UPDATE` 和 `FULL_UPDATE`
- 可以成功拉到资源地址
- Web player 能开始播放至少 1 套模板
- 服务端能收到 heartbeat
- 服务端能收到 events
- 断网后可以继续播放本地内容
- 恢复网络后可以补发事件并重新同步

## 9. 风险和处理原则

### 9.1 如果真实签名 URL 来不及

处理：

- 先返回可访问的固定 `cdn_path` 或 mock 下载地址

### 9.2 如果离线模式细节过多

处理：

- 只保证 last-good manifest + 本地缓存资源可继续播
- 不扩展复杂优先级策略

### 9.3 如果命令系统来不及

处理：

- heartbeat 响应中先稳定返回空数组
- 保留结构，不阻塞主链路

### 9.4 如果 schema 太细导致本周推进变慢

处理：

- 先保证主链路跑通
- 字段收口可以放在下一轮补

## 10. 一句话总结

本周的核心不是继续补设计，而是把 `Player Application Server` 做成一个可联调、可演示、可继续迭代的 MVP 服务端，让 Web-based player 可以真实跑通主链路。
