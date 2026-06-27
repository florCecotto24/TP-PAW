// Capa de acceso a la API para el área RESERVATIONS.
// Thin wrapper sobre sessionClient: una llamada por operación, sin lógica de UI.
import { type ApiResponse } from '../../api/client';
import { appBasePath } from '../../appBasePath';
import { sessionClient, useSessionStore } from '../../session/sessionStore';
import { MediaTypes } from '../../api/mediaTypes';
import { idFromUri } from '../../api/uri';
import type {
  MessageDto,
  ReservationCreateDto,
  ReservationDto,
  ReservationPatchDto,
  ReviewDto,
} from './types';

// Las URN canónicas viven en `links`; el carId de la ruta /reservar/:carId se
// recibe del front, por eso se arma carUri a alto nivel. El resto se navega.

// idFromUri ("/reservations/42" -> "42") vive en api/uri (helper compartido);
// se re-exporta para no tocar los call sites que lo importan desde aquí.
export { idFromUri };

export interface ReservationListQuery {
  riderId?: string | number;
  ownerId?: string | number;
  carId?: string | number;
  status?: string;
  sort?: 'recent' | 'start_date' | 'price_asc' | 'price_desc';
  page?: number;
  pageSize?: number;
}

export function listReservations(
  q: ReservationListQuery,
): Promise<ApiResponse<ReservationDto[]>> {
  // 204 (sin reservas) llega como data undefined; la página lo normaliza a [].
  return sessionClient.get<ReservationDto[]>('/reservations', {
    accept: MediaTypes.reservation,
    query: {
      riderId: q.riderId,
      ownerId: q.ownerId,
      carId: q.carId,
      status: q.status,
      sort: q.sort,
      page: q.page,
      pageSize: q.pageSize,
    },
  });
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
  links: Record<string, string>;
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
  return sessionClient.follow<CarSummaryView>(carUri, { accept: MediaTypes.car });
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

export function getCounterparty(userUri: string): Promise<ApiResponse<CounterpartyView>> {
  return sessionClient.follow<CounterpartyView>(userUri, { accept: MediaTypes.user });
}

export function getAvailability(uri: string): Promise<ApiResponse<AvailabilityView>> {
  return sessionClient.follow<AvailabilityView>(uri, { accept: MediaTypes.availability });
}

/** Sube un documento del rider (licencia/identidad) — PUT octet-stream. */
export function uploadUserDocument(
  userUri: string,
  type: 'license' | 'identity',
  file: File,
): Promise<ApiResponse<unknown>> {
  return sessionClient.request(`${userUri.replace(/\/$/, '')}/documents/${type}`, {
    method: 'PUT',
    body: file,
    contentType: file.type || 'application/octet-stream',
  });
}

export function listCarAvailabilities(
  carUri: string,
): Promise<ApiResponse<AvailabilityView[]>> {
  const base = carUri.endsWith('/') ? carUri.slice(0, -1) : carUri;
  return sessionClient.get<AvailabilityView[]>(`${base}/availabilities`, {
    accept: MediaTypes.availability,
  });
}

/** Abre un recurso binario autenticado (comprobante, adjunto) en pestaña nueva. */
export async function openBinaryLink(link: string): Promise<boolean> {
  const token = useSessionStore.getState().accessToken;
  const path = link.startsWith('http') ? link : `${appBasePath()}${link.startsWith('/') ? link : `/${link}`}`;
  const res = await fetch(path, {
    headers: token ? { Authorization: `Bearer ${token}` } : undefined,
  });
  if (!res.ok) return false;
  const blob = await res.blob();
  const objectUrl = URL.createObjectURL(blob);
  const win = window.open(objectUrl, '_blank', 'noopener,noreferrer');
  if (!win) {
    URL.revokeObjectURL(objectUrl);
    return false;
  }
  setTimeout(() => URL.revokeObjectURL(objectUrl), 60_000);
  return true;
}

export function createReservation(
  body: ReservationCreateDto,
): Promise<ApiResponse<ReservationDto>> {
  return sessionClient.post<ReservationDto>('/reservations', body, {
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

/** Sube un comprobante (pago o reintegro) como octet-stream al sub-recurso. */
export function uploadReceipt(receiptUri: string, file: File): Promise<ApiResponse<unknown>> {
  return sessionClient.put<unknown>(receiptUri, file, {
    contentType: 'application/octet-stream',
  });
}

export interface ReviewInput {
  rating: number;
  comment: string;
  image?: File | null;
}

/** POST review (multipart). El lado (madeByRider) lo deriva el server por rol. */
export function postReview(
  reviewsUri: string,
  input: ReviewInput,
): Promise<ApiResponse<ReviewDto>> {
  const fd = new FormData();
  fd.append('rating', String(input.rating));
  fd.append('comment', input.comment);
  if (input.image) fd.append('image', input.image);
  return sessionClient.post<ReviewDto>(reviewsUri, fd, {
    accept: MediaTypes.review,
  });
}

export function listReviews(reviewsUri: string): Promise<ApiResponse<ReviewDto[]>> {
  return sessionClient.get<ReviewDto[]>(reviewsUri, { accept: MediaTypes.review });
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

/** Envía un mensaje (multipart: body + file opcional). */
export function sendMessage(
  messagesUri: string,
  body: string,
  file?: File | null,
): Promise<ApiResponse<MessageDto>> {
  const fd = new FormData();
  fd.append('body', body);
  if (file) fd.append('file', file);
  return sessionClient.post<MessageDto>(messagesUri, fd, {
    accept: MediaTypes.message,
  });
}
