import type { TFunction } from 'i18next';
import { ApiError } from '../../api/client';

// Mapea un error de API a un mensaje i18n. El backend pone en `code` la clave de dominio
// (p.ej. "user.verification.codeInvalid"); `message` es texto/localización de soporte.
// Lookup por `code` (con `message` como respaldo) y cae al genérico si no hay entrada.
export function apiErrorMessage(t: TFunction, err: unknown): string {
  if (err instanceof ApiError) {
    const key = err.code ?? err.body?.message;
    if (key) {
      const translated = t(`error.byCode.${key}`, { defaultValue: '' });
      if (translated) return translated;
    }
  }
  return t('error.generic') as string;
}
