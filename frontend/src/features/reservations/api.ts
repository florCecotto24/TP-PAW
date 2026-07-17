// Capa de acceso a la API para el área RESERVATIONS.
// Thin wrapper sobre sessionClient: una llamada por operación, sin lógica de UI.
import { getCollectionPath } from '../../api/apiDiscovery';
import { type ApiResponse, getLinkCollectionPage } from '../../api/client';
import { encodeMultipart } from '../../api/multipart';
import { sessionClient } from '../../session/sessionStore';
import { getChatHistoryPageSize } from './chatAttachment';
import { MediaTypes } from '../../api/mediaTypes';
import { idFromUri } from '../../api/uri';
import { openAuthenticatedBinary } from '../../api/openAuthenticatedBinary';
import type {
  MessageDto,
  ReservationCreateDto,
  ReservationDto,
  ReservationSummaryDto,
  ReservationPatchDto,
  ReviewDto,
} from './types';
import type { MultipartPart } from '../../api/multipart';

// Las URN canónicas viven en `links`; el carId de la ruta /reservar/:carId se
// recibe del front para la URL del SPA. Preferir `location.state.carSelf` o
// `links.*` del DTO padre; el fallback `/cars/{id}` queda solo para bookmarks.

// idFromUri ("/reservations/42" -> "42") vive en api/uri (helper compartido);
// se re-exporta para no tocar los call sites que lo importan desde aquí.
export { idFromUri };

export interface ReservationListQuery {
  riderId?: string | number;
  ownerId?: string | number;
  carId?: string | number;
  status?: string | string[];
  riderStatus?: string | string[];
  q?: string;
  category?: string | string[];
  transmission?: string | string[];
  powertrain?: string | string[];
  priceMin?: number;
  priceMax?: number;
  rating?: string | string[];
  sort?: 'recent' | 'start_date' | 'price_asc' | 'price_desc';
  page?: number;
  pageSize?: number;
}

function toQueryArray(value?: string | string[]): string[] | undefined {
  if (value == null) return undefined;
  const items = Array.isArray(value) ? value : [value];
  const out = items.map((v) => String(v).trim()).filter(Boolean);
  return out.length > 0 ? out : undefined;
}

export async function listReservations(
  q: ReservationListQuery,
): Promise<ApiResponse<ReservationSummaryDto[]>> {
  const collectionRes = await getLinkCollectionPage<ReservationSummaryDto>(
    sessionClient,
    getCollectionPath('reservations'),
    {
      collectionAccept: MediaTypes.reservationLinks,
      itemAccept: MediaTypes.reservationSummary,
      query: {
        riderId: q.riderId,
        ownerId: q.ownerId,
        carId: q.carId,
        status: toQueryArray(q.status),
        riderStatus: toQueryArray(q.riderStatus),
        q: q.q,
        category: toQueryArray(q.category),
        transmission: toQueryArray(q.transmission),
        powertrain: toQueryArray(q.powertrain),
        priceMin: q.priceMin,
        priceMax: q.priceMax,
        rating: toQueryArray(q.rating),
        sort: q.sort,
        page: q.page,
        pageSize: q.pageSize,
      },
    },
  );
  return collectionRes;
}

export function getReservation(uri: string): Promise<ApiResponse<ReservationDto>> {
  return sessionClient.follow<ReservationDto>(uri, { accept: MediaTypes.reservation });
}

// ---- Datos para el formulario de reserva (réplica de reservationForm.jsp) ----

/** Vista mínima del rider para chequear documentos (licencia/identidad) y datos de cuenta. */
export interface RiderUserView {
  forename: string;
  surname: string;
  email: string;
  licenseValidated: boolean;
  identityValidated: boolean;
  licenseUploaded?: boolean;
  identityUploaded?: boolean;
  links: Record<string, string> & {
    identityDocument?: string;
    licenseDocument?: string;
  };
}

export interface CarSummaryView {
  brandName: string;
  modelName: string;
  minimumRentalDays?: number;
  links: Record<string, string>;
}

export interface AvailabilityView {
  startDate: string;
  endDate: string;
  dayPrice: number;
  kind?: 'offered' | 'withdrawn';
  startPointStreet?: string;
  startPointNumber?: string;
  checkInTime?: string;
  checkOutTime?: string;
  links: Record<string, string>;
}

/** Usuario propio (rider) para el resumen de la nueva reserva: vista PRIVADA (muestra su email). */
export function getUser(userUri: string): Promise<ApiResponse<RiderUserView>> {
  return sessionClient.follow<RiderUserView>(userUri, { accept: MediaTypes.userPrivate });
}

export function getCarSummary(carUri: string): Promise<ApiResponse<CarSummaryView>> {
  return sessionClient.follow<CarSummaryView>(carUri, { accept: MediaTypes.carSummary });
}

/** Vista del auto de la reserva (marca/modelo, transmisión, combustible, dueño). */
export interface CarDetailView {
  brandName: string;
  modelName: string;
  transmission?: string;
  powertrain?: string;
  links: Record<string, string>;
}

export function getCar(carUri: string): Promise<ApiResponse<CarDetailView>> {
  return sessionClient.follow<CarDetailView>(carUri, { accept: MediaTypes.car });
}

/** Vista de la contraparte (dueño o inquilino) para el panel de contacto. */
export interface CounterpartyView {
  forename: string;
  surname: string;
  email?: string | null;
  phoneNumber?: string | null;
  cbu?: string | null;
  links: Record<string, string>;
}

export function getCounterparty(counterpartyUri: string): Promise<ApiResponse<CounterpartyView>> {
  return sessionClient.follow<CounterpartyView>(counterpartyUri, {
    accept: MediaTypes.counterpartyContact,
  });
}

export function getAvailability(uri: string): Promise<ApiResponse<AvailabilityView>> {
  return sessionClient.follow<AvailabilityView>(uri, { accept: MediaTypes.availability });
}

/** Sube un documento del rider (licencia/identidad) — PUT octet-stream. */
export function uploadUserDocument(
  user: Pick<RiderUserView, 'links'>,
  type: 'license' | 'identity',
  file: File,
): Promise<ApiResponse<unknown>> {
  const documentLink = type === 'identity'
    ? user.links.identityDocument
    : user.links.licenseDocument;
  if (!documentLink) throw new Error('reservation.user.missingDocumentLink');
  return sessionClient.request(documentLink, {
    method: 'PUT',
    body: file,
    contentType: file.type || 'application/octet-stream',
  });
}

export function listCarAvailabilities(
  availabilitiesLink: string,
): Promise<ApiResponse<AvailabilityView[]>> {
  return sessionClient.follow<AvailabilityView[]>(availabilitiesLink, {
    accept: MediaTypes.availability,
  });
}

/** Abre un recurso binario autenticado (comprobante, adjunto) en pestaña nueva. */
export async function openBinaryLink(link: string): Promise<boolean> {
  return openAuthenticatedBinary(link);
}

export function createReservation(
  body: ReservationCreateDto,
): Promise<ApiResponse<ReservationDto>> {
  return sessionClient.post<ReservationDto>(getCollectionPath('reservations'), body, {
    accept: MediaTypes.reservation,
    contentType: MediaTypes.reservation,
  });
}

export function patchReservation(
  uri: string,
  body: ReservationPatchDto,
): Promise<ApiResponse<ReservationDto>> {
  return sessionClient.patch<ReservationDto>(uri, body, {
    accept: MediaTypes.reservation,
    contentType: MediaTypes.reservation,
  });
}

/** Sube un comprobante (pago o reintegro). El Content-Type real del archivo
 *  (image/* o application/pdf) es lo que valida el server; no usar octet-stream. */
export function uploadReceipt(receiptUri: string, file: File): Promise<ApiResponse<unknown>> {
  return sessionClient.put<unknown>(receiptUri, file, {
    contentType: receiptContentType(file),
  });
}

function receiptContentType(file: File): string {
  const declared = (file.type || '').trim().toLowerCase();
  if (declared.startsWith('image/') || declared === 'application/pdf') {
    return declared;
  }
  const name = (file.name || '').toLowerCase();
  if (name.endsWith('.pdf')) return 'application/pdf';
  if (/\.(jpe?g)$/.test(name)) return 'image/jpeg';
  if (name.endsWith('.png')) return 'image/png';
  if (name.endsWith('.gif')) return 'image/gif';
  if (name.endsWith('.webp')) return 'image/webp';
  return declared || 'application/octet-stream';
}

export interface ReviewInput {
  rating: number;
  comment: string;
  image?: File | null;
}

/** POST review (multipart) a `/reviews` con URN de reserva en el body. */
export async function postReview(
  reviewsUri: string,
  reservationUri: string,
  input: ReviewInput,
): Promise<ApiResponse<ReviewDto>> {
  const parts: MultipartPart[] = [
    { name: 'reservationUri', value: reservationUri },
    { name: 'rating', value: String(input.rating) },
    { name: 'comment', value: input.comment ?? '' },
  ];
  if (input.image) {
    parts.push({
      name: 'image',
      value: input.image,
      filename: input.image.name || 'review-image',
      contentType: input.image.type || 'application/octet-stream',
    });
  }
  const { body, contentType } = await encodeMultipart(parts);
  // POST to the reviews collection (strip filter query from the list link).
  const collectionPath = reviewsUri.split('?')[0];
  return sessionClient.post<ReviewDto>(collectionPath, body, {
    accept: MediaTypes.review,
    contentType,
  });
}

export async function listReviews(reviewsUri: string): Promise<ApiResponse<ReviewDto[]>> {
  return getLinkCollectionPage<ReviewDto>(sessionClient, reviewsUri, {
    collectionAccept: MediaTypes.reviewLinks,
    itemAccept: MediaTypes.review,
  });
}

export function listMessages(
  messagesUri: string,
  opts: { afterId?: string | number; page?: number; pageSize?: number } = {},
): Promise<ApiResponse<MessageDto[]>> {
  return sessionClient.get<MessageDto[]>(messagesUri, {
    accept: MediaTypes.message,
    query: { afterId: opts.afterId, page: opts.page, pageSize: opts.pageSize },
  });
}

/** Carga la página más reciente del historial (Link rel=last o cálculo por X-Total-Count). */
export async function listMessagesLatestPage(
  messagesUri: string,
): Promise<ApiResponse<MessageDto[]>> {
  const probe = await sessionClient.get<MessageDto[]>(messagesUri, {
    accept: MediaTypes.message,
    query: { page: 1, pageSize: getChatHistoryPageSize() },
  });
  if (probe.status === 204 || (probe.page.total ?? 0) === 0) {
    return { ...probe, data: [] };
  }
  if (probe.page.last) {
    return sessionClient.follow<MessageDto[]>(probe.page.last, { accept: MediaTypes.message });
  }
  const total = probe.page.total ?? 0;
  const pageSize = getChatHistoryPageSize();
  const lastPage = Math.max(1, Math.ceil(total / pageSize));
  return listMessages(messagesUri, { page: lastPage, pageSize });
}

/** Envía un mensaje (multipart: body + file opcional). */
export async function sendMessage(
  messagesUri: string,
  body: string,
  file?: File | null,
): Promise<ApiResponse<MessageDto>> {
  const parts: MultipartPart[] = [{ name: 'body', value: body ?? '' }];
  if (file) {
    parts.push({
      name: 'file',
      value: file,
      filename: file.name || 'attachment',
      contentType: file.type || 'application/octet-stream',
    });
  }
  const { body: payload, contentType } = await encodeMultipart(parts);
  return sessionClient.post<MessageDto>(messagesUri, payload, {
    accept: MediaTypes.message,
    contentType,
  });
}
