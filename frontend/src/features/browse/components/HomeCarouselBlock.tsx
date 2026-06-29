import type { ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { CarouselSection } from '../../../components/ryden';
import BrowseCarCard from './BrowseCarCard';
import type { CarDto } from '../types';

export interface HomeCarouselBlockProps {
  sectionId: string;
  carouselId: string;
  sectionClassName?: string;
  title: string;
  subtitle: string;
  items: CarDto[];
  isLoading: boolean;
  isError: boolean;
  prevPageHref?: string;
  nextPageHref?: string;
  emptyMessage?: string;
}

/** Home carousel section: loading/error feedback + {@link CarouselSection} of {@link BrowseCarCard}s. */
export default function HomeCarouselBlock({
  sectionId,
  carouselId,
  sectionClassName = 'carouselSection animate-on-scroll',
  title,
  subtitle,
  items,
  isLoading,
  isError,
  prevPageHref,
  nextPageHref,
  emptyMessage,
}: HomeCarouselBlockProps) {
  const { t } = useTranslation();

  return (
    <section className={sectionClassName} id={sectionId}>
      {isLoading ? (
        <p className="text-secondary" role="status">
          {t('app.loading')}
        </p>
      ) : null}
      {isError ? (
        <div className="alert alert-danger" role="alert">
          {t('browse.home.loadError')}
        </div>
      ) : (
        <CarouselSection
          id={carouselId}
          items={items}
          title={title}
          subtitle={subtitle}
          emptyMessage={emptyMessage ?? t('carousel.noVehicles')}
          prevPageHref={prevPageHref}
          nextPageHref={nextPageHref}
          renderItem={(car: CarDto): ReactNode => <BrowseCarCard car={car} />}
        />
      )}
    </section>
  );
}
