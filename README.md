# Player Application Server

播放器服务端，负责设备注册、内容同步、资源地址下发、心跳上报、事件回传和命令回执。

当前仓库已经完成第一版服务端骨架、模块拆分、Manifest / DSL 契约整理，以及 `MySQL + Redis` 存储骨架，后面可以在这上面继续补真实业务逻辑。

## 接口

- `POST /api/v1/player/register`
- `POST /api/v1/player/events`
- `POST /api/v1/player/manifest/pull`
- `POST /api/v1/assets/batch-url`
- `POST /api/v1/player/heartbeat`
- `POST /api/v1/player/commands/ack`
- `GET /health`

## 关键流程

- 首次启动：没有 `device_id` 时先走 `register`
- 内容同步：通过 `manifest/pull` 判断是否有新版本
- 资源更新：通过 `assets/batch-url` 获取下载地址
- 状态上报：通过 `events` 上报启动、同步、下载和异常事件
- 心跳保活：通过 `heartbeat` 上报状态并拉取待执行命令
- 命令回执：设备执行后通过 `commands/ack` 回传结果
- 离线恢复：网络恢复后补发事件，再重新拉取 Manifest

## 目录结构

```text
src/
  config/
  domain/
  lib/
  modules/
    register/
    events/
    manifest/
    assets/
    heartbeat/
    commands/
  plugins/
  repositories/
  routes/
```

## 安装

```bash
git clone https://github.com/XINGYUAN168/Player_application_server.git
cd Player_application_server
npm install
cp .env.example .env
```

按需修改 `.env` 中的 `MYSQL_URL`、`REDIS_URL` 和 `STORAGE_DRIVER`。

## 运行

```bash
npm run dev
```

默认启动地址：`http://localhost:3000`

常用命令：

```bash
npm run check
npm run build
npm run start
npm run test
```

## 文档

- 接口总览：`docs/server-api-summary.md`
- 接口 Schema：`docs/api-schema-design.md`
- 错误码：`docs/error-codes.md`
- Manifest 协议与 DSL：`docs/manifest-contract.md`
- 前端对齐说明：`docs/frontend-alignment.md`
- 存储设计：`docs/persistence-design.md`
- 表结构说明：`docs/schema-overview.md`
- 建表 SQL：`sql/schema.sql`

## 说明

- 当前默认支持 `memory` 和 `mysql_redis` 两种存储模式
- 媒体文件本身通过 `OSS/CDN` 分发，服务端主要负责配置和元数据
- 现在已经有完整骨架，但鉴权、签名 URL 和部分真实查询逻辑还会继续补
