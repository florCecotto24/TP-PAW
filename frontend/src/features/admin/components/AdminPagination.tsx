import { useTranslation } from 'react-i18next';
import type { PageLinks } from '../../../api/types';

interface AdminPaginationProps {
  page: PageLinks;
  onGo: (link: string | undefined) => void;
}

export default function AdminPagination({ page, onGo }: AdminPaginationProps) {
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
          onClick={() => onGo(page.prev)}
        >
          {t('admin.common.prev')}
        </button>
        <button
          type="button"
          className="btn btn-outline-secondary btn-sm"
          disabled={!page.next}
          onClick={() => onGo(page.next)}
        >
          {t('admin.common.next')}
        </button>
      </div>
    </nav>
  );
}
