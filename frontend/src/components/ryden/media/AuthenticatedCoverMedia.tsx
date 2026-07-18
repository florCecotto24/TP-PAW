import { useEffect, useState, type CSSProperties, type ReactNode } from 'react';
import { sessionClient } from '../../../session/sessionStore';
import { paintVideoPoster, videoPreviewSrc } from './videoPoster';

export type AuthenticatedCoverMediaProps = {
  /** API path or URN (e.g. {@code links.cover} or picture {@code links.self}). Loaded with Bearer. */
  src: string | null | undefined;
  alt?: string;
  className?: string;
  style?: CSSProperties;
  /** Hint when the MIME is still unknown (e.g. picture DTO {@code kind}). */
  preferVideo?: boolean;
  fallback?: ReactNode;
};

/**
 * Cover thumbnail for car cards/headers. Sniffs the blob MIME so legacy video-only
 * galleries (no photo cover) still show a poster instead of a broken {@code <img>}.
 */
export default function AuthenticatedCoverMedia({
  src,
  alt = '',
  className,
  style,
  preferVideo = false,
  fallback = null,
}: AuthenticatedCoverMediaProps) {
  const [objectUrl, setObjectUrl] = useState<string | null>(null);
  const [isVideo, setIsVideo] = useState(preferVideo);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    let active = true;
    let localUrl: string | null = null;
    setObjectUrl(null);
    setFailed(false);
    setIsVideo(preferVideo);
    if (!src) {
      setFailed(true);
      return undefined;
    }
    void sessionClient
      .getBlob(src)
      .then((blob) => {
        if (!active) return;
        if (!blob) {
          setFailed(true);
          return;
        }
        const type = (blob.type || '').toLowerCase();
        setIsVideo(type.startsWith('video/') || (preferVideo && !type.startsWith('image/')));
        localUrl = URL.createObjectURL(blob);
        setObjectUrl(localUrl);
      })
      .catch(() => {
        if (active) setFailed(true);
      });
    return () => {
      active = false;
      if (localUrl) URL.revokeObjectURL(localUrl);
    };
  }, [src, preferVideo]);

  if (!objectUrl || failed) {
    return <>{fallback}</>;
  }

  if (isVideo) {
    return (
      <video
        src={videoPreviewSrc(objectUrl)}
        className={className}
        style={style}
        muted
        playsInline
        preload="metadata"
        aria-label={alt || undefined}
        onLoadedMetadata={(ev) => paintVideoPoster(ev.currentTarget)}
        onLoadedData={(ev) => paintVideoPoster(ev.currentTarget)}
      />
    );
  }

  return <img src={objectUrl} alt={alt} className={className} style={style} />;
}
