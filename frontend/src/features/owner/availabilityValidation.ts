import { getClientConfig } from '../../api/clientConfig';
import type { AvailabilityCreateDto } from './types';

export type AvailabilityValidationError =
  | 'datesRequired'
  | 'invalidDateRange'
  | 'priceInvalid'
  | 'priceBelowMin'
  | 'priceDigits'
  | 'streetRequired'
  | 'streetTooLong'
  | 'numberTooLong'
  | 'numberNotDigits'
  | 'neighborhoodRequired'
  | 'checkOutNotAfterCheckIn';

function listingLimits() {
  return getClientConfig().listing;
}

function priceDigitViolation(
  value: number,
  integerDigits: number,
  fractionDigits: number,
): boolean {
  if (!Number.isFinite(value)) return true;
  const [intPart, fracPart = ''] = value.toString().split('.');
  const normalizedInt = intPart.replace(/^-/, '').replace(/^0+(?=\d)/, '') || '0';
  const normalizedFrac = fracPart.replace(/0+$/, '');
  return normalizedInt.length > integerDigits || normalizedFrac.length > fractionDigits;
}

function compareTimes(checkIn: string, checkOut: string): boolean {
  return checkOut > checkIn;
}

/** Client-side mirror of {@code AvailabilityCreateForm} validators. */
export function firstAvailabilityValidationError(
  form: AvailabilityCreateDto,
): AvailabilityValidationError | null {
  const limits = listingLimits();

  if (!form.startDate || !form.endDate) return 'datesRequired';
  if (form.endDate < form.startDate) return 'invalidDateRange';

  const price = form.dayPrice;
  if (!Number.isFinite(price) || price <= 0) return 'priceInvalid';
  if (price < limits.pricePerDayMin) return 'priceBelowMin';
  if (
    priceDigitViolation(
      price,
      limits.pricePerDayIntegerDigits,
      limits.pricePerDayFractionDigits,
    )
  ) {
    return 'priceDigits';
  }

  const street = form.startPointStreet?.trim() ?? '';
  if (!street) return 'streetRequired';
  if (street.length > limits.addressStreetMaxLength) return 'streetTooLong';

  const number = form.startPointNumber?.trim() ?? '';
  if (number && !/^[0-9]*$/.test(number)) return 'numberNotDigits';
  if (number.length > limits.addressNumberMaxLength) return 'numberTooLong';

  if (!form.neighborhoodUri?.trim()) return 'neighborhoodRequired';

  if (form.checkInTime && form.checkOutTime && !compareTimes(form.checkInTime, form.checkOutTime)) {
    return 'checkOutNotAfterCheckIn';
  }

  return null;
}
