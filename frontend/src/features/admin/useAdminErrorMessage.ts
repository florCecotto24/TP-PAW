import { useTranslation } from 'react-i18next';
import { ApiError } from '../../api/client';

/**
 * Traduce un error de la API a un mensaje i18n para las páginas ADMIN.
 *
 * El backend manda `message` con la CLAVE i18n del error (o, en su defecto, un
 * `code`), mapeada en el catálogo global `error.byCode.*`. Si hay match se usa
 * ese mensaje específico; si no, se cae a `error.generic`.
 *
 * Centraliza el patrón que antes duplicaban AdminUsersPage / AdminCarsPage /
 * AdminCatalogPage. Es el espejo admin de `useApiErrorMessage` (área OWNER).
 */
export function useAdminErrorMessage(): (err: unknown) => string {
  const { t } = useTranslation();
  return (err) => {
    const code = err instanceof ApiError ? (err.body?.message ?? err.code) : undefined;
    return code ? t([`error.byCode.${code}`, 'error.generic']) : t('error.generic');
  };
}
