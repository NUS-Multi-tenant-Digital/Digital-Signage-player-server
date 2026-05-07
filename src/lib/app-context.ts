import type { AppConfig } from '../config/env.js';
import { InMemoryStore, type AssetRepository, type DeviceRepository, type ManifestRepository, type RuntimeStateRepository } from '../repositories/store.js';
import { SqlRedisStore } from '../repositories/sql-redis-store.js';

export type Store = DeviceRepository & ManifestRepository & AssetRepository & RuntimeStateRepository;

let activeStore: Store = new InMemoryStore();
let activeCloser: (() => Promise<void>) | undefined;

export const store: Store = new Proxy(
  {},
  {
    get(_target, property) {
      const value = (activeStore as unknown as Record<PropertyKey, unknown>)[property];
      return typeof value === 'function' ? (value as (...args: unknown[]) => unknown).bind(activeStore) : value;
    }
  }
) as Store;

export async function initializeStore(config: AppConfig): Promise<void> {
  if (config.storageDriver === 'memory') {
    activeStore = new InMemoryStore();
    activeCloser = undefined;
    return;
  }

  const sqlRedisStore = await SqlRedisStore.create(config);
  activeStore = sqlRedisStore;
  activeCloser = () => sqlRedisStore.close();
}

export async function closeStore(): Promise<void> {
  if (activeCloser) {
    await activeCloser();
    activeCloser = undefined;
  }
}
