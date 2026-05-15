# Player Application Server

Digital Signage 播放端后端服务。为播放设备提供内容同步、资源下载、心跳上报、事件采集等能力。

## 技术栈

- Java 17 + Spring Boot 3.2
- MySQL 8.0（结构化数据）
- Redis（心跳状态 / Manifest 缓存 / 在线检测）
- 阿里云 OSS（媒体资源存储 + 签名 URL）
- JWT 鉴权

## 快速开始

```bash
git clone https://github.com/NUS-Multi-tenant-Digital/Digital-Signage-player-server.git
cd Digital-Signage-player-server
cp .env.example .env   # 按需修改数据库和 OSS 配置
```

确保本地 MySQL 和 Redis 已启动，然后建库导数据：

```bash
mysql -u root -e "CREATE DATABASE IF NOT EXISTS player_application_server"
mysql -u root player_application_server < sql/schema.sql
mysql -u root player_application_server < sql/mvp_seed.sql
```

启动服务：

```bash
mvn spring-boot:run
```

默认运行在 `http://localhost:3000`。

## 接口

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| POST | `/api/v1/player/register` | 设备注册，返回 JWT token | 无 |
| POST | `/api/v1/player/manifest/pull` | 拉取播放清单 | Bearer |
| POST | `/api/v1/assets/batch-url` | 批量获取资源签名下载地址 | Bearer |
| POST | `/api/v1/player/heartbeat` | 心跳上报 + 拉取待执行命令 | Bearer |
| POST | `/api/v1/player/events` | 播放事件上报 | Bearer |
| POST | `/api/v1/player/commands/ack` | 命令执行确认 | Bearer |
| GET | `/health` | 健康检查 | 无 |

## 目录结构

```
src/main/java/com/digitalsignage/playerserver/
├── config/          # Redis、OSS、Security 配置
├── controller/      # REST 接口层
├── dto/             # 请求/响应数据结构
├── entity/          # JPA 实体
├── repository/      # 数据访问层
├── security/        # JWT 生成/验证/拦截
└── service/         # 业务逻辑层
```

## 测试

```bash
mvn test                     # 全部测试（单元 + 集成 + 压力）
mvn test -Dtest="*Test"      # 仅单元测试
mvn test -Dtest="*IntegrationTest"  # 仅集成测试
mvn test -Dtest="StressTest" # 压力测试
```

## 核心流程

1. 设备首次启动 → 调用 `register` 获取 `device_id` 和 `access_token`
2. 轮询 `manifest/pull` 检查是否有新版播放计划
3. 有更新时调用 `assets/batch-url` 获取签名下载链接，缓存到本地
4. 定时发送 `heartbeat`，服务端返回待执行命令（如刷新、重启）
5. 播放过程中通过 `events` 上报状态
6. 执行命令后通过 `commands/ack` 回传结果

## 文档

- [接口总览](docs/server-api-summary.md)
- [接口 Schema](docs/api-schema-design.md)
- [Manifest 协议与 DSL](docs/manifest-contract.md)
- [存储设计](docs/persistence-design.md)
- [表结构说明](docs/schema-overview.md)
- [错误码](docs/error-codes.md)
- [前端对齐说明](docs/frontend-alignment.md)
- [CI/CD 部署](docs/cicd-aliyun-cd.md)
- [MVP 开发计划](docs/mvp-week-plan.md)
