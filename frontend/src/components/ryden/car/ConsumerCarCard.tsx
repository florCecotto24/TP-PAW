import CarCard, { type CarCardProps } from './CarCard';
import type { To } from 'react-router-dom';
import type { AppLinkTarget } from '../../../routes/navigationState';

/** Datos mínimos de tarjeta de consumidor para grids de browse. */
export interface ConsumerCarCardData {
  carId: number;
  model: string;
  brand: string;
  price: number;
  ratingAvg?: number | null;
  reviewCount?: number | null;
  priceMarketPositionModifier?: CarCardProps['priceMarketPositionModifier'];
  marketAveragePrice?: number | null;
  marketSampleCount?: number | null;
  minimumRentalDays?: number | null;
  favoritable?: boolean;
  favorited?: boolean;
}

export interface ConsumerCarCardProps {
  card: ConsumerCarCardData;
  image?: string | null;
  href?: To | AppLinkTarget | null;
  reviewCount?: number | null;
  onToggleFavorite?: (carId: number) => void;
  imageSlot?: CarCardProps['imageSlot'];
  overlay?: CarCardProps['overlay'];
}

/** Espejo de {@code ryden-car:consumerCarCard}: envuelve {@link CarCard} con periodo día. */
export default function ConsumerCarCard({
  card,
  image,
  href,
  reviewCount,
  onToggleFavorite,
  imageSlot,
  overlay,
}: ConsumerCarCardProps) {
  return (
    <CarCard
      model={card.model}
      brand={card.brand}
      price={card.price}
      image={image}
      pricePeriod="day"
      ratingAvg={card.ratingAvg}
      reviewCount={reviewCount ?? card.reviewCount}
      href={href}
      priceMarketPositionModifier={card.priceMarketPositionModifier}
      marketAveragePrice={card.marketAveragePrice}
      marketSampleCount={card.marketSampleCount}
      minimumRentalDays={card.minimumRentalDays}
      carId={card.carId}
      showFavoriteButton={card.favoritable}
      favorited={card.favorited}
      onToggleFavorite={onToggleFavorite}
      imageSlot={imageSlot}
      overlay={overlay}
    />
  );
}
