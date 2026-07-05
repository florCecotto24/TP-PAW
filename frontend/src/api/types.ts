// Tipos del contrato de la API (subconjunto usado por la infra core).
// Derivados de openapi.yaml. Las páginas de feature (F9) extienden esto.

/** Bloque de hipervínculos presente en TODA representación (siempre `self`). */
export interface Links {
  self: string;
  [rel: string]: string;
}

/** DTO de usuario público (lectura). Campos privados solo en user.private MIME. */
export interface UserDto {
  forename: string;
  surname: string;
  email?: string;
  phoneNumber?: string | null;
  birthDate?: string | null;
  about?: string | null;
  cbu?: string | null;
  memberSince?: string;
  latestLocale?: string | null;
  emailVerified: boolean;
  licenseValidated: boolean;
  identityValidated: boolean;
  licenseUploaded?: boolean;
  identityUploaded?: boolean;
  blocked?: boolean;
  /** Deep-link a la reserva con comprobante de reembolso vencido, cuando hay exactamente una (banner bloqueado). */
  blockedOverdueReservationId?: number | null;
  role?: 'user' | 'admin';
  ratingAsRider?: number | null;
  ratingAsOwner?: number | null;
  links: Links;
}

/** Cuerpo para registrar un usuario (POST /users). */
export interface UserCreateDto {
  email: string;
  forename: string;
  surname: string;
  password: string;
  passwordConfirm: string;
}

/** ErrorDto uniforme de los ExceptionMappers. */
export interface ErrorDto {
  status: number;
  code?: string;
  message?: string;
  errors?: Array<{ field: string; message: string }>;
}

/** Links de paginación parseados del header `Link` (RFC 5988) + X-Total-Count. */
export interface PageLinks {
  first?: string;
  prev?: string;
  next?: string;
  last?: string;
  total?: number;
}
