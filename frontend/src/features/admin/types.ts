// Tipos del área ADMIN. Reexportan/estrechan el contrato de la API
// (openapi.yaml) para las vistas de administración. Las representaciones que
// llegan a estas vistas son DTOs hypermedia: traen un bloque `links` con al
// menos `self` (URN canónica), que es de donde derivamos el "id" para PATCH /
// DELETE — nunca armamos URLs a mano (LINEAMIENTOS, navegación por links).

import type { Links } from '../../api/types';
import type { MessageAttachmentDto } from '../reservations/types';
import { lastPathSegment } from '../../api/uri';

export type { UserDto } from '../../api/types';

// --- Enums del dominio (subconjunto usado por las vistas admin) -------------

export type CarStatus =
  | 'active'
  | 'paused'
  | 'admin_paused'
  | 'lack_doc'
  | 'unavailable'
  | 'deactivated';

export type CarType =
  | 'sedan'
  | 'hatchback'
  | 'suv'
  | 'coupe'
  | 'convertible'
  | 'wagon'
  | 'van'
  | 'pickup';

export type ReservationStatus =
  | 'pending'
  | 'accepted'
  | 'started'
  | 'cancelled'
  | 'cancelled_by_rider'
  | 'cancelled_by_owner'
  | 'cancelled_due_to_missing_payment_proof'
  | 'finished';

// --- DTOs (lectura) ---------------------------------------------------------

export interface CarDto {
  plate: string;
  year?: number | null;
  powertrain?: string;
  transmission?: string;
  type?: CarType;
  status: CarStatus;
  description?: string | null;
  minimumRentalDays?: number;
  ratingAvg?: number | null;
  brandName: string;
  modelName: string;
  modelValidated?: boolean;
  createdAt?: string;
  links: Links;
}

export interface ReservationSummaryDto {
  startDate: string;
  endDate: string;
  status: ReservationStatus;
  totalPrice: number;
  brandName: string;
  modelName: string;
  links: Links;
}

export interface ReservationDto {
  startDate: string;
  endDate: string;
  status: ReservationStatus;
  totalPrice: number;
  carReturned?: boolean;
  createdAt?: string;
  links: Links;
}

/**
 * Mensaje del chat de una reserva (lectura). El admin lee el historial para
 * auditoría vía GET /reservations/{id}/messages. El nombre del emisor viene
 * inline (`senderForename` / `senderSurname`); `links.sender` sigue apuntando
 * a la URN canónica del usuario.
 */
export interface MessageDto {
  senderForename?: string;
  senderSurname?: string;
  body: string;
  createdAt?: string;
  seen?: boolean;
  hasAttachment?: boolean;
  attachment?: MessageAttachmentDto | null;
  links: Links; // self, reservation, sender, attachment?
}

export interface BrandDto {
  name: string;
  validated: boolean;
  links: Links; // self, models
}

export interface ModelDto {
  name: string;
  type?: CarType;
  brandName?: string;
  validated: boolean;
  links: Links; // self, brand, price-insight
}

/** Marca pendiente junto a sus modelos pendientes (vista de catálogo admin). */
export interface PendingBrandGroup {
  brand: BrandDto;
  models: ModelDto[];
}

// --- Helpers ----------------------------------------------------------------

/**
 * Deriva el id numérico de una URN `self` (p.ej. `/users/42` -> "42").
 * Tolerante a query/trailing slash. Devuelve "" si no hay match.
 *
 * Firma propia (recibe el bloque `links`, devuelve string no-nullable) por
 * conveniencia de las vistas admin; la extracción del segmento delega en el
 * helper compartido `api/uri#lastPathSegment`.
 */
export function idFromSelf(links: Links | undefined): string {
  const self = links?.self;
  if (!self) return '';
  return lastPathSegment(self);
}
