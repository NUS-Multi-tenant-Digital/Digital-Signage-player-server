# Error Codes

本文档定义 `Player Application Server` 当前阶段的统一错误码约定。

## 返回约定

成功响应：

```json
{
  "success": true,
  "data": {},
  "message": "ok"
}
```

失败响应建议：

```json
{
  "success": false,
  "error": {
    "code": "PLAYER_DEVICE_NOT_FOUND",
    "message": "Device does not exist",
    "details": {}
  }
}
```

## 通用错误

- `PLAYER_BAD_REQUEST`：请求参数不合法
- `PLAYER_UNAUTHORIZED`：访问令牌无效或缺失
- `PLAYER_FORBIDDEN`：当前设备无权限访问目标资源
- `PLAYER_NOT_FOUND`：目标资源不存在
- `PLAYER_INTERNAL_ERROR`：服务端内部异常
- `PLAYER_RATE_LIMITED`：请求过于频繁

## 注册相关

- `PLAYER_ACTIVATION_CODE_INVALID`：激活码不存在或无效
- `PLAYER_ACTIVATION_CODE_EXPIRED`：激活码已过期
- `PLAYER_ACTIVATION_CODE_USED`：激活码已被使用
- `PLAYER_DEVICE_SN_CONFLICT`：设备序列号已绑定其他设备
- `PLAYER_DEVICE_REGISTER_FAILED`：设备注册失败

## Manifest 相关

- `PLAYER_MANIFEST_NOT_FOUND`：当前设备没有可用 Manifest
- `PLAYER_MANIFEST_VERSION_INVALID`：Manifest 版本参数不合法
- `PLAYER_MANIFEST_CHECKSUM_INVALID`：Manifest 校验失败
- `PLAYER_TEMPLATE_UNSUPPORTED`：模板类型不受支持
- `PLAYER_PLAYBACK_PLAN_INVALID`：播放计划结构非法

## 资源相关

- `PLAYER_ASSET_NOT_FOUND`：资源不存在
- `PLAYER_ASSET_BATCH_EMPTY`：资源列表为空
- `PLAYER_ASSET_URL_GENERATE_FAILED`：下载地址生成失败
- `PLAYER_ASSET_METADATA_INVALID`：资源元数据不完整

## 心跳与事件相关

- `PLAYER_HEARTBEAT_PAYLOAD_INVALID`：心跳载荷格式不合法
- `PLAYER_EVENT_PAYLOAD_INVALID`：事件载荷格式不合法
- `PLAYER_EVENT_BATCH_EMPTY`：事件列表为空
- `PLAYER_DEVICE_OFFLINE`：设备当前处于离线状态

## 命令相关

- `PLAYER_COMMAND_NOT_FOUND`：命令不存在
- `PLAYER_COMMAND_EXPIRED`：命令已过期
- `PLAYER_COMMAND_ACK_INVALID`：命令回执格式不合法
- `PLAYER_COMMAND_STATE_INVALID`：命令状态流转非法

## 建议

- 文档阶段先统一错误码前缀为 `PLAYER_`
- 服务端后续可补 `http status -> error code` 映射
- 关键业务错误必须保持稳定，不要在联调阶段频繁改名
