import Fastify, { type FastifyInstance } from 'fastify';
import { loadConfig } from './config/env.js';
import { closeStore, initializeStore } from './lib/app-context.js';
import { registerHealthRoutes } from './plugins/health.js';
import { registerRoutes } from './routes/index.js';

export async function buildApp(): Promise<FastifyInstance> {
  const config = loadConfig();
  const app = Fastify({ logger: config.nodeEnv !== 'test' });

  await initializeStore(config);
  await registerHealthRoutes(app);
  await registerRoutes(app);

  app.addHook('onClose', async () => {
    await closeStore();
  });

  return app;
}
