import { useState, type ReactNode } from 'react';
import { apiAssetUrl } from '../api/uri';

/** Thumbnail de tarjeta de auto: usa {@code links.cover} cuando la API lo proyecta. */
export default function CarCardImage({
  coverUri,
  children,
}: {
  coverUri?: string | null;
  children?: ReactNode;
}) {
  const [loaded, setLoaded] = useState(false);
  const [failed, setFailed] = useState(false);
  const src = coverUri ? apiAssetUrl(coverUri) : null;
  const showPlaceholder = !src || failed;
  const loading = Boolean(src && !loaded && !failed);

  return (
    <div className={`carcard-image${loading ? ' img-loading' : ''}`}>
      {showPlaceholder ? (
        <div className="no-image-badge">
          <i className="bi bi-car-front" aria-hidden="true"></i>
        </div>
      ) : (
        <img
          src={src}
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
