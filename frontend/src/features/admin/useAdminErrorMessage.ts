import { useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { ApiError } from '../../api/client';

/**
 * Traduce un error de la API a un mensaje i18n para las páginas ADMIN.
 *
 * {@code ErrorDto.code} lleva la clave de dominio (p.ej. {@code user.email.alreadyExists});
 * {@code ErrorDto.message} ya viene localizado por el servidor y solo se usa como fallback.
 * Las claves se resuelven en {@code error.byCode.*}; si no hay match, se prefiere el
 * {@code message} del body antes de caer a {@code error.generic}.
 */
export function useAdminErrorMessage(): (err: unknown) => string {
  const { t } = useTranslation();
  return useCallback(
    (err) => {
      if (!(err instanceof ApiError)) {
        return t('error.generic');
      }
      const code = err.code ?? err.body?.code;
      if (code) {
        const byCode = t(`error.byCode.${code}`, { defaultValue: '' });
        if (byCode) {
          return byCode;
        }
      }
      const serverMessage = err.body?.message?.trim();
      if (serverMessage) {
        return serverMessage;
      }
      if (err.status === 409) {
        return t('error.conflict');
      }
      if (err.status === 403) {
        return t('error.forbidden');
      }
      if (err.status === 404) {
        return t('error.notFound');
      }
      return t('error.generic');
    },
    [t],
  );
}
