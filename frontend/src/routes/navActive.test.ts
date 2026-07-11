import { describe, expect, it } from 'vitest';
import {
  isMyCarsNavActive,
  isOwnerReservationDetailPath,
  isOwnerReservationsNavActive,
  isRiderReservationsNavActive,
} from './navActive';

describe('navActive', () => {
  describe('isMyCarsNavActive', () => {
    it('testIsMyCarsNavActiveOnFleetList', () => {
      // 1.Arrange / 2.Act / 3.Assert
      expect(isMyCarsNavActive('/my-cars')).toBe(true);
    });

    it('testIsMyCarsNavActiveOnCarDetail', () => {
      // 1.Arrange / 2.Act / 3.Assert
      expect(isMyCarsNavActive('/my-cars/car/42')).toBe(true);
    });

    it('testIsMyCarsNavActiveFalseOnOwnerReservations', () => {
      // 1.Arrange / 2.Act / 3.Assert
      expect(isMyCarsNavActive('/my-cars/reservations')).toBe(false);
      expect(isMyCarsNavActive('/my-cars/reservations/7')).toBe(false);
    });
  });

  describe('isOwnerReservationsNavActive', () => {
    it('testIsOwnerReservationsNavActiveOnListAndCarFilter', () => {
      // 1.Arrange / 2.Act / 3.Assert
      expect(isOwnerReservationsNavActive('/my-cars/reservations', '')).toBe(true);
      expect(isOwnerReservationsNavActive('/my-cars/reservations/7', '')).toBe(true);
    });

    it('testIsOwnerReservationsNavActiveOnOwnerDetailQuery', () => {
      // 1.Arrange / 2.Act / 3.Assert
      expect(isOwnerReservationsNavActive('/my-reservations/9', '?role=OWNER')).toBe(true);
      expect(isOwnerReservationsNavActive('/my-reservations/9', '?fromCar=3')).toBe(true);
    });

    it('testIsOwnerReservationsNavActiveFalseOnFleetRoutes', () => {
      // 1.Arrange / 2.Act / 3.Assert
      expect(isOwnerReservationsNavActive('/my-cars', '')).toBe(false);
      expect(isOwnerReservationsNavActive('/my-cars/car/42', '')).toBe(false);
    });
  });

  describe('mutual exclusion', () => {
    it('testMyCarsAndOwnerReservationsAreMutuallyExclusive', () => {
      // 1.Arrange
      const cases = [
        '/my-cars',
        '/my-cars/car/1',
        '/my-cars/reservations',
        '/my-cars/reservations/1',
      ] as const;

      // 2.Act / 3.Assert
      for (const pathname of cases) {
        const myCars = isMyCarsNavActive(pathname);
        const owner = isOwnerReservationsNavActive(pathname, '');
        expect(myCars && owner).toBe(false);
      }
    });
  });

  describe('isOwnerReservationDetailPath', () => {
    it('testIsOwnerReservationDetailPathRequiresOwnerContext', () => {
      // 1.Arrange / 2.Act / 3.Assert
      expect(isOwnerReservationDetailPath('/my-reservations/9', '')).toBe(false);
      expect(isOwnerReservationDetailPath('/my-reservations/9', '?role=OWNER')).toBe(true);
      expect(isOwnerReservationDetailPath('/my-cars/reservations', '?role=OWNER')).toBe(false);
    });
  });

  describe('isRiderReservationsNavActive', () => {
    it('testIsRiderReservationsNavActiveOnRiderListAndDetail', () => {
      // 1.Arrange / 2.Act / 3.Assert
      expect(isRiderReservationsNavActive('/my-reservations', '')).toBe(true);
      expect(isRiderReservationsNavActive('/my-reservations/9', '')).toBe(true);
      expect(isRiderReservationsNavActive('/my-reservations/9', '?role=OWNER')).toBe(false);
    });
  });
});
