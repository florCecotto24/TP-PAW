import { describe, expect, it } from 'vitest';
import { __testing } from './openAuthenticatedBinary';

describe('openAuthenticatedBinary helpers', () => {
  it('testSniffViewableMimeDetectsPdfMagicBytes', () => {
    const bytes = new Uint8Array([0x25, 0x50, 0x44, 0x46, 0x2d]);
    expect(__testing.sniffViewableMime(bytes)).toBe('application/pdf');
  });

  it('testCanOpenInlineUsesOctetStreamWithoutExtensionAsDownload', () => {
    expect(__testing.canOpenInline('application/octet-stream', 'insurance')).toBe(false);
  });
});
