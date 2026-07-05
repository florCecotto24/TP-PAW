import { sessionClient } from '../session/sessionStore';

const VIEWABLE_SIGNATURES: ReadonlyArray<{ mime: string; match: (bytes: Uint8Array) => boolean }> = [
  {
    mime: 'application/pdf',
    match: (b) => b.length >= 4 && b[0] === 0x25 && b[1] === 0x50 && b[2] === 0x44 && b[3] === 0x46,
  },
  {
    mime: 'image/jpeg',
    match: (b) => b.length >= 2 && b[0] === 0xff && b[1] === 0xd8,
  },
  {
    mime: 'image/png',
    match: (b) => b.length >= 4 && b[0] === 0x89 && b[1] === 0x50 && b[2] === 0x4e && b[3] === 0x47,
  },
  {
    mime: 'image/gif',
    match: (b) => b.length >= 3 && b[0] === 0x47 && b[1] === 0x49 && b[2] === 0x46,
  },
  {
    mime: 'image/webp',
    match: (b) =>
      b.length >= 12
      && b[0] === 0x52
      && b[1] === 0x49
      && b[2] === 0x46
      && b[3] === 0x46
      && b[8] === 0x57
      && b[9] === 0x45
      && b[10] === 0x42
      && b[11] === 0x50,
  },
];

function defaultDownloadName(contentType: string, fileName?: string): string {
  if (fileName && fileName.includes('.')) return fileName;
  if (contentType.includes('pdf')) return 'document.pdf';
  if (contentType.startsWith('image/')) {
    const sub = contentType.split('/')[1]?.split(';')[0] ?? 'bin';
    return `document.${sub === 'jpeg' ? 'jpg' : sub}`;
  }
  return fileName ?? 'document';
}

function canOpenInline(contentType: string, fileName?: string): boolean {
  const name = (fileName ?? '').toLowerCase();
  if (name.endsWith('.pdf')) return true;
  if (/\.(jpe?g|png|gif|webp|bmp|svg)$/.test(name)) return true;
  return contentType.startsWith('image/') || contentType.includes('pdf');
}

function sniffViewableMime(bytes: Uint8Array): string | null {
  for (const { mime, match } of VIEWABLE_SIGNATURES) {
    if (match(bytes)) return mime;
  }
  return null;
}

async function resolveViewableMime(blob: Blob, fileName?: string): Promise<string | null> {
  const declared = blob.type || 'application/octet-stream';
  if (canOpenInline(declared, fileName)) return declared;
  const header = new Uint8Array(await blob.slice(0, 12).arrayBuffer());
  return sniffViewableMime(header);
}

/**
 * Descarga un recurso binario autenticado (documento KYC, comprobante de pago/reembolso, adjunto
 * de chat) vía el cliente central ({@link sessionClient.getBlobDownload}) y lo abre en una pestaña
 * nueva o lo descarga con un nombre razonable.
 *
 * Usa un `<a>` programático en lugar de `window.open` para evitar falsos negativos cuando el
 * navegador bloquea pop-ups tras un `await` (el blob sí llegó, pero la UI mostraba error).
 */
export async function openAuthenticatedBinary(path: string): Promise<boolean> {
  const download = await sessionClient.getBlobDownload(path);
  if (!download) return false;

  const { blob, fileName } = download;
  const viewableMime = await resolveViewableMime(blob, fileName);
  const typedBlob =
    viewableMime && viewableMime !== blob.type ? new Blob([blob], { type: viewableMime }) : blob;
  const contentType = typedBlob.type || 'application/octet-stream';
  const objectUrl = URL.createObjectURL(typedBlob);
  const anchor = document.createElement('a');
  anchor.href = objectUrl;
  anchor.rel = 'noopener noreferrer';
  if (viewableMime) {
    anchor.target = '_blank';
  } else {
    anchor.download = defaultDownloadName(contentType, fileName);
  }
  document.body.appendChild(anchor);
  anchor.click();
  document.body.removeChild(anchor);
  setTimeout(() => URL.revokeObjectURL(objectUrl), 60_000);
  return true;
}

// Exported for unit tests.
export const __testing = {
  canOpenInline,
  sniffViewableMime,
  resolveViewableMime,
};
