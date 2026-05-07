import type { FastifyInstance } from 'fastify';
import { heartbeatSchema } from '../../lib/api-schemas.js';
import { ok } from '../../lib/http.js';
import { reportHeartbeat, type HeartbeatRequest } from './service.js';

export async function registerHeartbeatRoutes(app: FastifyInstance): Promise<void> {
  app.post('/api/v1/player/heartbeat', { schema: heartbeatSchema }, async (request) => {
    const body = request.body as HeartbeatRequest;
    return ok(await reportHeartbeat(body), 'Heartbeat accepted');
  });
}
