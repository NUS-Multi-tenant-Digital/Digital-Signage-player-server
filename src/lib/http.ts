export interface ApiEnvelope<T> {
  success: boolean;
  data: T;
  message?: string;
}

export function ok<T>(data: T, message?: string): ApiEnvelope<T> {
  return { success: true, data, message };
}
