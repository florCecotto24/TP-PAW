import { useTranslation } from 'react-i18next';

export interface CarouselHeaderProps {
  seeAllHref?: string;
  title?: string;
  subtitle?: string;
  id?: string;
  /** @deprecated Home pagination uses URL params; carousel arrows slide within the current page (RYDEN-APP). */
  prevPageHref?: string;
  /** @deprecated Home pagination uses URL params; carousel arrows slide within the current page (RYDEN-APP). */
  nextPageHref?: string;
  showSlideControls?: boolean;
}

/** Mirror of {@code ryden:carouselHeader}: title + Bootstrap slide prev/next on {@code id}. */
export default function CarouselHeader({
  title,
  subtitle,
  id = 'cheapestCarsCarousel',
  showSlideControls = true,
}: CarouselHeaderProps) {
  const { t } = useTranslation();
  const resolvedTitle = title ?? t('carousel.defaultTitle');
  const resolvedSubtitle = subtitle ?? t('carousel.defaultSubtitle');

  return (
    <div className="d-flex flex-wrap justify-content-between align-items-end gap-3 mb-4 carouselHeader">
      <div>
        <h4 className="fw-semibold mb-1">{resolvedTitle}</h4>
        <p className="text-secondary small mb-0">{resolvedSubtitle}</p>
      </div>
      {showSlideControls ? (
        <div className="d-flex gap-2 align-items-center">
          <button
            className="btn btn-sm btn-outline-secondary"
            type="button"
            data-bs-target={`#${id}`}
            data-bs-slide="prev"
            aria-label={t('carousel.prevPage')}
          >
            <i className="bi bi-chevron-left" aria-hidden="true"></i>
          </button>
          <button
            className="btn btn-sm btn-outline-secondary"
            type="button"
            data-bs-target={`#${id}`}
            data-bs-slide="next"
            aria-label={t('carousel.nextPage')}
          >
            <i className="bi bi-chevron-right" aria-hidden="true"></i>
          </button>
        </div>
      ) : null}
    </div>
  );
}
