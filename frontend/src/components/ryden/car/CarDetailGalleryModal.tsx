import { useTranslation } from 'react-i18next';
import type { GalleryMediaItem } from './CarDetailGalleryGrid';

export interface CarDetailGalleryModalProps {
  modalId: string;
  carouselId: string;
  mediaItems: GalleryMediaItem[];
  vehicleLabel?: string;
  initialIndex?: number;
}

/** Espejo de {@code ryden-car:carDetailGalleryModal}: modal oscuro con carrusel de fotos/videos. */
export default function CarDetailGalleryModal({
  modalId,
  carouselId,
  mediaItems,
  vehicleLabel,
}: CarDetailGalleryModalProps) {
  const { t } = useTranslation();
  const label = vehicleLabel ?? t('carDetailGalleryModal.defaultVehicleLabel');

  return (
    <div
      className="modal fade car-detail-gallery-modal"
      id={modalId}
      tabIndex={-1}
      aria-hidden="true"
      aria-labelledby={`${modalId}Title`}
    >
      <div className="modal-dialog modal-dialog-centered modal-xl">
        <div className="modal-content bg-dark text-white border-0">
          <div className="modal-header border-0">
            <h2 className="modal-title fs-6" id={`${modalId}Title`}>
              {t('carDetailGalleryModal.title')}
            </h2>
            <button
              type="button"
              className="btn-close btn-close-white"
              data-bs-dismiss="modal"
              aria-label={t('common.close')}
            />
          </div>
          <div className="modal-body p-0">
            <div id={carouselId} className="carousel slide carousel-fade" data-bs-ride="false" data-bs-wrap="false">
              <div className="carousel-inner">
                {mediaItems.map((item, index) => (
                  <div key={index} className={`carousel-item${index === 0 ? ' active' : ''}`}>
                    <div className="car-detail-carousel-frame">
                      {item.video ? (
                        <>
                          <video
                            src={item.url}
                            className="car-detail-carousel-backdrop"
                            muted
                            playsInline
                            preload="metadata"
                            tabIndex={-1}
                            aria-hidden="true"
                          />
                          <video
                            src={item.url}
                            className="car-detail-carousel-video"
                            controls
                            playsInline
                            preload="metadata"
                            aria-label={t('carDetailGalleryModal.videoAlt', {
                              vehicle: label,
                              index: index + 1,
                            })}
                          >
                            {item.contentType ? (
                              <source src={item.url} type={item.contentType} />
                            ) : null}
                          </video>
                        </>
                      ) : (
                        <>
                          <img
                            src={item.url}
                            className="car-detail-carousel-backdrop"
                            alt=""
                            aria-hidden="true"
                          />
                          <img
                            src={item.url}
                            className="car-detail-carousel-img"
                            alt={t('carDetailGalleryModal.photoAlt', {
                              vehicle: label,
                              index: index + 1,
                            })}
                          />
                        </>
                      )}
                    </div>
                  </div>
                ))}
              </div>
              {mediaItems.length > 1 ? (
                <>
                  <button
                    className="carousel-control-prev"
                    type="button"
                    data-bs-target={`#${carouselId}`}
                    data-bs-slide="prev"
                  >
                    <span className="carousel-control-prev-icon" aria-hidden="true"></span>
                    <span className="visually-hidden">{t('common.previous')}</span>
                  </button>
                  <button
                    className="carousel-control-next"
                    type="button"
                    data-bs-target={`#${carouselId}`}
                    data-bs-slide="next"
                  >
                    <span className="carousel-control-next-icon" aria-hidden="true"></span>
                    <span className="visually-hidden">{t('common.next')}</span>
                  </button>
                </>
              ) : null}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
