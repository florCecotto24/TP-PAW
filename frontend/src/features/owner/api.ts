// Adaptador de la API para el área OWNER. Centraliza llamadas y MIME types para
// que las páginas queden delgadas. Sigue el contrato (vendor MIME, hipervínculos,
// paginación por header). Reusa el cliente del core (sessionClient).

import { sessionClient } from '../../session/sessionStore';
import { getCollectionPath } from '../../api/apiDiscovery';
import { openAuthenticatedBinary } from '../../api/openAuthenticatedBinary';
import { MediaTypes } from '../../api/mediaTypes';
import type { ApiResponse } from '../../api/client';
import { followAllPages } from '../../api/client';
import { encodeMultipart } from '../../api/multipart';
import type { UserDto } from '../../api/types';
import { idFromUri } from '../../api/uri';
import type {
  AvailabilityCreateDto,
  AvailabilityDto,
  BrandDto,
  CarCreateDto,
  CarDto,
  CarSummaryDto,
  CarPatchDto,
  CarStatus,
  ModelDto,
  NeighborhoodDto,
  PictureDto,
} from './types';

// Las URN de los DTOs son del tipo "/cars/42". Las rutas del SPA usan :id, así
// que necesitamos pasar de URN -> id y de id -> URN (sin armar URLs "a mano":
// reconstruimos la MISMA URN canónica que la API ya nos dio en `links.self`).
// idFromUri vive en api/uri (helper compartido); se re-exporta para no tocar
// los call sites que lo importan desde aquí.
export { idFromUri };

export function carUri(id: string): string {
  return `/cars/${id}`;
}

// ---- Publisher / prerequisitos (identidad + CBU) ----
// El viejo flujo exigía, ANTES de publicar, tener (a) identidad cargada y (b) CBU.
// Replicamos esos dos prerequisitos contra el recurso user.

/** Deriva un sub-recurso del usuario a partir de su URN canónica ("/users/{id}"). */
function userSubResource(userUri: string, suffix: string): string {
  const base = userUri.endsWith('/') ? userUri.slice(0, -1) : userUri;
  return `${base}/${suffix}`;
}

/** GET /users/{id} → UserDto (para releer identidad/cbu tras cargarlos). Es el usuario propio
 *  (publisher), así que pide la vista PRIVADA: necesita el cbu, que no viaja en la vista pública. */
export function fetchUser(userUri: string): Promise<ApiResponse<UserDto>> {
  return sessionClient.get<UserDto>(userUri, { accept: MediaTypes.userPrivate });
}

/** PATCH /users/{id} con { cbu } (carga del CBU del publisher; respuesta en vista privada). */
export function patchCbu(userUri: string, cbu: string): Promise<ApiResponse<UserDto>> {
  return sessionClient.patch<UserDto>(userUri, { cbu }, {
    accept: MediaTypes.userPrivate,
    contentType: MediaTypes.user,
  });
}

/**
 * PUT /users/{id}/documents/identity (octet-stream): sube el documento de identidad.
 * Mismo mecanismo que usa el área PROFILE y reservas. Tras subirlo, la identidad
 * queda "en revisión" (identityValidated lo fija el admin), pero el documento ya
 * está en el sistema y el prerequisito de publicación se considera cumplido.
 */
export function uploadIdentityDocument(
  user: UserDto,
  file: File,
): Promise<ApiResponse<unknown>> {
  const base = user.links.documents ?? userSubResource(user.links.self, 'documents');
  const normalized = base.endsWith('/') ? base.slice(0, -1) : base;
  return sessionClient.request(`${normalized}/identity`, {
    method: 'PUT',
    body: file,
    contentType: file.type || 'application/octet-stream',
  });
}

// ---- Catálogo ----
/**
 * Todas las marcas del catálogo. {@code GET /brands} pagina (máx. 100); hay ~190+
 * en la DB local, así que se siguen los {@code Link: next} hasta completar.
 */
export async function fetchBrands(): Promise<ApiResponse<BrandDto[]>> {
  const first = await sessionClient.get<BrandDto[]>(getCollectionPath('brands'), {
    accept: MediaTypes.brand,
    query: { page: 1, pageSize: 100 },
  });
  if (!first.page.next) {
    return { ...first, data: first.data ?? [] };
  }
  const rest = await followAllPages<BrandDto>(sessionClient, first.page.next, {
    accept: MediaTypes.brand,
  });
  return {
    ...rest,
    data: [...(first.data ?? []), ...(rest.data ?? [])],
  };
}

/** Modelos de una marca: se navega su link `models` (hipervínculo, no URL armada). */
export function fetchModels(brand: BrandDto): Promise<ApiResponse<ModelDto[]>> {
  return sessionClient.follow<ModelDto[]>(brand.links.models, {
    accept: MediaTypes.model,
  });
}

export function createBrand(name: string): Promise<ApiResponse<BrandDto>> {
  return sessionClient.post<BrandDto>(getCollectionPath('brands'), { name }, {
    accept: MediaTypes.brand,
    contentType: MediaTypes.brand,
  });
}

export function createModel(
  brand: BrandDto,
  name: string,
  type: string,
): Promise<ApiResponse<ModelDto>> {
  return sessionClient.request<ModelDto>(brand.links.models, {
    method: 'POST',
    accept: MediaTypes.model,
    contentType: MediaTypes.model,
    body: { name, type },
  });
}

// ---- Cars ----
export interface OwnerCarsQuery {
  page?: number;
  pageSize?: number;
  q?: string;
  status?: CarStatus[];
  category?: string[];
  transmission?: string[];
  powertrain?: string[];
  priceMin?: number;
  priceMax?: number;
  rating?: string[];
  sort?: string;
}

export function fetchOwnerCars(
  ownerId: string,
  opts: OwnerCarsQuery = {},
): Promise<ApiResponse<CarSummaryDto[]>> {
  return sessionClient.get<CarSummaryDto[]>(getCollectionPath('cars'), {
    accept: MediaTypes.carSummary,
    query: {
      ownerId,
      page: opts.page ?? 1,
      pageSize: opts.pageSize ?? 12,
      q: opts.q || undefined,
      status: opts.status?.length ? opts.status : undefined,
      category: opts.category?.length ? opts.category : undefined,
      transmission: opts.transmission?.length ? opts.transmission : undefined,
      powertrain: opts.powertrain?.length ? opts.powertrain : undefined,
      priceMin: opts.priceMin != null ? String(opts.priceMin) : undefined,
      priceMax: opts.priceMax != null ? String(opts.priceMax) : undefined,
      rating: opts.rating?.length ? opts.rating : undefined,
      sort: opts.sort || undefined,
    },
  });
}

export function fetchCar(id: string): Promise<ApiResponse<CarDto>> {
  return sessionClient.get<CarDto>(carUri(id), { accept: MediaTypes.car });
}

/**
 * POST /cars como multipart/form-data: parte `car` (JSON, sin filename),
 * `pictures` e `insurance` opcional. El body se arma a mano con boundary
 * explícito: si el browser manda `multipart/form-data` sin boundary, Jersey
 * responde 400 genérico ({@code BadRequestException} / ~68 bytes).
 */
export async function publishCar(
  car: CarCreateDto,
  pictures: File[],
  insurance: File | null,
): Promise<ApiResponse<CarDto>> {
  const parts = [
    {
      name: 'car',
      value: JSON.stringify(car),
      contentType: MediaTypes.car,
    },
    ...pictures.map((pic) => ({
      name: 'pictures',
      value: pic,
      filename: pic.name || 'picture',
      contentType: pic.type || 'application/octet-stream',
    })),
  ];
  if (insurance) {
    parts.push({
      name: 'insurance',
      value: insurance,
      filename: insurance.name || 'insurance',
      contentType: insurance.type || 'application/octet-stream',
    });
  }

  const { body, contentType } = await encodeMultipart(parts);
  return sessionClient.post<CarDto>(getCollectionPath('cars'), body, {
    accept: MediaTypes.car,
    contentType,
  });
}

export function fetchPriceMarketInsight(
  modelUri: string,
  excludeCarId: string,
): Promise<ApiResponse<{ minPrice: number; maxPrice: number; averagePrice: number; sampleCount: number } | undefined>> {
  const base = modelUri.endsWith('/') ? modelUri.slice(0, -1) : modelUri;
  return sessionClient.get(`${base}/price-insight`, {
    accept: MediaTypes.priceMarketInsight,
    query: { excludeCarId },
  });
}

export function patchCar(id: string, patch: CarPatchDto): Promise<ApiResponse<CarDto>> {
  return sessionClient.patch<CarDto>(carUri(id), patch, {
    accept: MediaTypes.car,
    contentType: MediaTypes.car,
  });
}

export function deactivateCar(id: string): Promise<ApiResponse<unknown>> {
  return sessionClient.del(carUri(id), { accept: MediaTypes.car });
}

// ---- Availabilities (sub-recurso) ----
export function fetchAvailabilities(
  car: CarDto,
  month?: string,
): Promise<ApiResponse<AvailabilityDto[]>> {
  return sessionClient.follow<AvailabilityDto[]>(car.links.availabilities, {
    accept: MediaTypes.availability,
    query: { month: month || undefined, pageSize: 100 },
  });
}

export function createAvailability(
  car: CarDto,
  body: AvailabilityCreateDto,
): Promise<ApiResponse<AvailabilityDto>> {
  return sessionClient.request<AvailabilityDto>(car.links.availabilities, {
    method: 'POST',
    accept: MediaTypes.availability,
    contentType: MediaTypes.availability,
    body,
  });
}

export async function updateAvailability(
  availability: AvailabilityDto,
  car: CarDto,
  body: AvailabilityCreateDto,
): Promise<ApiResponse<AvailabilityDto>> {
  const self = availability.links.self;
  if (self.includes('/range')) {
    await deleteAvailability(availability);
    return createAvailability(car, body);
  }
  return sessionClient.patch<AvailabilityDto>(self, body, {
    accept: MediaTypes.availability,
    contentType: MediaTypes.availability,
  });
}

export function deleteAvailability(availability: AvailabilityDto): Promise<ApiResponse<unknown>> {
  return sessionClient.del(availability.links.self, { accept: MediaTypes.availability });
}

// ---- Gallery (sub-recurso débil) ----
export async function fetchPictures(car: CarDto): Promise<ApiResponse<PictureDto[]>> {
  const res = await followAllPages<PictureDto>(sessionClient, car.links.pictures, {
    accept: MediaTypes.picture,
  });
  return {
    ...res,
    data: (res.data ?? []).sort((a, b) => a.displayOrder - b.displayOrder),
  };
}

export async function addPicture(
  car: CarDto,
  file: File,
): Promise<ApiResponse<unknown>> {
  const parts = [
    {
      name: 'file',
      value: file,
      filename: file.name || 'picture',
      contentType: file.type || 'application/octet-stream',
    },
  ];
  const { body, contentType } = await encodeMultipart(parts);
  return sessionClient.request(car.links.pictures, {
    method: 'POST',
    accept: MediaTypes.picture,
    body,
    contentType,
  });
}

export function deletePicture(picture: PictureDto): Promise<ApiResponse<unknown>> {
  return sessionClient.del(picture.links.self, { accept: MediaTypes.picture });
}

// ---- Insurance (binario, octet-stream, PUT) ----
export function uploadInsurance(car: CarDto, file: File): Promise<ApiResponse<unknown>> {
  return sessionClient.put(car.links.insurance, file, {
    contentType: file.type || 'application/octet-stream',
    extraHeaders: file.name ? { 'X-Ryden-Filename': file.name } : undefined,
  });
}

export async function openInsurance(car: CarDto): Promise<boolean> {
  return openAuthenticatedBinary(car.links.insurance);
}

// ---- Neighborhoods (catálogo para el select de disponibilidad) ----
export function fetchNeighborhoods(): Promise<ApiResponse<NeighborhoodDto[]>> {
  return sessionClient.get<NeighborhoodDto[]>(getCollectionPath('neighborhoods'), {
    accept: MediaTypes.neighborhood,
  });
}
