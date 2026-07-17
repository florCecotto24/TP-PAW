import { useTranslation } from 'react-i18next';
import CarCardImage from '../../../components/CarCardImage';
import { ConsumerCarCard } from '../../../components/ryden';
import { apiAssetUrl } from '../../../api/uri';
import { carDetailHref, carDtoToConsumerCard } from '../carCardAdapter';
import { useBrowseCarFavorite } from '../hooks';
import type { To } from 'react-router-dom';
import type { CarSummaryDto } from '../types';

/** Tarjeta de auto para home/búsqueda — delega en {@link ConsumerCarCard} (tag JSP). */
export default function BrowseCarCard({
  car,
  href,
  searchQuery,
}: {
  car: CarSummaryDto;
  href?: To | string | null;
  searchQuery?: Record<string, string>;
}) {
  const { t } = useTranslation();
  const card = carDtoToConsumerCard(car);
  const { favoritable, favorited, favoriteBusy, onToggleFavorite } = useBrowseCarFavorite(car);
  const resolvedHref = href ?? carDetailHref(car, searchQuery ? { ...searchQuery, src: 'search' } : undefined);
  const imageUrl = car.links.cover ? apiAssetUrl(car.links.cover) : null;

  return (
    <ConsumerCarCard
      card={{
        ...card,
        favoritable,
        favorited,
      }}
      href={resolvedHref}
      favoriteBusy={favoriteBusy}
      onToggleFavorite={
        favoritable
          ? () => {
              if (favoriteBusy) return;
              onToggleFavorite();
            }
          : undefined
      }
      image={imageUrl}
      imageSlot={
        <CarCardImage coverUri={car.links.cover}>
          {resolvedHref ? (
            <span className="carcard-view-chip" aria-hidden="true">
              {t('carCard.viewChip')}
            </span>
          ) : null}
        </CarCardImage>
      }
    />
  );
}
