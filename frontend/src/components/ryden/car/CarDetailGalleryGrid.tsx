import { useTranslation } from 'react-i18next';

export interface GalleryMediaItem {
  url: string;
  video?: boolean;
  contentType?: string;
}

export interface CarDetailGalleryGridProps {
  mediaItems: GalleryMediaItem[];
  modalId: string;
  vehicleLabel?: string;
  onOpenAtIndex?: (index: number) => void;
}

function GalleryCell({
  item,
  src,
  vehicleLabel,
  modalId,
  index,
  className,
  extraOverlay,
  onOpenAtIndex,
}: {
  item: GalleryMediaItem;
  src: string;
  vehicleLabel: string;
  modalId: string;
  index: number;
  className: string;
  extraOverlay?: React.ReactNode;
  onOpenAtIndex?: (index: number) => void;
}) {
  const openProps = {
    type: 'button' as const,
    'data-bs-toggle': 'modal',
    'data-bs-target': `#${modalId}`,
    'data-carousel-index': index,
    onClick: () => onOpenAtIndex?.(index),
  };

  if (item.video) {
    return (
      <div className={className}>
        <video src={src} className="car-detail-gallery__video" muted playsInline preload="metadata" />
        <button
          {...openProps}
          className="car-detail-gallery__open-btn btn p-0 border-0 bg-transparent text-start"
          aria-label={vehicleLabel}
        >
          <span className="car-detail-gallery__play-overlay" aria-hidden="true">
            <i className="bi bi-play-circle"></i>
          </span>
        </button>
        {extraOverlay}
      </div>
    );
  }

  return (
    <button
      {...openProps}
      className={`${className} btn p-0 border-0 bg-transparent w-100 h-100 text-start position-relative`}
      aria-label={vehicleLabel}
    >
      <img src={src} className="car-detail-gallery__img" alt={vehicleLabel} />
      {extraOverlay}
    </button>
  );
}

/** Espejo de {@code ryden-car:carDetailGalleryGrid}. */
export default function CarDetailGalleryGrid({
  mediaItems,
  modalId,
  vehicleLabel,
  onOpenAtIndex,
}: CarDetailGalleryGridProps) {
  const { t } = useTranslation();
  const label = vehicleLabel ?? t('carDetailGallery.defaultVehicleLabel');
  const count = mediaItems.length;

  if (count === 0) {
    return (
      <div
        className="car-detail-gallery car-detail-gallery--empty rounded-4 overflow-hidden d-flex align-items-center justify-content-center bg-secondary-subtle"
        style={{ minHeight: 280 }}
      >
        <div className="text-center text-secondary px-3">
          <i className="bi bi-image fs-1 d-block mb-2" aria-hidden="true"></i>
          <p className="mb-0 small">{t('carDetailGallery.noPhotos')}</p>
        </div>
      </div>
    );
  }

  const openAria = t('carDetailGallery.openGallery', { vehicle: label });

  if (count === 1) {
    return (
      <div className="car-detail-gallery car-detail-gallery--single rounded-4 overflow-hidden">
        <GalleryCell
          item={mediaItems[0]}
          src={mediaItems[0].url}
          vehicleLabel={openAria}
          modalId={modalId}
          index={0}
          className="car-detail-gallery__cell car-detail-gallery__main"
          onOpenAtIndex={onOpenAtIndex}
        />
      </div>
    );
  }

  if (count === 2) {
    return (
      <div className="car-detail-gallery car-detail-gallery--pair rounded-4 overflow-hidden">
        <GalleryCell
          item={mediaItems[0]}
          src={mediaItems[0].url}
          vehicleLabel={openAria}
          modalId={modalId}
          index={0}
          className="car-detail-gallery__cell car-detail-gallery__main"
          onOpenAtIndex={onOpenAtIndex}
        />
        <GalleryCell
          item={mediaItems[1]}
          src={mediaItems[1].url}
          vehicleLabel={openAria}
          modalId={modalId}
          index={1}
          className="car-detail-gallery__cell car-detail-gallery__side"
          onOpenAtIndex={onOpenAtIndex}
        />
      </div>
    );
  }

  const extra = count - 3;
  return (
    <div className="car-detail-gallery rounded-4 overflow-hidden">
      <GalleryCell
        item={mediaItems[0]}
        src={mediaItems[0].url}
        vehicleLabel={openAria}
        modalId={modalId}
        index={0}
        className="car-detail-gallery__cell car-detail-gallery__main"
        onOpenAtIndex={onOpenAtIndex}
      />
      <GalleryCell
        item={mediaItems[1]}
        src={mediaItems[1].url}
        vehicleLabel={openAria}
        modalId={modalId}
        index={1}
        className="car-detail-gallery__cell car-detail-gallery__side"
        onOpenAtIndex={onOpenAtIndex}
      />
      <GalleryCell
        item={mediaItems[2]}
        src={mediaItems[2].url}
        vehicleLabel={openAria}
        modalId={modalId}
        index={2}
        className="car-detail-gallery__cell car-detail-gallery__side"
        onOpenAtIndex={onOpenAtIndex}
        extraOverlay={
          extra > 0 ? (
            <div className="car-detail-gallery__more-overlay" aria-hidden="true">
              +{extra}
            </div>
          ) : undefined
        }
      />
    </div>
  );
}
