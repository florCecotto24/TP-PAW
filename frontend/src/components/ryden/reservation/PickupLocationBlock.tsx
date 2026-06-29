import { useTranslation } from 'react-i18next';

export interface PickupLocationBlockProps {
  label?: string;
  address: string;
  mapImageSrc: string;
  mapLinkHref?: string | null;
}

/** Espejo de {@code ryden:pickupLocationBlock}. */
export default function PickupLocationBlock({
  label,
  address,
  mapImageSrc,
  mapLinkHref,
}: PickupLocationBlockProps) {
  const { t } = useTranslation();
  const resolvedLabel = label ?? t('pickupLocationBlock.defaultLabel');

  return (
    <div className="pickup-location-block mt-5">
      <p className="text-uppercase small text-secondary fw-semibold mb-2 letter-spacing-tight">
        {resolvedLabel}
      </p>
      <h3 className="h4 fw-bold mb-2 ryden-text-break">{address}</h3>
      {mapLinkHref ? (
        <a
          href={mapLinkHref}
          className="d-inline-flex align-items-center gap-1 text-decoration-none mb-3"
          target="_blank"
          rel="noopener noreferrer"
        >
          <i className="bi bi-map" aria-hidden="true"></i>
          {t('pickupLocationBlock.viewMap')}
        </a>
      ) : (
        <span className="d-inline-flex align-items-center gap-1 text-primary mb-3">
          <i className="bi bi-map" aria-hidden="true"></i>
          {t('pickupLocationBlock.viewMap')}
        </span>
      )}
      <div className="rounded-4 overflow-hidden border pickup-map-placeholder">
        <img src={mapImageSrc} className="w-100 d-block" alt={t('pickupLocationBlock.mapPreviewAlt')} />
      </div>
    </div>
  );
}
