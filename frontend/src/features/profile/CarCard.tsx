import type { ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { ConsumerCarCard } from '../../components/ryden';
import CarCardImage from '../../components/CarCardImage';
import { useBrowseCarFavorite } from '../browse/hooks';
import { carDetailHref, carDtoToConsumerCard } from '../browse/carCardAdapter';
import type { CarDto } from './types';

/** Tarjeta de auto en favoritos/perfil público — delega en tag {@code carCard}. */
export default function CarCard({
  car,
  action,
}: {
  car: CarDto;
  action?: ReactNode;
}) {
  const { t } = useTranslation();
  const href = carDetailHref(car);
  const { favoritable, favorited, onToggleFavorite } = useBrowseCarFavorite(car);

  return (
    <ConsumerCarCard
      card={{
        ...carDtoToConsumerCard(car),
        favoritable,
        favorited,
      }}
      href={href}
      onToggleFavorite={favoritable ? () => onToggleFavorite() : undefined}
      overlay={action}
      imageSlot={
        <CarCardImage coverUri={car.links.cover}>
          {href ? (
            <span className="carcard-view-chip" aria-hidden="true">
              {t('carCard.viewChip')}
            </span>
          ) : null}
        </CarCardImage>
      }
    />
  );
}
