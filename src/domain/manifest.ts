export interface SlotDefinitionRecord {
  slotId: string;
  slotType: string;
  required: boolean;
}

export interface TemplateConfigRecord {
  templateId: string;
  templateVersion: string;
  designWidth: number;
  designHeight: number;
  slots: SlotDefinitionRecord[];
}

export interface DisplayPolicyRecord {
  objectFit: string;
  muted: boolean;
  loop: boolean;
}

export interface SlotBindingRecord {
  slotId: string;
  contentType: string;
  assetId: string;
  text: string;
  displayPolicy: DisplayPolicyRecord;
}

export interface PlaybackSceneRecord {
  sceneId: string;
  sceneName: string;
  durationMs: number;
  order: number;
  slotBindings: SlotBindingRecord[];
}

export interface PlaybackPlanRecord {
  planId: string;
  playMode: string;
  scenes: PlaybackSceneRecord[];
}

export interface AssetRecord {
  assetId: string;
  assetType: string;
  fileName: string;
  assetRef: string;
  ossPath: string;
  cdnPath: string;
  mimeType: string;
  sizeBytes: number;
  sha256: string;
  durationMs: number;
  required: boolean;
  priority: number;
  width?: number;
  height?: number;
  expireAt?: number;
}

export interface CachePolicyRecord {
  maxCacheSizeMb: number;
  minFreeStorageMb: number;
  allowDeleteUnusedAssets: boolean;
}

export interface FallbackPolicyRecord {
  fallbackAssetId: string;
  fallbackText: string;
  maxRetryCount: number;
  retryIntervalSec: number;
  loopLastGoodManifest: boolean;
  showBlackScreenAllowed: boolean;
}

export interface ManifestRecord {
  manifestId: string;
  version: number;
  tenantId: string;
  deviceId: string;
  locationId: string;
  groupId?: string;
  validFrom: number;
  validTo: number;
  ttlSec: number;
  templateId: string;
  templateVersion: string;
  designWidth: number;
  designHeight: number;
  playMode: string;
  templateConfigJson: TemplateConfigRecord;
  playbackPlanJson: PlaybackPlanRecord;
  assets: AssetRecord[];
  cachePolicy: CachePolicyRecord;
  fallbackPolicy: FallbackPolicyRecord;
  checksum: string;
  generatedAt: number;
}
