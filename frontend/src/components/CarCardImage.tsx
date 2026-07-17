import { useEffect, useRef, useState, type ReactNode } from 'react';
import { apiAssetUrl } from '../api/uri';
import AuthenticatedImg from './ryden/media/AuthenticatedImg';

/** Thumbnail de tarjeta de auto: usa {@code links.cover} cuando la API lo proyecta. */
export default function CarCardImage({
  coverUri,
  authenticated = false,
  children,
}: {
  coverUri?: string | null;
  /** When true, load cover with Bearer (non-public / paused listings). */
  authenticated?: boolean;
  children?: ReactNode;
}) {
  const [loaded, setLoaded] = useState(false);
  const [failed, setFailed] = useState(false);
  const imgRef = useRef<HTMLImageElement | null>(null);
  const publicSrc = coverUri && !authenticated ? apiAssetUrl(coverUri) : null;
  const authSrc = coverUri && authenticated ? coverUri : null;

  // Cached images often fire {@code load} before React attaches onLoad — mark them loaded.
  useEffect(() => {
    setLoaded(false);
    setFailed(false);
    const img = imgRef.current;
    if (img?.complete && img.naturalWidth > 0) {
      setLoaded(true);
    }
  }, [publicSrc]);

  const placeholder = (
    <div className="no-image-badge">
      <i className="bi bi-car-front" aria-hidden="true"></i>
    </div>
  );

  if (authenticated) {
    return (
      <>
        {authSrc ? (
          <AuthenticatedImg
            src={authSrc}
            alt=""
            className="img-loaded"
            fallback={placeholder}
          />
        ) : (
          placeholder
        )}
        {children}
      </>
    );
  }

  const showPlaceholder = !publicSrc || failed;
  const loading = Boolean(publicSrc && !loaded && !failed);

  // Fill the parent {@code .carcard-image} shell from CarCard — do not nest another
  // {@code .carcard-image} (breaks fixed height / opacity rules).
  return (
    <div className={`carcard-thumb${loading ? ' img-loading' : ''}`}>
      {showPlaceholder ? (
        placeholder
      ) : (
        <img
          ref={imgRef}
          src={publicSrc!}
          alt=""
          className={loaded ? 'img-loaded' : 'img-loading-state'}
          onLoad={() => setLoaded(true)}
          onError={() => setFailed(true)}
        />
      )}
      {children}
    </div>
  );
}
