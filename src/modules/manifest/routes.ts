import type { FastifyInstance } from 'fastify';
import { pullManifestSchema } from '../../lib/api-schemas.js';
import { ok } from '../../lib/http.js';
import { pullManifest, type PullManifestRequest } from './service.js';

export async function registerPlayerManifestRoutes(app: FastifyInstance): Promise<void> {
  app.post('/api/v1/player/manifest/pull', { schema: pullManifestSchema }, async (request) => {
    const body = request.body as PullManifestRequest;
    return ok(await pullManifest(body), 'Manifest processed');
  });
}
