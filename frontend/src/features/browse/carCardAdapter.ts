import { idFromUri } from '../../api/uri';
import { carDetail } from '../../routes/paths';
import type { CarDto } from './types';

export function carDtoToConsumerCard(car: CarDto) {
  const carIdRaw = idFromUri(car.links.self);
  const carId = carIdRaw != null ? Number(carIdRaw) : 0;
  return {
    carId: Number.isFinite(carId) ? carId : 0,
    model: car.modelName ?? '',
    brand: car.brandName ?? '',
    price: car.dayPrice ?? 0,
    ratingAvg: car.ratingAvg,
    minimumRentalDays: car.minimumRentalDays,
  };
}

export function carDetailHref(car: CarDto, extraQuery?: Record<string, string>): string | null {
  const carId = idFromUri(car.links.self);
  if (!carId) return null;
  return carDetail(carId, extraQuery ?? undefined);
}
