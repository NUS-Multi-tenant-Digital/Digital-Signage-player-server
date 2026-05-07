import type { FastifyInstance } from 'fastify';
import { ackCommandSchema } from '../../lib/api-schemas.js';
import { ok } from '../../lib/http.js';
import { ackCommand, type CommandAckRequest } from './service.js';

export async function registerCommandRoutes(app: FastifyInstance): Promise<void> {
  app.post('/api/v1/player/commands/ack', { schema: ackCommandSchema }, async (request) => {
    const body = request.body as CommandAckRequest;
    return ok(await ackCommand(body), 'Command ACK accepted');
  });
}
