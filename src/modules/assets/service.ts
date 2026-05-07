import { store } from '../../lib/app-context.js';

export interface BatchGetAssetUrlRequest {
  device_id: string;
  manifest_id: string;
  manifest_version: number;
  asset_ids: string[];
}

export async function batchGetAssetUrls(input: BatchGetAssetUrlRequest) {
  const assets = await store.findAssetsByIds(input.device_id, input.asset_ids);

  return {
    assets: assets.map((asset, index) => ({
      asset_id: asset.assetId,
      download_url: asset.cdnPath || `https://cdn.example.com/assets/${asset.assetId}`,
      expire_at: asset.expireAt ?? Date.now() + 10 * 60 * 1000,
      sha256: asset.sha256,
      size_bytes: asset.sizeBytes || 1024 * 1024 * (index + 1)
    }))
  };
}
