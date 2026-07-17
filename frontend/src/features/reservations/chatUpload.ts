import { ApiError, AUTH_PROBE_PATH, extractPageLinks, type ApiResponse } from '../../api/client';
import { MediaTypes } from '../../api/mediaTypes';
import { encodeMultipart, type MultipartPart } from '../../api/multipart';
import { resolveApiUrl } from '../../api/uri';
import { sessionClient, useSessionStore } from '../../session/sessionStore';
import {
  computeUploadTimeoutMs,
  getChatMaxAttachmentMb,
  UPLOAD_MAX_RETRIES,
} from './chatAttachment';
import type { MessageDto } from './types';

function absorbAuthHeaders(xhr: XMLHttpRequest): void {
  const access = xhr.getResponseHeader('X-Access-Token');
  const refresh = xhr.getResponseHeader('X-Refresh-Token');
  if (access && refresh) {
    useSessionStore.getState().applyTokens(access, refresh);
  }
}

function xhrPostMessage(
  url: string,
  body: Uint8Array,
  contentType: string,
  opts: { timeoutMs: number; onProgress?: (percent: number | null) => void },
): Promise<ApiResponse<MessageDto>> {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    const absoluteUrl = resolveApiUrl(url);
    xhr.open('POST', absoluteUrl, true);
    xhr.timeout = opts.timeoutMs;
    xhr.setRequestHeader('Accept', MediaTypes.message);
    // Boundary must be explicit — native FormData often arrives without it under Tomcat/Jersey
    // (MIMEParsingException: Missing start boundary → bare 400).
    xhr.setRequestHeader('Content-Type', contentType);

    const token = useSessionStore.getState().accessToken;
    if (token) xhr.setRequestHeader('Authorization', `Bearer ${token}`);

    xhr.upload.onprogress = (ev) => {
      if (opts.onProgress && ev.lengthComputable) {
        opts.onProgress((ev.loaded / ev.total) * 100);
      }
    };

    xhr.onload = () => {
      absorbAuthHeaders(xhr);
      opts.onProgress?.(null);
      const status = xhr.status;
      let data: unknown;
      try {
        data = xhr.responseText ? JSON.parse(xhr.responseText) : undefined;
      } catch {
        data = xhr.responseText;
      }
      if (status >= 200 && status < 300) {
        const headers = new Headers();
        const link = xhr.getResponseHeader('Link');
        if (link) headers.set('Link', link);
        const total = xhr.getResponseHeader('X-Total-Count');
        if (total) headers.set('X-Total-Count', total);
        resolve({
          data: data as MessageDto,
          status,
          page: extractPageLinks(headers),
          location: xhr.getResponseHeader('Location'),
          headers,
        });
        return;
      }
      const errBody =
        data && typeof data === 'object'
          ? { status, ...(data as { code?: string; message?: string }) }
          : { status, message: String(data ?? '') };
      reject(new ApiError(status, errBody));
    };

    xhr.onerror = () => {
      opts.onProgress?.(null);
      reject(new Error('network'));
    };
    xhr.ontimeout = () => {
      opts.onProgress?.(null);
      reject(new Error('timeout'));
    };

    opts.onProgress?.(0);
    // Copy into a plain ArrayBuffer — Uint8Array.buffer may be SharedArrayBuffer under TS DOM libs.
    const payload = new ArrayBuffer(body.byteLength);
    new Uint8Array(payload).set(body);
    xhr.send(payload);
  });
}

async function refreshAccessToken(): Promise<void> {
  const refresh = useSessionStore.getState().refreshToken;
  if (!refresh) throw new ApiError(401);
  await sessionClient.request(AUTH_PROBE_PATH, {
    method: 'GET',
    accept: MediaTypes.api,
    authorization: `Bearer ${refresh}`,
    retryOnUnauthorized: false,
  });
}

async function postWithRefreshRetry(
  url: string,
  body: Uint8Array,
  contentType: string,
  opts: { timeoutMs: number; onProgress?: (percent: number | null) => void },
): Promise<ApiResponse<MessageDto>> {
  try {
    return await xhrPostMessage(url, body, contentType, opts);
  } catch (err) {
    if (!(err instanceof ApiError) || err.status !== 401) throw err;
    await refreshAccessToken();
    return xhrPostMessage(url, body, contentType, opts);
  }
}

async function encodeChatMessageBody(
  text: string,
  file: File | null,
): Promise<{ body: Uint8Array; contentType: string }> {
  const parts: MultipartPart[] = [{ name: 'body', value: text ?? '' }];
  if (file) {
    parts.push({
      name: 'file',
      value: file,
      filename: file.name || 'attachment',
      contentType: file.type || 'application/octet-stream',
    });
  }
  return encodeMultipart(parts);
}

/** Envía un mensaje de chat con progreso de subida (multipart vía XHR). */
export async function sendChatMessage(
  messagesUri: string,
  body: string,
  file: File | null,
  options?: {
    onProgress?: (percent: number | null) => void;
    maxAttachmentMb?: number;
  },
): Promise<ApiResponse<MessageDto>> {
  const { body: payload, contentType } = await encodeChatMessageBody(body, file);

  const maxMb = options?.maxAttachmentMb ?? getChatMaxAttachmentMb();
  const timeoutMs = file ? computeUploadTimeoutMs(maxMb) : 60_000;
  const uploadOpts = { timeoutMs, onProgress: options?.onProgress };

  let lastError: unknown;
  for (let attempt = 0; attempt <= UPLOAD_MAX_RETRIES; attempt++) {
    try {
      return await postWithRefreshRetry(messagesUri, payload, contentType, uploadOpts);
    } catch (err) {
      lastError = err;
      const retriable =
        err instanceof Error && (err.message === 'network' || err.message === 'timeout');
      if (retriable && attempt < UPLOAD_MAX_RETRIES) continue;
      throw err;
    }
  }
  throw lastError;
}
