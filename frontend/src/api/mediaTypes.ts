// MIME types propios de la API (LINEAMIENTOS §1.7).
// NUNCA usar `application/json` genérico: cada recurso negocia su vendor type
// versionado `application/vnd.paw.<entidad>.vN+json`.
//
// Se usan en `Accept` (lecturas) y `Content-Type` (escrituras). Los binarios
// (imágenes, documentos, comprobantes) NO se versionan: se piden/suben como
// image/* u application/octet-stream (decisión D8), por eso no están acá.

export const MediaTypes = {
  api: 'application/vnd.paw.api.v1+json',
  config: 'application/vnd.paw.config.v1+json',
  user: 'application/vnd.paw.user.v1+json',
  // Vista privada del usuario (email, cbu, teléfono, rol, blocked, KYC). El server la devuelve solo a
  // self/admin y tira 403 si se pide sin permiso. Se usa para "mi perfil", el publisher y el admin.
  userPrivate: 'application/vnd.paw.user.private.v1+json',
  userFavorites: 'application/vnd.paw.user.favorites.v1+json',
  // Content-Type dedicado para POST /users cuando lo crea un admin (cuenta admin
  // pre-verificada + contraseña temporal). Discrimina del alta pública (`user`).
  adminCreateUser: 'application/vnd.paw.admincreateuser.v1+json',
  carSummary: 'application/vnd.paw.car.summary.v1+json',
  carSimilar: 'application/vnd.paw.car.similar.v1+json',
  car: 'application/vnd.paw.car.v1+json',
  availability: 'application/vnd.paw.availability.v1+json',
  picture: 'application/vnd.paw.picture.v1+json',
  reservationSummary: 'application/vnd.paw.reservation.summary.v1+json',
  reservationLinks: 'application/vnd.paw.reservation.links.v1+json',
  reservation: 'application/vnd.paw.reservation.v1+json',
  counterpartyContact: 'application/vnd.paw.counterpartycontact.v1+json',
  message: 'application/vnd.paw.message.v1+json',
  review: 'application/vnd.paw.review.v1+json',
  reviewLinks: 'application/vnd.paw.review.links.v1+json',
  brand: 'application/vnd.paw.brand.v1+json',
  model: 'application/vnd.paw.model.v1+json',
  priceMarketInsight: 'application/vnd.paw.pricemarketinsight.v1+json',
  neighborhood: 'application/vnd.paw.neighborhood.v1+json',
  bookableSegment: 'application/vnd.paw.bookablesegment.v1+json',
  credential: 'application/vnd.paw.credential.v1+json',
  emailVerificationCode: 'application/vnd.paw.emailverificationcode.v1+json',
  error: 'application/vnd.paw.error.v1+json',
  validationError: 'application/vnd.paw.validation-error.v1+json',
} as const;

export type MediaType = (typeof MediaTypes)[keyof typeof MediaTypes];
