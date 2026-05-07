import type { FastifyInstance } from 'fastify';
import { reportPlayerEventsSchema } from '../../lib/api-schemas.js';
import { ok } from '../../lib/http.js';
import { reportPlayerEvents, type ReportPlayerEventsRequest } from './service.js';

export async function registerPlayerEventRoutes(app: FastifyInstance): Promise<void> {
  app.post('/api/v1/player/events', { schema: reportPlayerEventsSchema }, async (request) => {
    const body = request.body as ReportPlayerEventsRequest;
    return ok(await reportPlayerEvents(body), 'Events accepted');
  });
}
