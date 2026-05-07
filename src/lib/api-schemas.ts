const successEnvelope = <TBody extends Record<string, unknown>>(dataProperties: TBody) => ({
  type: 'object',
  required: ['success', 'data'],
  properties: {
    success: { type: 'boolean', const: true },
    message: { type: 'string' },
    data: {
      type: 'object',
      properties: dataProperties
    }
  }
}) as const;

const manifestAssetSchema = {
  type: 'object',
  required: [
    'asset_id',
    'asset_type',
    'file_name',
    'asset_ref',
    'oss_path',
    'cdn_path',
    'mime_type',
    'size_bytes',
    'sha256',
    'duration_ms',
    'required',
    'priority'
  ],
  properties: {
    asset_id: { type: 'string' },
    asset_type: { type: 'string' },
    file_name: { type: 'string' },
    asset_ref: { type: 'string' },
    oss_path: { type: 'string' },
    cdn_path: { type: 'string' },
    mime_type: { type: 'string' },
    size_bytes: { type: 'number' },
    sha256: { type: 'string' },
    duration_ms: { type: 'number' },
    required: { type: 'boolean' },
    priority: { type: 'number' }
  }
} as const;

export const registerPlayerSchema = {
  body: {
    type: 'object',
    required: [
      'device_sn',
      'activation_code',
      'device_name',
      'platform',
      'app_version',
      'os_version',
      'screen_resolution',
      'timezone',
      'mac_address',
      'ip_address',
      'capabilities'
    ],
    properties: {
      device_sn: { type: 'string' },
      activation_code: { type: 'string' },
      device_name: { type: 'string' },
      platform: { type: 'string' },
      app_version: { type: 'string' },
      os_version: { type: 'string' },
      firmware_version: { type: 'string' },
      screen_resolution: { type: 'string' },
      timezone: { type: 'string' },
      mac_address: { type: 'string' },
      ip_address: { type: 'string' },
      capabilities: { type: 'object', additionalProperties: true }
    }
  },
  response: {
    200: successEnvelope({
      device_id: { type: 'string' },
      tenant_id: { type: 'string' },
      location_id: { type: 'string' },
      access_token: { type: 'string' },
      token_expire_at: { type: 'number' },
      config: {
        type: 'object',
        properties: {
          device_id: { type: 'string' },
          tenant_id: { type: 'string' },
          location_id: { type: 'string' },
          heartbeat_interval_sec: { type: 'number' },
          manifest_sync_interval_sec: { type: 'number' },
          event_flush_interval_sec: { type: 'number' },
          max_cache_size_mb: { type: 'number' },
          asset_download_concurrency: { type: 'number' },
          enable_offline_mode: { type: 'boolean' },
          enable_watchdog: { type: 'boolean' },
          enable_screenshot: { type: 'boolean' },
          log_level: { type: 'string' },
          supported_asset_types: {
            type: 'array',
            items: { type: 'string' }
          }
        }
      }
    })
  }
} as const;

export const reportPlayerEventsSchema = {
  body: {
    type: 'object',
    required: ['device_id', 'events'],
    properties: {
      device_id: { type: 'string' },
      events: {
        type: 'array',
        minItems: 1,
        items: {
          type: 'object',
          required: ['event_id', 'event_type', 'timestamp'],
          properties: {
            event_id: { type: 'string' },
            event_type: { type: 'string' },
            timestamp: { type: 'number' },
            manifest_id: { type: 'string' },
            manifest_version: { type: 'number' },
            asset_id: { type: 'string' },
            playlist_item_id: { type: 'string' },
            error_code: { type: 'string' },
            error_message: { type: 'string' },
            extra_json: { type: 'string' }
          }
        }
      }
    }
  },
  response: {
    200: successEnvelope({
      success: { type: 'boolean' },
      accepted_count: { type: 'number' },
      rejected_count: { type: 'number' }
    })
  }
} as const;

export const pullManifestSchema = {
  body: {
    type: 'object',
    required: [
      'device_id',
      'tenant_id',
      'location_id',
      'app_version',
      'platform',
      'screen_resolution'
    ],
    properties: {
      device_id: { type: 'string' },
      tenant_id: { type: 'string' },
      location_id: { type: 'string' },
      current_manifest_id: { type: 'string' },
      current_manifest_version: { type: 'number' },
      app_version: { type: 'string' },
      platform: { type: 'string' },
      screen_resolution: { type: 'string' },
      last_success_sync_at: { type: 'number' }
    }
  },
  response: {
    200: successEnvelope({
      update_type: { type: 'string' },
      manifest: {
        anyOf: [
          { type: 'null' },
          {
            type: 'object',
            properties: {
              manifest_id: { type: 'string' },
              version: { type: 'number' },
              tenant_id: { type: 'string' },
              device_id: { type: 'string' },
              location_id: { type: 'string' },
              group_id: { anyOf: [{ type: 'string' }, { type: 'null' }] },
              valid_from: { type: 'number' },
              valid_to: { type: 'number' },
              ttl_sec: { type: 'number' },
              template_config: {
                type: 'object',
                properties: {
                  template_id: { type: 'string' },
                  template_version: { type: 'string' },
                  design_width: { type: 'number' },
                  design_height: { type: 'number' },
                  slots: {
                    type: 'array',
                    items: {
                      type: 'object',
                      properties: {
                        slot_id: { type: 'string' },
                        slot_type: { type: 'string' },
                        required: { type: 'boolean' }
                      }
                    }
                  }
                }
              },
              playback_plan: {
                type: 'object',
                properties: {
                  plan_id: { type: 'string' },
                  play_mode: { type: 'string' },
                  scenes: {
                    type: 'array',
                    items: {
                      type: 'object',
                      properties: {
                        scene_id: { type: 'string' },
                        scene_name: { type: 'string' },
                        duration_ms: { type: 'number' },
                        order: { type: 'number' },
                        slot_bindings: {
                          type: 'array',
                          items: {
                            type: 'object',
                            properties: {
                              slot_id: { type: 'string' },
                              content_type: { type: 'string' },
                              asset_id: { type: 'string' },
                              text: { type: 'string' },
                              display_policy: {
                                type: 'object',
                                properties: {
                                  object_fit: { type: 'string' },
                                  muted: { type: 'boolean' },
                                  loop: { type: 'boolean' }
                                }
                              }
                            }
                          }
                        }
                      }
                    }
                  }
                }
              },
              assets: {
                type: 'array',
                items: manifestAssetSchema
              },
              cache_policy: {
                type: 'object',
                properties: {
                  max_cache_size_mb: { type: 'number' },
                  min_free_storage_mb: { type: 'number' },
                  allow_delete_unused_assets: { type: 'boolean' }
                }
              },
              fallback_policy: {
                type: 'object',
                properties: {
                  fallback_asset_id: { type: 'string' },
                  fallback_text: { type: 'string' },
                  max_retry_count: { type: 'number' },
                  retry_interval_sec: { type: 'number' },
                  loop_last_good_manifest: { type: 'boolean' },
                  show_black_screen_allowed: { type: 'boolean' }
                }
              },
              checksum: { type: 'string' },
              generated_at: { type: 'number' }
            }
          }
        ]
      },
      next_poll_interval_sec: { type: 'number' },
      server_time: { type: 'number' },
      message: { type: 'string' }
    })
  }
} as const;

export const batchGetAssetUrlsSchema = {
  body: {
    type: 'object',
    required: ['device_id', 'manifest_id', 'manifest_version', 'asset_ids'],
    properties: {
      device_id: { type: 'string' },
      manifest_id: { type: 'string' },
      manifest_version: { type: 'number' },
      asset_ids: {
        type: 'array',
        minItems: 1,
        items: { type: 'string' }
      }
    }
  },
  response: {
    200: successEnvelope({
      assets: {
        type: 'array',
        items: {
          type: 'object',
          properties: {
            asset_id: { type: 'string' },
            download_url: { type: 'string' },
            expire_at: { type: 'number' },
            sha256: { type: 'string' },
            size_bytes: { type: 'number' }
          }
        }
      }
    })
  }
} as const;

export const heartbeatSchema = {
  body: {
    type: 'object',
    required: [
      'device_id',
      'app_version',
      'manifest_id',
      'manifest_version',
      'timestamp',
      'playback',
      'health',
      'cache',
      'network'
    ],
    properties: {
      device_id: { type: 'string' },
      app_version: { type: 'string' },
      manifest_id: { type: 'string' },
      manifest_version: { type: 'number' },
      timestamp: { type: 'number' },
      playback: { type: 'object', additionalProperties: true },
      health: { type: 'object', additionalProperties: true },
      cache: { type: 'object', additionalProperties: true },
      network: { type: 'object', additionalProperties: true }
    }
  },
  response: {
    200: successEnvelope({
      success: { type: 'boolean' },
      next_interval_sec: { type: 'number' },
      commands: {
        type: 'array',
        items: {
          type: 'object',
          properties: {
            command_id: { type: 'string' },
            type: { type: 'string' },
            issued_at: { type: 'number' },
            expire_at: { type: 'number' },
            payload_json: { type: 'string' }
          }
        }
      }
    })
  }
} as const;

export const ackCommandSchema = {
  body: {
    type: 'object',
    required: ['device_id', 'command_id', 'type', 'success', 'executed_at'],
    properties: {
      device_id: { type: 'string' },
      command_id: { type: 'string' },
      type: { type: 'string' },
      success: { type: 'boolean' },
      error_code: { type: 'string' },
      error_message: { type: 'string' },
      executed_at: { type: 'number' }
    }
  },
  response: {
    200: successEnvelope({
      success: { type: 'boolean' }
    })
  }
} as const;
