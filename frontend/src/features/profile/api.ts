import { sessionClient } from '../../session/sessionStore';
import { MediaTypes } from '../../api/mediaTypes';
import { getLinkCollectionPage } from '../../api/client';
import { openAuthenticatedBinary } from '../../api/openAuthenticatedBinary';
import { favoriteMembershipUri } from '../../api/uri';
import type { CarSummaryDto, DocumentType, UserDto, UserPatchDto } from './types';
import type { ReviewDto } from '../browse/types';

// =============================================================================
// Capa de acceso a la API para el área PROFILE.
// -----------------------------------------------------------------------------
// Todas las operaciones sobre sub-recursos siguen links tipados del DTO usuario.
// La URN `self` no autoriza a que el cliente infiera rutas ni ACLs derivadas.
// =============================================================================

/**
 * GET /users/{id} → UserDto. Por defecto pide la vista PÚBLICA (sirve para perfiles ajenos). Para el
 * perfil propio pasá `{ private: true }`: el server devuelve email/cbu/etc. bajo el media type privado
 * (solo self/admin; 403 si no corresponde). Son DOS representaciones, no el mismo type recortado.
 */
export async function fetchUser(userUri: string, opts?: { private?: boolean }): Promise<UserDto> {
  const accept = opts?.private ? MediaTypes.userPrivate : MediaTypes.user;
  const res = await sessionClient.get<UserDto>(userUri, { accept });
  return res.data;
}

/** PATCH /users/{id} con solo los campos cambiados (perfil propio → vista privada en la respuesta). */
export async function patchUser(userUri: string, patch: UserPatchDto): Promise<UserDto> {
  const res = await sessionClient.patch<UserDto>(userUri, patch, {
    accept: MediaTypes.userPrivate,
    contentType: MediaTypes.user,
  });
  return res.data;
}

/** PUT /users/{id}/profile-picture (raw image/* body). */
export async function uploadProfilePicture(user: UserDto, file: File): Promise<void> {
  const path = user.links?.profilePicture;
  if (!path) throw new Error('profile.picture.missingLink');
  await sessionClient.request(path, {
    method: 'PUT',
    body: file,
    contentType: file.type || 'application/octet-stream',
  });
}

/** DELETE /users/{id}/profile-picture. */
export async function deleteProfilePicture(user: UserDto): Promise<void> {
  const path = user.links?.profilePicture;
  if (!path) throw new Error('profile.picture.missingLink');
  await sessionClient.del(path);
}

/** Link tipado de un documento (license/identity) — para subir, borrar y descargar. */
export function documentPath(user: UserDto, type: DocumentType): string {
  const path = type === 'identity'
    ? user.links?.identityDocument
    : user.links?.licenseDocument;
  if (!path) throw new Error('profile.documents.missingLink');
  return path;
}

/** Sube al link tipado del documento (raw octet-stream body). */
export async function uploadDocument(
  user: UserDto,
  type: DocumentType,
  file: File,
): Promise<void> {
  await sessionClient.request(documentPath(user, type), {
    method: 'PUT',
    body: file,
    contentType: file.type || 'application/octet-stream',
  });
}

/** Elimina mediante el link tipado del documento. */
export async function deleteDocument(user: UserDto, type: DocumentType): Promise<void> {
  await sessionClient.del(documentPath(user, type));
}

/**
 * Abre un documento (license/identity) en una pestaña nueva.
 *
 * El GET del documento exige `Authorization` (self/admin) y devuelve binario
 * (octet-stream / image / pdf). Un `<a href>` plano NO manda el Bearer token
 * (auth stateless por header, no cookies) → daría 401/403. Por eso replicamos
 * el viewer del JSP viejo: se baja el binario autenticado (vía el cliente
 * central, con el mismo reintento 401→refresh que cualquier otro request), se
 * crea un blob URL y se abre en una pestaña nueva. Devuelve false si falla
 * (p.ej. no subido, o el navegador bloqueó el popup).
 */
export async function openDocument(user: UserDto, type: DocumentType): Promise<boolean> {
  return openAuthenticatedBinary(documentPath(user, type));
}

/** Path de la colección de favoritos del usuario. */
export function favoritesPath(user: UserDto): string {
  const path = user.links?.favorites;
  if (!path) throw new Error('profile.favorites.missingLink');
  return path;
}

/**
 * GET favoritos (paginados). `pathOrLink` puede ser el path base o un link de
 * paginación (next/prev) que ya trae los query params.
 */
export async function fetchFavorites(pathOrLink: string) {
  const res = await getLinkCollectionPage<CarSummaryDto>(sessionClient, pathOrLink, {
    collectionAccept: MediaTypes.userFavorites,
    itemAccept: MediaTypes.carSummary,
  });
  return { data: res.data ?? [], page: res.page, status: res.status };
}

/** GET favoritos por índice de página (0-based URL, API 1-based). */
export async function fetchFavoritesPage(user: UserDto, pageIndex: number, pageSize?: number) {
  const res = await getLinkCollectionPage<CarSummaryDto>(sessionClient, favoritesPath(user), {
    collectionAccept: MediaTypes.userFavorites,
    itemAccept: MediaTypes.carSummary,
    query: { page: pageIndex + 1, pageSize },
  });
  return { data: res.data ?? [], page: res.page, status: res.status };
}

/** DELETE membresía de favoritos (idempotente) vía favorites-item-template. */
export async function removeFavorite(user: UserDto, carSelfLink: string): Promise<void> {
  const template = user.links?.['favorites-item-template'];
  if (!template) throw new Error('profile.favorites.missingMembershipTemplate');
  await sessionClient.del(favoriteMembershipUri(template, carSelfLink));
}

/** Path para listar los autos de un usuario (perfil público): GET /cars?ownerId=. */
export function userCarsLink(user: UserDto): string | null {
  return user.links?.cars ?? null;
}

/** GET autos activos de un usuario navegando user.links.cars. */
export async function fetchUserCars(carsLink: string) {
  const res = await sessionClient.get<CarSummaryDto[]>(carsLink, { accept: MediaTypes.carSummary });
  return { data: res.data ?? [], page: res.page, status: res.status };
}

/** Link a las reseñas recibidas por el usuario (GET /reviews?recipientUserId=). */
export function userReviewsLink(user: UserDto): string | null {
  return user.links?.reviews ?? null;
}

/**
 * GET reseñas recibidas navegando user.links.reviews (colección canónica).
 */
export async function getUserReviews(
  reviewsLink: string,
): Promise<{ data: ReviewDto[]; total: number }> {
  const res = await getLinkCollectionPage<ReviewDto>(sessionClient, reviewsLink, {
    collectionAccept: MediaTypes.reviewLinks,
    itemAccept: MediaTypes.review,
  });
  const data = res.data ?? [];
  return { data, total: res.page.total ?? data.length };
}
