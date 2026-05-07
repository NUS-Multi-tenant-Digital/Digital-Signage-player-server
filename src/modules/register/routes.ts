import type { FastifyInstance } from 'fastify';
import { registerPlayerSchema } from '../../lib/api-schemas.js';
import { ok } from '../../lib/http.js';
import { registerPlayer, type RegisterPlayerRequest } from './service.js';

export async function registerPlayerRegisterRoutes(app: FastifyInstance): Promise<void> {
  app.post('/api/v1/player/register', { schema: registerPlayerSchema }, async (request) => {
    const body = request.body as RegisterPlayerRequest;
    return ok(await registerPlayer(body), 'Player registered');
  });
}
