/** Validación de adjuntos de chat — espejo de `reservation-chat.js`. */

export const CHAT_MAX_ATTACHMENT_MB = 25;
export const CHAT_MESSAGE_MAX_LENGTH = 1000;
/** Espejo de {@code app.reservation.chat.history-page-size}. */
export const CHAT_HISTORY_PAGE_SIZE = 12;
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

export function computeUploadTimeoutMs(maxAttachmentMb = CHAT_MAX_ATTACHMENT_MB): number {
  const mb = Number.isFinite(maxAttachmentMb) ? maxAttachmentMb : CHAT_MAX_ATTACHMENT_MB;
  return Math.max(MIN_UPLOAD_TIMEOUT_MS, mb * MS_PER_MEGABYTE);
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
  maxAttachmentMb = CHAT_MAX_ATTACHMENT_MB,
): ChatFileValidationError | null {
  if (!file) return 'invalidType';
  const maxBytes = maxAttachmentMb * 1024 * 1024;
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
