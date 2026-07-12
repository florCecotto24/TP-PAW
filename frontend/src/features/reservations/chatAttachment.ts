/** Validación de adjuntos de chat — límites desde GET /config. */

import { getClientConfig, megabytesToBytes } from '../../api/clientConfig';

export const UPLOAD_MAX_RETRIES = 1;

const MIN_UPLOAD_TIMEOUT_MS = 120_000;
const MS_PER_MEGABYTE = 5000;

const ALLOWED_EXTENSIONS: Record<string, true> = {
  jpg: true,
  jpeg: true,
  png: true,
  gif: true,
  webp: true,
  bmp: true,
  svg: true,
  pdf: true,
  doc: true,
  docx: true,
  mp4: true,
  webm: true,
  mov: true,
  txt: true,
  zip: true,
  xls: true,
  xlsx: true,
  ppt: true,
  pptx: true,
};

export function chatLimits() {
  return getClientConfig().chat;
}

/** @deprecated Prefer {@link chatLimits}.maxAttachmentMegabytes */
export function getChatMaxAttachmentMb(): number {
  return chatLimits().maxAttachmentMegabytes;
}

/** @deprecated Prefer {@link chatLimits}.messageMaxLength */
export function getChatMessageMaxLength(): number {
  return chatLimits().messageMaxLength;
}

/** @deprecated Prefer {@link chatLimits}.historyPageSize */
export function getChatHistoryPageSize(): number {
  return chatLimits().historyPageSize;
}

export function computeUploadTimeoutMs(maxAttachmentMb?: number): number {
  const mb = maxAttachmentMb ?? chatLimits().maxAttachmentMegabytes;
  const safe = Number.isFinite(mb) ? mb : chatLimits().maxAttachmentMegabytes;
  return Math.max(MIN_UPLOAD_TIMEOUT_MS, safe * MS_PER_MEGABYTE);
}

function fileExtension(name: string): string {
  const lower = name.toLowerCase();
  const dot = lower.lastIndexOf('.');
  return dot >= 0 ? lower.slice(dot + 1) : '';
}

export function isAllowedChatFile(file: File): boolean {
  const ext = fileExtension(file.name);
  if (ext && ALLOWED_EXTENSIONS[ext]) return true;
  const type = (file.type || '').toLowerCase();
  if (type.startsWith('image/')) return true;
  if (type === 'application/pdf') return true;
  if (
    type === 'application/msword'
    || type === 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
  ) {
    return true;
  }
  if (type === 'video/mp4' || type === 'video/webm' || type === 'video/quicktime') return true;
  if (type === 'text/plain' || type === 'application/zip' || type === 'application/x-zip-compressed') {
    return true;
  }
  return false;
}

export type ChatFileValidationError = 'invalidType' | 'tooLarge';

export function validateChatFile(
  file: File | null | undefined,
  maxAttachmentMb?: number,
): ChatFileValidationError | null {
  if (!file) return 'invalidType';
  const mb = maxAttachmentMb ?? chatLimits().maxAttachmentMegabytes;
  const maxBytes = megabytesToBytes(mb);
  if (file.size > maxBytes) return 'tooLarge';
  if (!isAllowedChatFile(file)) return 'invalidType';
  return null;
}

export function formatFileSize(bytes: number): string {
  if (!Number.isFinite(bytes) || bytes < 0) return '';
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

export const CHAT_FILE_ACCEPT =
  'image/*,application/pdf,.doc,.docx,video/mp4,video/webm,video/quicktime,.txt,.zip,.xls,.xlsx,.ppt,.pptx';
