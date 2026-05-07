export interface AppConfig {
  port: number;
  host: string;
  nodeEnv: string;
  storageDriver: 'mysql_redis' | 'memory';
  mysqlUrl: string;
  redisUrl: string;
}

export function loadConfig(): AppConfig {
  return {
    port: Number(process.env.PORT ?? 3000),
    host: process.env.HOST ?? '0.0.0.0',
    nodeEnv: process.env.NODE_ENV ?? 'development',
    storageDriver: process.env.STORAGE_DRIVER === 'memory' ? 'memory' : 'mysql_redis',
    mysqlUrl: process.env.MYSQL_URL ?? 'mysql://root:password@127.0.0.1:3306/player_application_server',
    redisUrl: process.env.REDIS_URL ?? 'redis://127.0.0.1:6379'
  };
}
