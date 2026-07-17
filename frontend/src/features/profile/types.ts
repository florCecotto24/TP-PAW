// Tipos del área PROFILE. Derivados de openapi.yaml (UserDto, UserPatchDto,
// CarDto). Se definen localmente para no acoplar la feature a src/api/types.ts
// más allá de lo necesario; los campos coinciden con el contrato.

/** Bloque de hipervínculos. Siempre incluye `self`; el resto es opcional. */
export interface Links {
  self: string;
  profilePicture?: string;
  cars?: string;
  reservations?: string;
  'owned-reservations'?: string;
  favorites?: string;
  identityDocument?: string;
  licenseDocument?: string;
  reviews?: string;
  [rel: string]: string | undefined;
}

/** Rol del usuario. */
export type UserRole = 'user' | 'admin';

/**
 * UserDto público ({@code application/vnd.paw.user.v1+json}) — sin email, CBU, teléfono, role, blocked.
 * UserPrivateDto ({@code application/vnd.paw.user.private.v1+json}) — schema fijo con todos los campos
 * privados; pedirlo con {@code Accept} en GET/PATCH (solo self/admin, 403 si no).
 */
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
  /** Present only on {@code user.private.v1}. */
  licenseValidated?: boolean;
  identityValidated?: boolean;
  licenseUploaded?: boolean;
  identityUploaded?: boolean;
  blocked?: boolean;
  role?: UserRole;
  ratingAsRider?: number | null;
  ratingAsOwner?: number | null;
  links: Links;
}

/**
 * Cuerpo del PATCH /users/{id}. Todos opcionales: solo se mandan los campos a
 * cambiar (LINEAMIENTOS / openapi UserPatchDto). La feature usa el subconjunto
 * que el usuario puede editar sobre sí mismo + el cambio de password.
 */
export interface UserPatchDto {
  forename?: string;
  surname?: string;
  phoneNumber?: string;
  birthDate?: string;
  about?: string;
  cbu?: string;
  latestLocale?: string;
  password?: string;
  currentPassword?: string;
}

/** Campos editables del perfil propio (datos, sin password). */
export interface ProfileFormValues {
  forename: string;
  surname: string;
  phoneNumber: string;
  birthDate: string;
  about: string;
  cbu: string;
}

/** Tipos de documento expuestos mediante links tipados del usuario privado. */
export type DocumentType = 'license' | 'identity';

/** Enums de auto (subconjunto necesario para mostrar el CarDto). */
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

/** Teaser de auto para colecciones (favoritos, autos activos en perfil). */
export interface CarSummaryDto {
  brandName: string;
  modelName: string;
  year?: number | null;
  status: CarStatus;
  minimumRentalDays: number;
  ratingAvg?: number | null;
  dayPrice?: number | null;
  modelValidated: boolean;
  links: Links & { owner?: string; pictures?: string; cover?: string };
}

/** CarDto completo (lectura). */
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
  createdAt: string;
  links: Links & { owner?: string; pictures?: string; cover?: string };
}
