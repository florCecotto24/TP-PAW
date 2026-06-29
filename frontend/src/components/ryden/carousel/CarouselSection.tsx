import type { ReactNode } from 'react';
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
  prevPageHref?: string;
  nextPageHref?: string;
  showSlideControls?: boolean;
}

/**
 * Mirror of {@code ryden:carouselSection}: header + Bootstrap carousel with N items per slide.
 * {@code renderItem} replaces the nested {@code consumerCarCard} tag.
 */
export default function CarouselSection<T>({
  items,
  title,
  subtitle,
  id,
  renderItem,
  itemsPerSlide = 4,
  emptyMessage,
  prevPageHref,
  nextPageHref,
}: CarouselSectionProps<T>) {
  const { t } = useTranslation();
  const slides = chunk(items, itemsPerSlide);

  return (
    <>
      <CarouselHeader
        title={title}
        subtitle={subtitle}
        id={id}
        prevPageHref={prevPageHref}
        nextPageHref={nextPageHref}
        showSlideControls={slides.length > 1}
      />
      {items.length === 0 ? (
        <div className="alert-project" role="alert">
          {emptyMessage ?? t('carousel.noVehicles')}
        </div>
      ) : (
        <div id={id} className="carousel slide" data-bs-ride="false">
          <div className="carousel-inner">
            {slides.map((slideItems, slideIndex) => (
              <div
                key={slideIndex}
                className={`carousel-item${slideIndex === 0 ? ' active' : ''}`}
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
