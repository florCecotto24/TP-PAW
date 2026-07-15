import { useTranslation } from 'react-i18next';
import type { PageLinks } from '../../../api/types';

interface AdminPaginationProps {
  page: PageLinks;
  /** Página actual (0-based, como SearchPage): leída de la URL por la pantalla. */
  currentPage: number;
  /** Navega a la página `pageIndex` (0-based); la pantalla la persiste en `?page=N`. */
  onPageChange: (pageIndex: number) => void;
}

/**
 * Controles de paginación URL-driven. La disponibilidad de prev/next se decide
 * por los rels del header `Link` (RFC 5988) que vinieron en la respuesta; al
 * clickear se navega por número de página (la pantalla lo persiste en `?page=N`).
 */
export default function AdminPagination({ page, currentPage, onPageChange }: AdminPaginationProps) {
  const { t } = useTranslation();
  if (!page.prev && !page.next && page.total == null) {
    return null;
  }
  return (
    <nav
      className="d-flex flex-wrap align-items-center justify-content-between gap-2 mt-3"
      aria-label={t('admin.common.pagination')}
    >
      {page.total != null ? (
        <span className="text-secondary small">{t('admin.common.total', { count: page.total })}</span>
      ) : (
        <span />
      )}
      <div className="btn-group">
        <button
          type="button"
          className="btn btn-outline-secondary btn-sm"
          disabled={!page.prev}
          onClick={() => onPageChange(Math.max(0, currentPage - 1))}
        >
          {t('admin.common.prev')}
        </button>
        <button
          type="button"
          className="btn btn-outline-secondary btn-sm"
          disabled={!page.next}
          onClick={() => onPageChange(currentPage + 1)}
        >
          {t('admin.common.next')}
        </button>
      </div>
    </nav>
  );
}
