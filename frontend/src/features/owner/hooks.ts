// Helpers compartidos por las páginas OWNER.

import { useTranslation } from 'react-i18next';
import { useSessionStore } from '../../session/sessionStore';
import { ApiError } from '../../api/client';
import { idFromUri } from './api';

/** id numérico del usuario logueado, derivado de su URN (currentUserUri). */
export function useCurrentUserId(): string | null {
  const uri = useSessionStore((s) => s.currentUserUri);
  return idFromUri(uri);
}

/**
 * Traduce un error de la API a un mensaje i18n. El backend manda `message` con la
 * CLAVE i18n del error (p.ej. "user.profile.cbuInvalid", "car.publish.cbuRequired"),
 * mapeada en el catálogo global `error.byCode.*`; si hay match, se usa ese mensaje
 * específico. Si no, se tratan 403 (prerequisitos no cumplidos: identidad/cbu) y 409
 * con mensajes propios del área, y por último el `fallbackKey`.
 */
export function useApiErrorMessage(): (err: unknown, fallbackKey?: string) => string {
  const { t } = useTranslation();
  return (err, fallbackKey = 'owner.errors.generic') => {
    if (err instanceof ApiError) {
      const key = err.body?.message ?? err.code;
      if (key) {
        const specific = t(`error.byCode.${key}`, { defaultValue: '' });
        if (specific) return specific;
      }
      if (err.status === 403) return t('owner.errors.forbidden');
      if (err.status === 409) return t('owner.errors.conflict');
    }
    return t(fallbackKey);
  };
}
