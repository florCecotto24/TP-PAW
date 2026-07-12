import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { sessionClient } from '../../../session/sessionStore';
import { openBinaryLink } from '../../../features/reservations/api';

function isImageType(contentType: string): boolean {
  return contentType.startsWith('image/');
}

function isVideoType(contentType: string): boolean {
  return contentType.startsWith('video/');
}

/** Adjunto inline en burbuja de chat (imagen/video visible; resto como tarjeta descargable). */
export default function ChatAttachmentPreview({
  attachmentUri,
  onVisualKind,
}: {
  attachmentUri: string;
  onVisualKind?: (visual: boolean) => void;
}) {
  const { t } = useTranslation();
  const [objectUrl, setObjectUrl] = useState<string | null>(null);
  const [contentType, setContentType] = useState('');
  const [failed, setFailed] = useState(false);

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
        const type = blob.type || 'application/octet-stream';
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
  }, [attachmentUri, onVisualKind]);

  if (objectUrl && isImageType(contentType)) {
    return (
      <a href={objectUrl} target="_blank" rel="noopener noreferrer" className="reservation-chat__attachment-media d-block">
        <img src={objectUrl} alt="" className="reservation-chat__attachment-image" />
      </a>
    );
  }

  if (objectUrl && isVideoType(contentType)) {
    return (
      <div className="reservation-chat__attachment-media">
        <video src={objectUrl} className="reservation-chat__attachment-video" controls preload="metadata" />
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
        <span className="reservation-chat__file-card-name">{t('res.chat.attachment')}</span>
        {failed ? (
          <span className="reservation-chat__file-card-meta text-danger">{t('res.chat.attachmentError')}</span>
        ) : null}
      </span>
    </button>
  );
}
