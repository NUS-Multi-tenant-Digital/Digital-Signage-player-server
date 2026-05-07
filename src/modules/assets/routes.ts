import type { FastifyInstance } from 'fastify';
import { batchGetAssetUrlsSchema } from '../../lib/api-schemas.js';
import { ok } from '../../lib/http.js';
import { batchGetAssetUrls, type BatchGetAssetUrlRequest } from './service.js';

export async function registerAssetRoutes(app: FastifyInstance): Promise<void> {
  app.post('/api/v1/assets/batch-url', { schema: batchGetAssetUrlsSchema }, async (request) => {
    const body = request.body as BatchGetAssetUrlRequest;
    return ok(await batchGetAssetUrls(body), 'Asset URLs generated');
  });
}
