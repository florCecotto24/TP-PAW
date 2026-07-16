import { useTranslation } from 'react-i18next';
import CarouselNavPair from './CarouselNavPair';

export interface CarouselHeaderProps {
  title?: string;
  subtitle?: string;
  id?: string;
  showSlideControls?: boolean;
  onPrevPage?: () => void;
  onNextPage?: () => void;
  /** When true, prev is hidden but its slot stays reserved if next is shown. */
  prevDisabled?: boolean;
  /** When true, next is hidden but its slot stays reserved if prev is shown. */
  nextDisabled?: boolean;
}

/** Title + optional prev/next controls (fixed nav slots). */
export default function CarouselHeader({
  title,
  subtitle,
  showSlideControls = true,
  onPrevPage,
  onNextPage,
  prevDisabled = false,
  nextDisabled = false,
}: CarouselHeaderProps) {
  const { t } = useTranslation();
  const resolvedTitle = title ?? t('carousel.defaultTitle');
  const resolvedSubtitle = subtitle ?? t('carousel.defaultSubtitle');
  const showPrev = showSlideControls && !prevDisabled && Boolean(onPrevPage);
  const showNext = showSlideControls && !nextDisabled && Boolean(onNextPage);

  return (
    <div className="d-flex flex-wrap justify-content-between align-items-end gap-3 mb-4 carouselHeader">
      <div>
        <h4 className="fw-semibold mb-1">{resolvedTitle}</h4>
        <p className="text-secondary small mb-0">{resolvedSubtitle}</p>
      </div>
      <CarouselNavPair
        showPrev={showPrev}
        showNext={showNext}
        onPrev={onPrevPage}
        onNext={onNextPage}
        prevLabel={t('carousel.prevPage')}
        nextLabel={t('carousel.nextPage')}
      />
    </div>
  );
}
