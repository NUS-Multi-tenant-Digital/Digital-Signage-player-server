import { store } from '../../lib/app-context.js';

export interface CommandAckRequest {
  device_id: string;
  command_id: string;
  type: string;
  success: boolean;
  error_code?: string;
  error_message?: string;
  executed_at: number;
}

export async function ackCommand(input: CommandAckRequest) {
  await store.ackCommand({
    deviceId: input.device_id,
    commandId: input.command_id,
    type: input.type,
    success: input.success,
    errorCode: input.error_code,
    errorMessage: input.error_message,
    executedAt: input.executed_at,
    receivedAt: Date.now()
  });

  return {
    success: true
  };
}
