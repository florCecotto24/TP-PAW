import { useTranslation } from 'react-i18next';

export interface CarouselHeaderProps {
  title?: string;
  subtitle?: string;
  id?: string;
  showSlideControls?: boolean;
  onPrevPage?: () => void;
  onNextPage?: () => void;
  prevDisabled?: boolean;
  nextDisabled?: boolean;
}

/** Mirror of {@code ryden:carouselHeader}: title + Bootstrap slide prev/next on {@code id}. */
export default function CarouselHeader({
  title,
  subtitle,
  id: _carouselId = 'cheapestCarsCarousel',
  showSlideControls = true,
  onPrevPage,
  onNextPage,
  prevDisabled = false,
  nextDisabled = false,
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
            disabled={prevDisabled}
            onClick={onPrevPage}
            aria-label={t('carousel.prevPage')}
          >
            <i className="bi bi-chevron-left" aria-hidden="true"></i>
          </button>
          <button
            className="btn btn-sm btn-outline-secondary"
            type="button"
            disabled={nextDisabled}
            onClick={onNextPage}
            aria-label={t('carousel.nextPage')}
          >
            <i className="bi bi-chevron-right" aria-hidden="true"></i>
          </button>
        </div>
      ) : null}
    </div>
  );
}
