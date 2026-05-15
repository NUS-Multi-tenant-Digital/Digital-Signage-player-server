# 阿里云云效 CD 部署配置

本文档说明如何在阿里云云效（Flow）上配置 CD 流水线，实现分环境部署和灰度发布。

## 触发方式

GitHub Actions CI 通过后自动推送 Docker 镜像到阿里云 ACR（容器镜像服务），云效流水线监听镜像更新自动触发 CD。

## 前置准备

1. **阿里云 ACR**：创建命名空间 `digital-signage`，镜像仓库 `player-server`
2. **阿里云 ECS / K8s**：准备 staging 和 production 环境
3. **云效 Flow**：创建流水线并关联 ACR 镜像源

## 环境划分

| 环境 | 用途 | 触发方式 |
|------|------|----------|
| staging | 联调测试 | main 分支 push 自动部署 |
| production | 正式环境 | staging 验证通过后手动审批 |

## 云效流水线配置步骤

### 1. 创建流水线

- 登录 [云效 Flow](https://flow.aliyun.com)
- 新建流水线 → 选择「空白模板」

### 2. 添加触发源

- 触发方式：「镜像更新触发」
- 镜像地址：`registry.cn-southeast-1.aliyuncs.com/digital-signage/player-server`

### 3. Staging 部署阶段

添加「主机部署」或「K8s 部署」任务：

```bash
# 如果是 ECS 主机部署
docker pull registry.cn-southeast-1.aliyuncs.com/digital-signage/player-server:latest
docker stop player-server || true
docker rm player-server || true
docker run -d \
  --name player-server \
  --restart unless-stopped \
  -p 3000:3000 \
  --env-file /etc/player-server/.env \
  registry.cn-southeast-1.aliyuncs.com/digital-signage/player-server:latest
```

### 4. 接口检测阶段

Staging 部署后自动运行：

```bash
# 健康检查
curl -sf http://<staging-ip>:3000/health

# 核心接口验证
curl -sf -X POST http://<staging-ip>:3000/api/v1/player/register \
  -H 'Content-Type: application/json' \
  -d '{"hardware_id":"cd-smoke","platform":"web","app_version":"1.0.0"}'
```

### 5. 灰度发布阶段（生产环境）

如果使用 K8s / SLB：

- **小流量**：先将新版本部署到 1 台实例，SLB 权重设为 10%
- **观察期**：监控 5-10 分钟，检查错误率和响应时间
- **全流量**：无异常后逐步调整权重到 100%

如果使用 ECS 单机：

- **手动审批**：staging 验证通过后，需人工点击「继续」
- **部署生产**：同 staging 部署脚本，替换为生产环境地址

### 6. 人工卡点

在 staging → production 之间添加「人工审批」节点，指定审批人。

## GitHub Secrets 配置

在 GitHub 仓库 Settings → Secrets 中添加：

| Secret | 说明 |
|--------|------|
| `ACR_USERNAME` | 阿里云 ACR 用户名 |
| `ACR_PASSWORD` | 阿里云 ACR 密码 |

## 环境变量管理

各环境的 `.env` 文件存储在服务器 `/etc/player-server/.env`，包含：

```env
PORT=3000
NODE_ENV=production
STORAGE_DRIVER=mysql_redis
MYSQL_URL=mysql://user:pass@<rds-endpoint>:3306/player_application_server
REDIS_URL=redis://<redis-endpoint>:6379
JWT_SECRET=<production-secret>
ALIYUN_OSS_ENDPOINT=https://oss-ap-southeast-1.aliyuncs.com
ALIYUN_OSS_BUCKET=digital-signage-media
ALIYUN_OSS_REGION=ap-southeast-1
ALIYUN_OSS_ACCESS_KEY_ID=<key>
ALIYUN_OSS_ACCESS_KEY_SECRET=<secret>
STORAGE_PUBLIC_BASE_URL=https://media.digital-signage.ltd
```

## 完整流程

```
GitHub Push → GitHub Actions CI → ACR 镜像 → 云效触发 → Staging 部署 → 接口检测 → 人工审批 → 灰度发布 → 全量上线
```
