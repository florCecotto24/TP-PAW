import { describe, expect, it } from 'vitest';
import {
  isMyCarsNavActive,
  isOwnerReservationDetailPath,
  isOwnerReservationsNavActive,
  isRiderReservationsNavActive,
} from './navActive';

describe('navActive', () => {
  it('highlights rider reservations on list and rider detail', () => {
    expect(isRiderReservationsNavActive('/my-reservations', '')).toBe(true);
    expect(isRiderReservationsNavActive('/my-reservations/42', '')).toBe(true);
    expect(isRiderReservationsNavActive('/my-reservations/42/confirmation', '')).toBe(true);
  });

  it('does not highlight rider nav on owner reservation detail', () => {
    expect(isRiderReservationsNavActive('/my-reservations/42', 'role=OWNER&fromCar=7')).toBe(false);
    expect(isRiderReservationsNavActive('/my-reservations/42', 'fromCar=7')).toBe(false);
  });

  it('highlights owner reservations for owner detail and owner list', () => {
    expect(isOwnerReservationsNavActive('/my-cars/reservations', '')).toBe(true);
    expect(isOwnerReservationsNavActive('/my-cars/reservations/7', '')).toBe(true);
    expect(isOwnerReservationsNavActive('/my-reservations/42', 'role=OWNER&fromCar=7')).toBe(true);
  });

  it('highlights my cars when drilling into owner reservation detail', () => {
    expect(isMyCarsNavActive('/my-reservations/42', 'role=OWNER&fromCar=7')).toBe(true);
    expect(isMyCarsNavActive('/my-cars/reservations/7', '')).toBe(false);
  });

  it('detects owner reservation detail paths', () => {
    expect(isOwnerReservationDetailPath('/my-reservations/42', 'role=owner')).toBe(true);
    expect(isOwnerReservationDetailPath('/my-reservations/42', 'fromCar=1')).toBe(true);
    expect(isOwnerReservationDetailPath('/my-reservations/42', '')).toBe(false);
  });
});
