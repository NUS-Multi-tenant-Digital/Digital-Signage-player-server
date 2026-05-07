# Player Application Server

`Player Application Server` 用于承接播放器设备的注册、内容同步、资源地址下发、心跳上报、事件回传和远程命令回执。

当前仓库已完成第一版服务端骨架、模块拆分、DSL 契约整理和 `MySQL + Redis` 存储骨架，便于继续补充真实业务逻辑。

## 接口清单

- `POST /api/v1/player/register`
- `POST /api/v1/player/events`
- `POST /api/v1/player/manifest/pull`
- `POST /api/v1/assets/batch-url`
- `POST /api/v1/player/heartbeat`
- `POST /api/v1/player/commands/ack`

## 关键流程

- 首次启动：检查本地设备状态，没有 `device_id` 时调用 `register`
- 启动播放：优先加载本地 `LocalPlayerState` 和本地 `PlayerManifest`
- 内容同步：调用 `manifest/pull` 判断是否有新版本
- 资源更新：通过 `assets/batch-url` 获取下载地址并更新本地缓存
- 状态上报：通过 `events` 上报启动、同步、下载、离线和播放异常事件
- 健康监控：通过 `heartbeat` 上报运行状态并拉取待执行命令
- 命令回执：设备执行命令后通过 `commands/ack` 回传结果
- 离线恢复：网络恢复后补发离线事件，再重新拉取最新 Manifest

## 目录结构

```text
src/
  config/
  lib/
  modules/
    register/
    events/
    manifest/
    assets/
    heartbeat/
    commands/
  plugins/
  routes/
```

## 快速开始

```bash
npm install
cp .env.example .env
npm run dev
```

默认地址：`http://localhost:3000`

健康检查：`GET /health`

## 当前实现说明

- 每个接口都已提供可运行的路由与示例响应。
- `register`、`manifest`、`heartbeat`、`events`、`commands` 已切到异步 repository 调用。
- 代码内已拆出领域模型、repository 接口和运行态状态结构。
- 已补充统一错误码文档和接口 request / response schema 设计文档。
- 已提供 `MySQL + Redis` repository 骨架，并支持通过环境变量切换到 `memory` 模式。
- 真实鉴权、对象存储签名 URL、完整业务查询逻辑和联调数据仍需继续补充。

## 核心文档

- 接口总览：`docs/server-api-summary.md`
- 接口 Schema：`docs/api-schema-design.md`
- 错误码：`docs/error-codes.md`
- Manifest 协议：`docs/manifest-contract.md`
- 前端对齐说明：`docs/frontend-alignment.md`
- 存储设计：`docs/persistence-design.md`
- 表结构说明：`docs/schema-overview.md`
- 建表 SQL：`sql/schema.sql`

## 补充架构约束

- 配置访问和媒体分发分离：业务服务返回配置和资源元数据，媒体文件仍由 `OSS/CDN` 直接分发。
- `MySQL` 更适合作为设备、配置、资源元数据等结构化数据的主存储。
- `Redis` 更适合作为设备在线状态、最近心跳、关键事件、最近同步状态等运行时数据存储。
- `RMQ` 可作为后续扩展项，用于异步日志消费、状态变更通知或告警处理，但当前不是主链路必需组件。
- 当前仓库已提供 `MySQL + Redis` repository 骨架，默认通过环境变量初始化真实存储连接，也可切回内存模式。

## 环境变量

- 参考模板：`.env.example`
- 关键变量：
  - `STORAGE_DRIVER=mysql_redis`
  - `MYSQL_URL=mysql://root:password@127.0.0.1:3306/player_application_server`
  - `REDIS_URL=redis://127.0.0.1:6379`

## 上传建议

```bash
git init -b main
git add .
git commit -m "chore: initialize player application server"
```

当前 `.gitignore` 已排除本地环境变量、依赖目录和文档提取中间文件，适合作为首版仓库直接提交。

## 下一步计划

1. 在现有 Fastify schema 基础上补 OpenAPI 文档导出。
2. 将 `SqlRedisStore` 从骨架实现补全为真实查询逻辑。
3. 接入真实鉴权，例如 `access_token` 的签发与校验。
4. 对接对象存储和 CDN 签名 URL 生成。
5. 为 `register`、`manifest pull`、`heartbeat` 增加集成测试。
