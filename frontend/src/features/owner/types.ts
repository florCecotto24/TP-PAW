// Tipos del área OWNER, derivados de openapi.yaml (schemas Car*, Availability*,
// Picture, Brand*, Model*, Neighborhood). Solo el subconjunto que consumen las
// páginas de esta feature. El core ya define `Links`/`PageLinks` en api/types.

import type { Links } from '../../api/types';

export type CarType =
  | 'sedan'
  | 'hatchback'
  | 'suv'
  | 'coupe'
  | 'convertible'
  | 'wagon'
  | 'van'
  | 'pickup';

export type Powertrain = 'gasoline' | 'diesel' | 'electric' | 'hybrid' | 'cng';

export type Transmission = 'manual' | 'automatic' | 'semi_automatic';

export type CarStatus =
  | 'active'
  | 'paused'
  | 'admin_paused'
  | 'lack_doc'
  | 'unavailable'
  | 'deactivated';

/**
 * Mapea cada estado del auto a una clase de badge Bootstrap (replica los colores
 * del JSP original: active=success, paused/lack_doc=warning, admin_paused=danger…).
 * Compartido por MyCarsPage y OwnerCarDetailPage para no duplicar (ni desincronizar)
 * las clases. Se usan los `text-dark` en los amarillos para mantener el contraste.
 */
export const STATUS_BADGE: Record<CarStatus, string> = {
  active: 'text-bg-success',
  paused: 'text-bg-warning text-dark',
  lack_doc: 'text-bg-warning text-dark',
  admin_paused: 'text-bg-danger',
  unavailable: 'text-bg-secondary',
  deactivated: 'text-bg-secondary',
};

/** Teaser de auto para colecciones (mis autos, favoritos). */
export interface CarSummaryDto {
  brandName: string;
  modelName: string;
  year?: number | null;
  status: CarStatus;
  minimumRentalDays: number;
  ratingAvg?: number | null;
  dayPrice?: number | null;
  modelValidated: boolean;
  links: Links;
}

/** DTO de auto (lectura completa). `links`: self, owner, model, brand, price-insight, cover, pictures, availabilities, bookable-segments, insurance, reviews, similar. */
export interface CarDto {
  plate: string;
  year?: number | null;
  powertrain: Powertrain;
  transmission: Transmission;
  type: CarType;
  status: CarStatus;
  description?: string | null;
  minimumRentalDays: number;
  ratingAvg?: number | null;
  brandName: string;
  modelName: string;
  modelValidated: boolean;
  hasInsurance?: boolean;
  createdAt: string;
  links: Links & {
    'price-insight'?: string;
    cover?: string;
    pictures?: string;
    availabilities: string;
    'bookable-segments'?: string;
    insurance?: string;
    reviews?: string;
    similar?: string;
    model?: string;
    brand?: string;
  };
}

/** Cuerpo del POST /cars (parte `car` del multipart, o JSON puro). */
export interface CarCreateDto {
  plate: string;
  year?: number;
  powertrain: Powertrain;
  transmission: Transmission;
  description?: string;
  minimumRentalDays?: number;
  // Catálogo: por nombre (crea/reusa) o por URN de modelo existente.
  brandName?: string;
  modelName?: string;
  type?: CarType;
  modelUri?: string;
}

/** Cuerpo del PATCH /cars/{id}: transición de estado y/o edición parcial. */
export interface CarPatchDto {
  status?: CarStatus;
  description?: string;
  minimumRentalDays?: number;
}

export type AvailabilityKind = 'offered' | 'withdrawn';

/**
 * Links de availability.
 * - Recurso persistido: `self` dereferenciable (PATCH/DELETE por id).
 * - Proyección mensual (`?month=`): sin `self`; mutaciones van por colección del auto
 *   (`car.links.availabilities` + `from`/`until`).
 */
export interface AvailabilityLinks {
  self?: string;
  car?: string;
  neighborhood?: string;
  [rel: string]: string | undefined;
}

/** DTO de período de disponibilidad. */
export interface AvailabilityDto {
  startDate: string;
  endDate: string;
  dayPrice: number;
  startPointStreet: string;
  startPointNumber?: string | null;
  checkInTime: string;
  checkOutTime: string;
  kind: AvailabilityKind;
  links: AvailabilityLinks;
}

/** Cuerpo del POST/PATCH availabilities. */
export interface AvailabilityCreateDto {
  startDate: string;
  endDate: string;
  dayPrice: number;
  startPointStreet: string;
  startPointNumber?: string;
  neighborhoodUri: string;
  checkInTime: string;
  checkOutTime: string;
}

export type PictureKind = 'image' | 'video';

/** Item de galería. `links`: self (bytes), car. */
export interface PictureDto {
  displayOrder: number;
  kind: PictureKind;
  contentType: string;
  links: Links;
}

/** Marca de catálogo. `links`: self, models. */
export interface BrandDto {
  name: string;
  validated: boolean;
  links: Links;
}

/** Modelo de catálogo. `links`: self, brand, price-insight. */
export interface ModelDto {
  name: string;
  type: CarType;
  validated: boolean;
  links: Links & { brand?: string; 'price-insight'?: string };
}

/** Barrio (select de ubicación en disponibilidad). `links`: self. */
export interface NeighborhoodDto {
  name: string;
  links: Links;
}
