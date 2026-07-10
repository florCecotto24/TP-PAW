import type { ReservationStatus, ReservationSummaryDto } from '../reservations/types';

export const CAR_RESERVATION_PREVIEW_LIMIT = 3;

const PREVIEW_STATUSES = new Set<ReservationStatus>(['pending', 'accepted', 'started']);

function isInProgress(reservation: ReservationSummaryDto, nowMs: number): boolean {
  if (reservation.status === 'started') return true;
  const start = Date.parse(reservation.startDate);
  const end = Date.parse(reservation.endDate);
  if (!Number.isFinite(start) || !Number.isFinite(end)) return false;
  return nowMs >= start && nowMs < end && reservation.status === 'accepted';
}

function previewSortKey(reservation: ReservationSummaryDto, nowMs: number): [number, number] {
  const start = Date.parse(reservation.startDate);
  const end = Date.parse(reservation.endDate);
  if (isInProgress(reservation, nowMs)) {
    return [0, Number.isFinite(end) ? end : start];
  }
  return [1, Number.isFinite(start) ? start : Number.MAX_SAFE_INTEGER];
}

/** Hasta 3 reservas activas del auto: en curso primero, luego las más próximas por fecha de inicio. */
export function pickCarReservationPreview(
  reservations: ReservationSummaryDto[],
  limit = CAR_RESERVATION_PREVIEW_LIMIT,
  now = new Date(),
): ReservationSummaryDto[] {
  const nowMs = now.getTime();
  return reservations
    .filter((r) => PREVIEW_STATUSES.has(r.status))
    .slice()
    .sort((a, b) => {
      const [ag, at] = previewSortKey(a, nowMs);
      const [bg, bt] = previewSortKey(b, nowMs);
      if (ag !== bg) return ag - bg;
      return at - bt;
    })
    .slice(0, limit);
}
