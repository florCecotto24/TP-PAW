import { describe, expect, it } from 'vitest';
import { availableActions, cancelStatusFor, sideOf } from './reservationActions';
import type { ReservationDto, ReservationStatus } from './types';

function res(
  status: ReservationStatus,
  extra: Partial<ReservationDto> = {},
): ReservationDto {
  return {
    startDate: '2024-01-01T00:00:00Z',
    endDate: '2024-01-05T00:00:00Z',
    status,
    totalPrice: 100,
    carReturned: false,
    paymentRefundRequired: false,
    createdAt: '2023-12-01T00:00:00Z',
    links: {
      self: '/reservations/1',
      rider: '/users/10',
      owner: '/users/20',
    },
    ...extra,
  };
}

describe('sideOf', () => {
  it('detects the rider', () => {
    // 1.Arrange
    const reservation = res('pending');

    // 2.Act
    const role = sideOf(reservation, '10');

    // 3.Assert
    expect(role).toBe('rider');
  });

  it('detects the owner', () => {
    // 1.Arrange
    const reservation = res('pending');

    // 2.Act
    const role = sideOf(reservation, '20');

    // 3.Assert
    expect(role).toBe('owner');
  });

  it('returns none for a third party or anonymous', () => {
    // 1.Arrange
    const reservation = res('pending');

    // 2.Act
    const thirdParty = sideOf(reservation, '99');
    const anonymous = sideOf(reservation, null);

    // 3.Assert
    expect(thirdParty).toBe('none');
    expect(anonymous).toBe('none');
  });
});

describe('availableActions', () => {
  it('rider on PENDING can pay, edit dates and cancel', () => {
    // 1.Arrange
    const reservation = res('pending');

    // 2.Act
    const actions = availableActions(reservation, 'rider');

    // 3.Assert
    expect(actions.canUploadPayment).toBe(true);
    expect(actions.canEditDates).toBe(true);
    expect(actions.canCancel).toBe(true);
    expect(actions.canMarkReturned).toBe(false);
  });

  it('owner cannot pay nor edit dates', () => {
    // 1.Arrange
    const reservation = res('pending');

    // 2.Act
    const actions = availableActions(reservation, 'owner');

    // 3.Assert
    expect(actions.canUploadPayment).toBe(false);
    expect(actions.canEditDates).toBe(false);
  });

  it('owner can mark returned only while accepted/started and not yet returned', () => {
    // 1.Arrange
    const accepted = res('accepted');
    const started = res('started');
    const pending = res('pending');
    const alreadyReturned = res('accepted', { carReturned: true });

    // 2.Act
    const acceptedActions = availableActions(accepted, 'owner');
    const startedActions = availableActions(started, 'owner');
    const pendingActions = availableActions(pending, 'owner');
    const returnedActions = availableActions(alreadyReturned, 'owner');

    // 3.Assert
    expect(acceptedActions.canMarkReturned).toBe(true);
    expect(startedActions.canMarkReturned).toBe(true);
    expect(pendingActions.canMarkReturned).toBe(false);
    expect(returnedActions.canMarkReturned).toBe(false);
  });

  it('refund upload only when the server flagged paymentRefundRequired', () => {
    // 1.Arrange
    const withoutRefund = res('cancelled_by_owner');
    const withRefund = res('cancelled_by_owner', { paymentRefundRequired: true });

    // 2.Act
    const blocked = availableActions(withoutRefund, 'owner');
    const allowed = availableActions(withRefund, 'owner');

    // 3.Assert
    expect(blocked.canUploadRefund).toBe(false);
    expect(allowed.canUploadRefund).toBe(true);
  });

  it('cancel is blocked in terminal states', () => {
    // 1.Arrange
    const terminal: ReservationStatus[] = [
      'cancelled',
      'cancelled_by_rider',
      'cancelled_by_owner',
      'cancelled_due_to_missing_payment_proof',
      'finished',
    ];

    // 2.Act / 3.Assert
    for (const status of terminal) {
      expect(availableActions(res(status), 'rider').canCancel).toBe(false);
    }
  });

  it('review is available only after FINISHED to a participant', () => {
    // 1.Arrange
    const finished = res('finished');
    const accepted = res('accepted');

    // 2.Act
    const riderFinished = availableActions(finished, 'rider');
    const ownerFinished = availableActions(finished, 'owner');
    const outsiderFinished = availableActions(finished, 'none');
    const riderAccepted = availableActions(accepted, 'rider');

    // 3.Assert
    expect(riderFinished.canReview).toBe(true);
    expect(ownerFinished.canReview).toBe(true);
    expect(outsiderFinished.canReview).toBe(false);
    expect(riderAccepted.canReview).toBe(false);
  });
});

describe('cancelStatusFor', () => {
  it('maps the side to the matching cancellation status', () => {
    // 2.Act / 3.Assert
    expect(cancelStatusFor('owner')).toBe('cancelled_by_owner');
    expect(cancelStatusFor('rider')).toBe('cancelled_by_rider');
    expect(cancelStatusFor('none')).toBe('cancelled_by_rider');
  });
});
