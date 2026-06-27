import type { TFunction } from 'i18next';
import { ApiError } from '../../api/client';

// Mapea un error de API a un mensaje i18n. El backend devuelve `code` (short, p.ej.
// "email_already_exists") y `message` (la CLAVE i18n, p.ej. "user.email.alreadyExists").
// El mapa `error.byCode.*` está keyed por esa clave de mensaje, así que el lookup usa
// `message` (con `code` como respaldo) y cae al genérico si la clave no existe.
// Centraliza el patrón usado por todas las páginas de auth.
export function apiErrorMessage(t: TFunction, err: unknown): string {
  if (err instanceof ApiError) {
    const key = err.body?.message ?? err.code;
    if (key) return t(`error.byCode.${key}`, t('error.generic') as string);
  }
  return t('error.generic') as string;
}
