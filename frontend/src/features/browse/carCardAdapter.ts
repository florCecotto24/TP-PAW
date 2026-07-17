import { idFromUri, lastPathSegment } from '../../api/uri';
import { carDetailTo, type AppLinkTarget } from '../../routes/navigationState';
import type { CarSummaryDto } from './types';

export function isCarOwnedByUser(car: CarSummaryDto, userSelf: string | null | undefined): boolean {
  if (!userSelf || !car.links?.owner) return false;
  return lastPathSegment(car.links.owner) === lastPathSegment(userSelf);
}

export function isCarFavoritable(
  car: CarSummaryDto,
  isLoggedIn: boolean,
  userSelf: string | null | undefined,
): boolean {
  if (!isLoggedIn || !userSelf || !car.links?.owner) return false;
  return !isCarOwnedByUser(car, userSelf);
}

export function carDtoToConsumerCard(car: CarSummaryDto) {
  const carIdRaw = idFromUri(car.links?.self);
  const carId = carIdRaw != null ? Number(carIdRaw) : 0;
  return {
    carId: Number.isFinite(carId) ? carId : 0,
    model: car.modelName ?? '',
    brand: car.brandName ?? '',
    year: car.year ?? null,
    price: car.dayPrice ?? 0,
    ratingAvg: car.ratingAvg,
    minimumRentalDays: car.minimumRentalDays,
    priceMarketPositionModifier: car.priceMarketPositionModifier ?? undefined,
    marketAveragePrice: car.marketAveragePrice ?? undefined,
    marketSampleCount: car.marketSampleCount ?? undefined,
  };
}

export function carDetailHref(car: CarSummaryDto, extraQuery?: Record<string, string>): AppLinkTarget | null {
  const carSelf = car.links?.self;
  const carId = idFromUri(carSelf);
  if (!carId) return null;
  return carDetailTo(carId, carSelf, extraQuery);
}
