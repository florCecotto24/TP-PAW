/**
 * Multipart encoder propio.
 *
 * Evita depender de que el browser arme `Content-Type: multipart/form-data; boundary=…`.
 * Si el header llega sin boundary, Jersey responde 400 genérico (~68 bytes).
 */

export type MultipartPart = {
  name: string;
  value: Blob | string | ArrayBuffer | Uint8Array;
  /** Si se omite, la parte va sin filename (p.ej. JSON `car`). */
  filename?: string;
  /** Content-Type de la parte; si falta y value es Blob, se usa `value.type`. */
  contentType?: string;
};

function newBoundary(): string {
  const rand =
    typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function'
      ? crypto.randomUUID().replace(/-/g, '')
      : `${Date.now().toString(16)}${Math.random().toString(16).slice(2)}`;
  return `----RydenFormBoundary${rand}`;
}

/** Evita romper el framing multipart con CR/LF/comillas en el filename. */
export function sanitizeMultipartFilename(name: string): string {
  return name.replace(/[\r\n"]/g, '_');
}

async function partBytes(value: MultipartPart['value']): Promise<Uint8Array> {
  if (typeof value === 'string') {
    return new TextEncoder().encode(value);
  }
  if (value instanceof Uint8Array) {
    return value;
  }
  if (value instanceof ArrayBuffer) {
    return new Uint8Array(value);
  }
  // Blob / File
  return new Uint8Array(await value.arrayBuffer());
}

function partContentType(part: MultipartPart): string {
  if (part.contentType) return part.contentType;
  if (typeof part.value !== 'string'
    && !(part.value instanceof Uint8Array)
    && !(part.value instanceof ArrayBuffer)
    && 'type' in part.value
    && part.value.type) {
    return part.value.type;
  }
  return '';
}

/**
 * Arma un body multipart + el Content-Type con boundary incluido.
 * El caller debe mandar ese Content-Type tal cual (no dejar que el browser lo invente).
 */
export async function encodeMultipart(
  parts: MultipartPart[],
): Promise<{ body: Uint8Array; contentType: string }> {
  const boundary = newBoundary();
  const enc = new TextEncoder();
  const chunks: Uint8Array[] = [];
  let total = 0;

  for (const part of parts) {
    let head = `--${boundary}\r\nContent-Disposition: form-data; name="${part.name}"`;
    if (part.filename != null && part.filename !== '') {
      head += `; filename="${sanitizeMultipartFilename(part.filename)}"`;
    }
    head += '\r\n';
    const partType = partContentType(part);
    if (partType) {
      head += `Content-Type: ${partType}\r\n`;
    }
    head += '\r\n';
    const headBytes = enc.encode(head);
    const valueBytes = await partBytes(part.value);
    const tail = enc.encode('\r\n');
    chunks.push(headBytes, valueBytes, tail);
    total += headBytes.length + valueBytes.length + tail.length;
  }
  const end = enc.encode(`--${boundary}--\r\n`);
  chunks.push(end);
  total += end.length;

  const body = new Uint8Array(total);
  let offset = 0;
  for (const chunk of chunks) {
    body.set(chunk, offset);
    offset += chunk.length;
  }

  return {
    body,
    contentType: `multipart/form-data; boundary=${boundary}`,
  };
}
