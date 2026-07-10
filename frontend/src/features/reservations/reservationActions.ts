// Reglas de qué acciones puede ver/ejecutar cada lado según estado de la reserva.
// El server es la autoridad final (valida 403/409); esto es solo gating de UI.
import { idFromUri } from './api';
import type { ReservationDto } from './types';

export type Side = 'rider' | 'owner' | 'none';

/** Deriva el lado del usuario actual en la reserva comparando con links.rider/owner. */
export function sideOf(reservation: ReservationDto, currentUserId: string | null): Side {
  if (!currentUserId) return 'none';
  const riderId = idFromUri(reservation.links.rider);
  const ownerId = idFromUri(reservation.links.owner);
  if (riderId === currentUserId) return 'rider';
  if (ownerId === currentUserId) return 'owner';
  return 'none';
}

export interface ReservationActions {
  /** Rider puede subir comprobante de pago (PENDING). */
  canUploadPayment: boolean;
  /** Owner puede subir comprobante de reintegro. */
  canUploadRefund: boolean;
  /** Rider edita fechas: solo PENDING (impaga). */
  canEditDates: boolean;
  /** Owner marca el auto como devuelto. */
  canMarkReturned: boolean;
  /** Puede cancelar (rider u owner) en estados no terminales. */
  canCancel: boolean;
  /** Puede reseñar: rider tras endDate; owner solo tras carReturned (espejo JSP). */
  canReview: boolean;
}

const TERMINAL = new Set([
  'cancelled',
  'cancelled_by_rider',
  'cancelled_by_owner',
  'cancelled_due_to_missing_payment_proof',
  'finished',
]);

/** Días de gracia para reseñar (espejo de {@code app.reservation.review-auto-skip-days}). */
export const REVIEW_AUTO_SKIP_DAYS = 15;

/** Estados cancelados en los que aplica la obligación de reintegro (espeja el MVC). */
const REFUNDABLE_CANCELLED = new Set(['cancelled_by_rider', 'cancelled_by_owner']);

/** Espeja {@code ReservationViewServiceImpl}: {@code nowUtc.isAfter(endDate)}. */
export function reservationPeriodEnded(
  reservation: Pick<ReservationDto, 'endDate'>,
  now: Date = new Date(),
): boolean {
  const end = Date.parse(reservation.endDate);
  return Number.isFinite(end) && now.getTime() > end;
}

/** Rider: ventana abierta desde endDate hasta endDate + REVIEW_AUTO_SKIP_DAYS. */
export function isRiderReviewWindowOpen(
  reservation: Pick<ReservationDto, 'endDate'>,
  now: Date = new Date(),
  windowDays: number = REVIEW_AUTO_SKIP_DAYS,
): boolean {
  if (windowDays < 1) return true;
  const end = Date.parse(reservation.endDate);
  if (!Number.isFinite(end)) return false;
  const deadline = end + windowDays * 24 * 60 * 60 * 1000;
  return now.getTime() < deadline;
}

/**
 * Owner: ventana desde carReturnedAt (o endDate si falta) hasta + REVIEW_AUTO_SKIP_DAYS.
 */
export function isOwnerReviewWindowOpen(
  reservation: Pick<ReservationDto, 'endDate' | 'carReturnedAt'>,
  now: Date = new Date(),
  windowDays: number = REVIEW_AUTO_SKIP_DAYS,
): boolean {
  if (windowDays < 1) return true;
  const startMs = Date.parse(reservation.carReturnedAt ?? reservation.endDate);
  if (!Number.isFinite(startMs)) return false;
  const deadline = startMs + windowDays * 24 * 60 * 60 * 1000;
  return now.getTime() < deadline;
}

export function availableActions(
  reservation: ReservationDto,
  side: Side,
  now: Date = new Date(),
): ReservationActions {
  const status = reservation.status;
  const isPending = status === 'pending';
  const isTerminal = TERMINAL.has(status);
  const isFinished = status === 'finished';
  const periodEnded = reservationPeriodEnded(reservation, now);
  // Comprobante ya subido (flags del server; los links de descarga son condicionales).
  const hasPaymentReceipt = reservation.hasPaymentReceipt === true;
  const hasRefundReceipt = reservation.hasRefundReceipt === true;

  return {
    canUploadPayment: side === 'rider' && isPending && !hasPaymentReceipt,
    // El reintegro solo aplica si: el server marcó la obligación, la reserva
    // fue cancelada (no por falta de pago) y todavía no se subió el comprobante.
    // Espeja el guard del myReservationDetail.jsp original.
    canUploadRefund:
      side === 'owner' &&
      reservation.paymentRefundRequired &&
      REFUNDABLE_CANCELLED.has(status) &&
      !hasRefundReceipt,
    canEditDates: side === 'rider' && isPending,
    // JSP: owner && periodEnded && !carReturned. Server también exige
    // ACCEPTED|STARTED|FINISHED (incluye finished sin return para destrabar reseña).
    canMarkReturned:
      side === 'owner' &&
      !reservation.carReturned &&
      periodEnded &&
      (status === 'accepted' || status === 'started' || isFinished),
    canCancel: side !== 'none' && !isTerminal,
    // JSP / ReviewServiceImpl: rider tras endDate; owner solo tras carReturned;
    // ambos dentro de la ventana de review-auto-skip-days.
    canReview:
      (side === 'rider' && periodEnded && isRiderReviewWindowOpen(reservation, now)) ||
      (side === 'owner' &&
        reservation.carReturned === true &&
        isOwnerReviewWindowOpen(reservation, now)),
  };
}

/** Status que produce una cancelación según el lado. */
export function cancelStatusFor(side: Side): 'cancelled_by_rider' | 'cancelled_by_owner' {
  return side === 'owner' ? 'cancelled_by_owner' : 'cancelled_by_rider';
}
