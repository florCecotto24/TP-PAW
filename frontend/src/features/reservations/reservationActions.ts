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
  /** Puede reseñar (post-FINISHED). */
  canReview: boolean;
}

const TERMINAL = new Set([
  'cancelled',
  'cancelled_by_rider',
  'cancelled_by_owner',
  'cancelled_due_to_missing_payment_proof',
  'finished',
]);

/** Estados cancelados en los que aplica la obligación de reintegro (espeja el MVC). */
const REFUNDABLE_CANCELLED = new Set(['cancelled_by_rider', 'cancelled_by_owner']);

export function availableActions(
  reservation: ReservationDto,
  side: Side,
): ReservationActions {
  const status = reservation.status;
  const isPending = status === 'pending';
  const isTerminal = TERMINAL.has(status);
  const isFinished = status === 'finished';
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
    // Marcar devuelto: owner, una vez iniciada/aceptada y aún sin devolver.
    canMarkReturned:
      side === 'owner' &&
      !reservation.carReturned &&
      (status === 'accepted' || status === 'started'),
    canCancel: side !== 'none' && !isTerminal,
    canReview: side !== 'none' && isFinished,
  };
}

/** Status que produce una cancelación según el lado. */
export function cancelStatusFor(side: Side): 'cancelled_by_rider' | 'cancelled_by_owner' {
  return side === 'owner' ? 'cancelled_by_owner' : 'cancelled_by_rider';
}
