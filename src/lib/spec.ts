export const endpointSpec = {
  register: '/api/v1/player/register',
  events: '/api/v1/player/events',
  manifestPull: '/api/v1/player/manifest/pull',
  assetBatchUrl: '/api/v1/assets/batch-url',
  heartbeat: '/api/v1/player/heartbeat',
  commandAck: '/api/v1/player/commands/ack'
} as const;
