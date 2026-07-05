import { describe, expect, it } from 'vitest';
import {
  CHAT_MAX_ATTACHMENT_MB,
  computeUploadTimeoutMs,
  formatFileSize,
  isAllowedChatFile,
  validateChatFile,
} from './chatAttachment';

describe('chatAttachment', () => {
  it('allows common image extensions', () => {
    const file = new File(['x'], 'photo.jpg', { type: 'image/jpeg' });
    expect(isAllowedChatFile(file)).toBe(true);
    expect(validateChatFile(file)).toBeNull();
  });

  it('rejects unknown types', () => {
    const file = new File(['x'], 'virus.exe', { type: 'application/octet-stream' });
    expect(validateChatFile(file)).toBe('invalidType');
  });

  it('rejects oversized files', () => {
    const file = new File(['x'], 'big.pdf', { type: 'application/pdf' });
    Object.defineProperty(file, 'size', { value: 26 * 1024 * 1024 });
    expect(validateChatFile(file, CHAT_MAX_ATTACHMENT_MB)).toBe('tooLarge');
  });

  it('formats file sizes', () => {
    expect(formatFileSize(512)).toBe('512 B');
    expect(formatFileSize(2048)).toBe('2.0 KB');
    expect(formatFileSize(2 * 1024 * 1024)).toBe('2.0 MB');
  });

  it('computes upload timeout from megabytes', () => {
    expect(computeUploadTimeoutMs(25)).toBe(125_000);
    expect(computeUploadTimeoutMs(1)).toBe(120_000);
  });
});
