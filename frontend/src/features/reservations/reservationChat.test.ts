import { describe, expect, it } from 'vitest';
import { CHAT_GRACE_DAYS_AFTER_FINISHED, isChatAvailable } from './reservationChat';
import type { ReservationDto } from './types';

function reservation(overrides: Partial<ReservationDto>): ReservationDto {
  return {
    startDate: '2026-01-01T10:00:00-03:00',
    endDate: '2026-01-05T20:00:00-03:00',
    status: 'accepted',
    totalPrice: 1000,
    carReturned: false,
    paymentRefundRequired: false,
    createdAt: '2025-12-20T10:00:00-03:00',
    links: { self: '/reservations/1' },
    ...overrides,
  };
}

describe('isChatAvailable', () => {
  it('returns false for pending reservations', () => {
    expect(isChatAvailable(reservation({ status: 'pending' }))).toBe(false);
  });

  it('returns true for accepted reservations', () => {
    expect(isChatAvailable(reservation({ status: 'accepted' }))).toBe(true);
  });

  it('returns false for finished reservations after grace period', () => {
    const end = new Date();
    end.setDate(end.getDate() - (CHAT_GRACE_DAYS_AFTER_FINISHED + 1));
    expect(
      isChatAvailable(
        reservation({ status: 'finished', endDate: end.toISOString() }),
        new Date(),
      ),
    ).toBe(false);
  });
});
