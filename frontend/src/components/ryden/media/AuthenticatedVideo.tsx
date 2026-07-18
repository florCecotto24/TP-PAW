import { useEffect, useState, type ReactNode, type VideoHTMLAttributes } from 'react';
import { sessionClient } from '../../../session/sessionStore';
import { paintVideoPoster, videoPreviewSrc } from './videoPoster';

export type AuthenticatedVideoProps = Omit<VideoHTMLAttributes<HTMLVideoElement>, 'src'> & {
  /** API path or URN (e.g. picture {@code links.self}). Loaded with Bearer. */
  src: string | null | undefined;
  /** Shown while the blob loads or if the fetch fails. */
  fallback?: ReactNode;
};

/**
 * {@code <video>} for API binaries that may require auth (same rationale as {@link AuthenticatedImg}).
 * Applies {@link videoPreviewSrc} / {@link paintVideoPoster} so the first frame is visible.
 */
export default function AuthenticatedVideo({
  src,
  className,
  fallback = null,
  ...videoProps
}: AuthenticatedVideoProps) {
  const [objectUrl, setObjectUrl] = useState<string | null>(null);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    let active = true;
    let localUrl: string | null = null;
    setObjectUrl(null);
    setFailed(false);
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
  }, [src]);

  if (!objectUrl || failed) {
    return <>{fallback}</>;
  }

  return (
    <video
      muted
      playsInline
      preload="metadata"
      {...videoProps}
      src={videoPreviewSrc(objectUrl)}
      className={className}
      onLoadedMetadata={(ev) => {
        paintVideoPoster(ev.currentTarget);
        videoProps.onLoadedMetadata?.(ev);
      }}
      onLoadedData={(ev) => {
        paintVideoPoster(ev.currentTarget);
        videoProps.onLoadedData?.(ev);
      }}
    />
  );
}
