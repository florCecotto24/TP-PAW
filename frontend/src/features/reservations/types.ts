// Tipos del contrato de la API para el área RESERVATIONS (derivados de openapi.yaml).
// La API expone DTOs hypermedia: nunca id propio, navegación por `links`.
import type { Links } from '../../api/types';

/** Estados posibles de una reserva (openapi: ReservationStatus). */
export type ReservationStatus =
  | 'pending'
  | 'accepted'
  | 'started'
  | 'cancelled'
  | 'cancelled_by_rider'
  | 'cancelled_by_owner'
  | 'cancelled_due_to_missing_payment_proof'
  | 'finished';

/**
 * Teaser de reserva para colecciones (mis reservas, admin listado).
 */
export interface ReservationSummaryDto {
  startDate: string;
  endDate: string;
  status: ReservationStatus;
  totalPrice: number;
  brandName: string;
  modelName: string;
  links: Links;
}

/**
 * Reserva (lectura completa). `links` trae: self, car, rider, owner, counterparty, messages,
 * reviews, payment-receipt, refund-receipt. Se navegan, no se arman a mano.
 */
export interface ReservationDto {
  startDate: string; // date-time UTC
  endDate: string; // date-time UTC
  status: ReservationStatus;
  totalPrice: number;
  carReturned: boolean;
  /** Momento de devolución (UTC); inicio de la ventana de reseña del owner. */
  carReturnedAt?: string | null;
  paymentProofDeadlineAt?: string | null;
  refundProofDeadlineAt?: string | null;
  paymentRefundRequired: boolean;
  hasPaymentReceipt?: boolean;
  hasRefundReceipt?: boolean;
  createdAt: string;
  // CBU del dueño para que el inquilino transfiera (útil sólo en PENDING). Nullable.
  ownerCbu?: string | null;
  // Ubicación de retiro + horarios, resueltos de la disponibilidad que cubre el inicio. Nullable.
  pickupStreet?: string | null;
  pickupNumber?: string | null;
  pickupNeighborhood?: string | null;
  checkInTime?: string | null; // "HH:mm"
  checkOutTime?: string | null; // "HH:mm"
  links: Links;
}

/** Cuerpo para crear una reserva (POST /reservations). */
export interface ReservationCreateDto {
  carUri: string;
  availabilityUri: string;
  startDate: string;
  endDate: string;
}

/**
 * Cuerpo de transición de estado (PATCH /reservations/{id}).
 * Absorbe cancelar, marcar devuelto y editar fechas de una PENDING impaga.
 */
export interface ReservationPatchDto {
  status?: ReservationStatus;
  carReturned?: boolean;
  startDate?: string;
  endDate?: string;
}

/**
 * Mensaje de chat de una reserva. `links`: self, reservation, sender,
 * attachment (si hay).
 */
export interface MessageAttachmentDto {
  fileName: string;
  contentType: string;
  sizeBytes: number;
  kind: 'IMAGE' | 'PDF' | 'DOCUMENT' | 'VIDEO' | 'GENERIC';
}

/**
 * Mensaje de chat de una reserva. `links`: self, reservation, sender,
 * attachment (si hay).
 */
export interface MessageDto {
  body: string;
  createdAt: string;
  seen: boolean;
  hasAttachment: boolean;
  attachment?: MessageAttachmentDto | null;
  links: Links;
}

/**
 * Reseña de una reserva (entidad débil: PK = reserva + madeByRider).
 * `links`: self, reservation, car, author, image (si hay).
 */
export interface ReviewDto {
  rating?: number | null;
  comment?: string | null;
  madeByRider: boolean;
  createdAt: string;
  links: Links;
}
