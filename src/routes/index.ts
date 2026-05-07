import type { FastifyInstance } from 'fastify';
import { registerPlayerRegisterRoutes } from '../modules/register/routes.js';
import { registerPlayerEventRoutes } from '../modules/events/routes.js';
import { registerPlayerManifestRoutes } from '../modules/manifest/routes.js';
import { registerAssetRoutes } from '../modules/assets/routes.js';
import { registerHeartbeatRoutes } from '../modules/heartbeat/routes.js';
import { registerCommandRoutes } from '../modules/commands/routes.js';

export async function registerRoutes(app: FastifyInstance): Promise<void> {
  await registerPlayerRegisterRoutes(app);
  await registerPlayerEventRoutes(app);
  await registerPlayerManifestRoutes(app);
  await registerAssetRoutes(app);
  await registerHeartbeatRoutes(app);
  await registerCommandRoutes(app);
}
