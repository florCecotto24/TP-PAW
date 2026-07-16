import { useEffect, useState, type ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import CarouselHeader from './CarouselHeader';
import { chunk } from '../utils/chunk';

export interface CarouselSectionProps<T> {
  items: T[];
  title: string;
  subtitle: string;
  id: string;
  renderItem: (item: T, index: number) => ReactNode;
  itemsPerSlide?: number;
  emptyMessage?: string;
  showSlideControls?: boolean;
  /** API / URL page index (resets the active slide when it changes). */
  pageIndex?: number;
  /** Called when the user presses prev on the first slide (e.g. previous API page). */
  onPrevPage?: () => void;
  /** Called when the user presses next on the last slide (e.g. next API page). */
  onNextPage?: () => void;
  prevPageDisabled?: boolean;
  nextPageDisabled?: boolean;
}

/**
 * Header + paged slides (N items per slide).
 * Arrows move between slides; at the ends they optionally call {@code onPrevPage}/
 * {@code onNextPage}. Directions with nothing left to do are hidden.
 */
export default function CarouselSection<T>({
  items,
  title,
  subtitle,
  id,
  renderItem,
  itemsPerSlide = 4,
  emptyMessage,
  showSlideControls,
  pageIndex = 0,
  onPrevPage,
  onNextPage,
  prevPageDisabled = false,
  nextPageDisabled = false,
}: CarouselSectionProps<T>) {
  const { t } = useTranslation();
  const slides = chunk(items, itemsPerSlide);
  const slideCount = slides.length;
  const [activeSlide, setActiveSlide] = useState(0);

  useEffect(() => {
    setActiveSlide(0);
  }, [pageIndex, id, itemsPerSlide, items.length]);

  useEffect(() => {
    if (slideCount === 0) {
      setActiveSlide(0);
      return;
    }
    if (activeSlide > slideCount - 1) {
      setActiveSlide(slideCount - 1);
    }
  }, [activeSlide, slideCount]);

  const canPrevSlide = activeSlide > 0;
  const canNextSlide = activeSlide < slideCount - 1;
  const canPrevPage = Boolean(onPrevPage) && !prevPageDisabled;
  const canNextPage = Boolean(onNextPage) && !nextPageDisabled;
  const canPrev = canPrevSlide || canPrevPage;
  const canNext = canNextSlide || canNextPage;

  const handlePrev = () => {
    if (canPrevSlide) {
      setActiveSlide((s) => s - 1);
      return;
    }
    onPrevPage?.();
  };

  const handleNext = () => {
    if (canNextSlide) {
      setActiveSlide((s) => s + 1);
      return;
    }
    onNextPage?.();
  };

  const controlsVisible =
    showSlideControls ?? (slideCount > 1 || canPrevPage || canNextPage);

  return (
    <>
      <CarouselHeader
        title={title}
        subtitle={subtitle}
        id={id}
        showSlideControls={controlsVisible}
        onPrevPage={canPrev ? handlePrev : undefined}
        onNextPage={canNext ? handleNext : undefined}
        prevDisabled={!canPrev}
        nextDisabled={!canNext}
      />
      {items.length === 0 ? (
        <div className="alert-project" role="alert">
          {emptyMessage ?? t('carousel.noVehicles')}
        </div>
      ) : (
        <div
          id={id}
          className="carousel slide"
          data-bs-ride="false"
          data-bs-wrap="false"
          aria-roledescription="carousel"
        >
          <div className="carousel-inner">
            {slides.map((slideItems, slideIndex) => (
              <div
                key={slideIndex}
                className={`carousel-item${slideIndex === activeSlide ? ' active' : ''}`}
                aria-hidden={slideIndex !== activeSlide}
              >
                <div className="row row-cols-1 row-cols-md-2 row-cols-lg-4 g-3 pb-3">
                  {slideItems.map((item, itemIndex) => {
                    const globalIndex = slideIndex * itemsPerSlide + itemIndex;
                    return (
                      <div key={globalIndex} className="col d-flex justify-content-center">
                        {renderItem(item, globalIndex)}
                      </div>
                    );
                  })}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </>
  );
}
