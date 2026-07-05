import type { ReservationDto } from './types';

/** Alineado con `application.properties` → `app.reservation.chat.grace-days-after-finished`. */
export const CHAT_GRACE_DAYS_AFTER_FINISHED = 7;

const CHAT_STATUSES = new Set(['accepted', 'started', 'finished']);

/**
 * Réplica de {@link ar.edu.itba.paw.policy.ReservationChatPolicy#isChatAvailable}.
 * El chat solo está activo en ACCEPTED/STARTED y FINISHED dentro del período de gracia.
 */
export function isChatAvailable(reservation: ReservationDto, now = new Date()): boolean {
  const status = reservation.status;
  if (!CHAT_STATUSES.has(status)) return false;
  if (status !== 'finished') return true;
  const end = new Date(reservation.endDate);
  if (Number.isNaN(end.getTime())) return false;
  const graceEnd = new Date(end);
  graceEnd.setDate(graceEnd.getDate() + CHAT_GRACE_DAYS_AFTER_FINISHED);
  return now < graceEnd;
}
