// Tipos del área BROWSE, derivados de openapi.yaml.
// Todas las representaciones de lectura traen un bloque `links` (hipermedia):
// se navega por esos links, nunca se arman URLs a mano.

import type { Links } from '../../api/types';
import type { PriceMarketPosition } from '../../components/ryden/car/CarCard';

// --- Enums del dominio (openapi.yaml §components/schemas) ---------------------
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

export type CarSort = 'price_asc' | 'price_desc' | 'rating_desc' | 'recent' | 'name';

export const CAR_TYPES: readonly CarType[] = [
  'sedan',
  'hatchback',
  'suv',
  'coupe',
  'convertible',
  'wagon',
  'van',
  'pickup',
];

export const POWERTRAINS: readonly Powertrain[] = ['gasoline', 'diesel', 'electric', 'hybrid', 'cng'];

export const TRANSMISSIONS: readonly Transmission[] = ['manual', 'automatic', 'semi_automatic'];

export const CAR_SORTS: readonly CarSort[] = [
  'recent',
  'price_asc',
  'price_desc',
  'rating_desc',
  'name',
];

/**
 * Links de un CarDto. `self` siempre presente (heredado de Links); el resto
 * según openapi: self, owner, model, brand, pictures, availabilities, insurance,
 * reviews. Se tipan como opcionales porque no toda vista trae todos.
 */
export interface CarLinks {
  self: string;
  owner?: string;
  model?: string;
  brand?: string;
  pictures?: string;
  cover?: string;
  availabilities?: string;
  insurance?: string;
  reviews?: string;
  [rel: string]: string | undefined;
}

/** DTO de auto (lectura). No expone id propio; la identidad vive en links.self. */
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
  // Solo en listados (proyección CarCard); en el detalle es null (el precio vive en availabilities).
  dayPrice?: number | null;
  /** Badge de mercado en home/búsqueda/favoritos cuando hay datos comparables. */
  priceMarketPositionModifier?: PriceMarketPosition | null;
  marketAveragePrice?: number | null;
  marketSampleCount?: number | null;
  brandName: string;
  modelName: string;
  modelValidated: boolean;
  hasInsurance?: boolean;
  createdAt: string;
  links: CarLinks;
}

/** DTO de período de disponibilidad. links: self, car, neighborhood. */
export interface AvailabilityDto {
  startDate: string;
  endDate: string;
  dayPrice: number;
  startPointStreet: string;
  startPointNumber?: string | null;
  checkInTime: string;
  checkOutTime: string;
  kind: 'offered' | 'withdrawn';
  links: Links & { car?: string; neighborhood?: string };
}

/** DTO de imagen/video. Los bytes se sirven en links.self (binario, sin versionar). */
export interface PictureDto {
  displayOrder: number;
  kind: 'image' | 'video';
  contentType: string;
  links: Links & { car?: string };
}

/** DTO de reseña. links: self, reservation, car, author, image (si hay). */
export interface ReviewDto {
  rating?: number | null;
  comment?: string | null;
  madeByRider: boolean;
  createdAt: string;
  links: Links & {
    reservation?: string;
    car?: string;
    author?: string;
    image?: string;
  };
}

/** DTO de barrio (filtro de búsqueda). */
export interface NeighborhoodDto {
  name: string;
  links: Links;
}
