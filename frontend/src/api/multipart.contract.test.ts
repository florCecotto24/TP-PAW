import { describe, expect, it } from 'vitest';
import { encodeMultipart, sanitizeMultipartFilename } from './multipart';

describe('multipart contract (frontend)', () => {
  it('testSanitizeMultipartFilenameStripsCrLfAndQuotes', () => {
    // 1.Arrange
    const unsafe = 'a\r\n"b.jpg';

    // 2.Act
    const safe = sanitizeMultipartFilename(unsafe);

    // 3.Assert
    expect(safe).toBe('a___b.jpg');
  });

  it('testEncodeMultipartMatchesOpenApiMultipartShape', async () => {
    // 1.Arrange
    const parts = [
      { name: 'car', value: '{"plate":"ABC123"}', contentType: 'application/vnd.paw.car.v1+json' },
      {
        name: 'pictures',
        value: new Uint8Array([1, 2, 3]),
        filename: 'photo.jpg',
        contentType: 'image/jpeg',
      },
    ];

    // 2.Act
    const { body, contentType } = await encodeMultipart(parts);

    // 3.Assert
    expect(contentType.startsWith('multipart/form-data; boundary=')).toBe(true);
    const boundary = contentType.slice('multipart/form-data; boundary='.length);
    const text = new TextDecoder().decode(body);
    expect(text).toContain(`--${boundary}`);
    expect(text).toContain('name="car"');
    expect(text).toContain('Content-Type: application/vnd.paw.car.v1+json');
    expect(text).toContain('filename="photo.jpg"');
    expect(text).toContain(`--${boundary}--`);
    expect(text).not.toContain('filename="car');
  });
});
