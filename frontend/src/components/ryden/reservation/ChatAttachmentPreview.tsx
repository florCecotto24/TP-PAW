import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { sessionClient } from '../../../session/sessionStore';
import { openBinaryLink } from '../../../features/reservations/api';
import {
  chatAttachmentMetaLine,
  type ChatAttachmentMeta,
} from '../../../features/reservations/chatAttachment';

function isImageType(contentType: string): boolean {
  return contentType.startsWith('image/');
}

function isVideoType(contentType: string): boolean {
  return contentType.startsWith('video/');
}

/** Adjunto inline en burbuja de chat (imagen/video visible; resto como tarjeta descargable). */
export default function ChatAttachmentPreview({
  attachmentUri,
  attachmentMeta,
  onVisualKind,
}: {
  attachmentUri: string;
  attachmentMeta?: ChatAttachmentMeta | null;
  onVisualKind?: (visual: boolean) => void;
}) {
  const { t } = useTranslation();
  const [objectUrl, setObjectUrl] = useState<string | null>(null);
  const [contentType, setContentType] = useState('');
  const [failed, setFailed] = useState(false);

  const displayName = attachmentMeta?.fileName?.trim() || t('res.chat.attachment');
  const metaLine = attachmentMeta ? chatAttachmentMetaLine(attachmentMeta, t) : null;

  useEffect(() => {
    let active = true;
    let localUrl: string | null = null;
    void sessionClient
      .getBlob(attachmentUri)
      .then((blob) => {
        if (!active || !blob) {
          setFailed(true);
          return;
        }
        const type = blob.type || attachmentMeta?.contentType || 'application/octet-stream';
        setContentType(type);
        const visual = isImageType(type) || isVideoType(type);
        onVisualKind?.(visual);
        if (visual) {
          localUrl = URL.createObjectURL(blob);
          setObjectUrl(localUrl);
        }
      })
      .catch(() => {
        if (active) {
          onVisualKind?.(false);
          setFailed(true);
        }
      });
    return () => {
      active = false;
      if (localUrl) URL.revokeObjectURL(localUrl);
    };
  }, [attachmentUri, attachmentMeta?.contentType, onVisualKind]);

  if (objectUrl && isImageType(contentType)) {
    return (
      <div>
        <a
          href={objectUrl}
          target="_blank"
          rel="noopener noreferrer"
          className="reservation-chat__attachment-media d-block"
        >
          <img src={objectUrl} alt="" className="reservation-chat__attachment-image" />
        </a>
        {attachmentMeta ? (
          <p className="reservation-chat__attachment-caption small text-muted mb-0 mt-1">
            <span className="d-block text-truncate">{displayName}</span>
            {metaLine ? <span className="d-block">{metaLine}</span> : null}
          </p>
        ) : null}
      </div>
    );
  }

  if (objectUrl && isVideoType(contentType)) {
    return (
      <div>
        <div className="reservation-chat__attachment-media">
          <video src={objectUrl} className="reservation-chat__attachment-video" controls preload="metadata" />
        </div>
        {attachmentMeta ? (
          <p className="reservation-chat__attachment-caption small text-muted mb-0 mt-1">
            <span className="d-block text-truncate">{displayName}</span>
            {metaLine ? <span className="d-block">{metaLine}</span> : null}
          </p>
        ) : null}
      </div>
    );
  }

  return (
    <button
      type="button"
      className="reservation-chat__file-card btn btn-link text-start w-100 text-decoration-none p-2"
      onClick={() => void openBinaryLink(attachmentUri)}
    >
      <span className="reservation-chat__file-card-icon bi bi-paperclip" aria-hidden="true" />
      <span className="reservation-chat__file-card-body">
        <span className="reservation-chat__file-card-name text-truncate d-block">{displayName}</span>
        {metaLine ? (
          <span className="reservation-chat__file-card-meta d-block">{metaLine}</span>
        ) : null}
        {failed ? (
          <span className="reservation-chat__file-card-meta text-danger">{t('res.chat.attachmentError')}</span>
        ) : null}
      </span>
    </button>
  );
}
