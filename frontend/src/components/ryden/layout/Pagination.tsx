import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';

export interface PaginationProps {
  /** Página actual (0-based, como el tag JSP). */
  currentPage: number;
  totalPages: number;
  baseUrl: string;
  pageParam?: string;
  sortParam?: string;
  sortParamName?: string;
}

function buildInternalPageTo(
  baseUrl: string,
  page: number,
  pageParam: string,
  sortParam?: string,
  sortParamName = 'sort',
): string {
  const [path, query = ''] = baseUrl.split('?');
  const qs = new URLSearchParams(query);
  qs.set(pageParam, String(page));
  if (sortParam) {
    qs.set(sortParamName, sortParam);
  } else {
    qs.delete(sortParamName);
  }
  const serialized = qs.toString();
  return serialized ? `${path}?${serialized}` : path;
}

function buildPageHref(
  baseUrl: string,
  page: number,
  pageParam: string,
  sortSuffix: string,
): string {
  const sep = baseUrl.includes('?') ? '&' : '?';
  return `${baseUrl}${sep}${pageParam}=${page}${sortSuffix}`;
}

/** Espejo de {@code ryden:pagination}: ventana de 7 páginas con elipsis. */
export default function Pagination({
  currentPage,
  totalPages,
  baseUrl,
  pageParam = 'page',
  sortParam,
  sortParamName = 'sort',
}: PaginationProps) {
  const { t } = useTranslation();

  if (totalPages <= 1) return null;

  const sortSuffix = sortParam ? `&${sortParamName}=${encodeURIComponent(sortParam)}` : '';
  const windowStart = Math.max(0, currentPage - 2);
  const windowEnd = Math.min(totalPages - 1, currentPage + 2);

  const pages: number[] = [];
  for (let p = windowStart; p <= windowEnd; p++) pages.push(p);

  const PageLink = ({ page, children }: { page: number; children: ReactNode }) => {
    const internal = baseUrl.startsWith('/') && !baseUrl.includes('://');
    if (internal) {
      return (
        <Link
          to={buildInternalPageTo(baseUrl, page, pageParam, sortParam, sortParamName)}
          className="page-link"
        >
          {children}
        </Link>
      );
    }
    const href = buildPageHref(baseUrl, page, pageParam, sortSuffix);
    return (
      <a href={href} className="page-link">
        {children}
      </a>
    );
  };

  return (
    <nav aria-label="Page navigation" className="mt-4">
      <ul className="pagination justify-content-center flex-wrap gap-1">
        <li className={`page-item${currentPage === 0 ? ' disabled' : ''}`}>
          {currentPage > 0 ? (
            <PageLink page={currentPage - 1}>{t('pagination.prev')}</PageLink>
          ) : (
            <span className="page-link">{t('pagination.prev')}</span>
          )}
        </li>

        {windowStart > 0 ? (
          <>
            <li className="page-item">
              <PageLink page={0}>1</PageLink>
            </li>
            {windowStart > 1 ? (
              <li className="page-item disabled">
                <span className="page-link">&hellip;</span>
              </li>
            ) : null}
          </>
        ) : null}

        {pages.map((p) => (
          <li key={p} className={`page-item${p === currentPage ? ' active' : ''}`}>
            {p === currentPage ? (
              <span className="page-link">{p + 1}</span>
            ) : (
              <PageLink page={p}>{p + 1}</PageLink>
            )}
          </li>
        ))}

        {windowEnd < totalPages - 1 ? (
          <>
            {windowEnd < totalPages - 2 ? (
              <li className="page-item disabled">
                <span className="page-link">&hellip;</span>
              </li>
            ) : null}
            <li className="page-item">
              <PageLink page={totalPages - 1}>{totalPages}</PageLink>
            </li>
          </>
        ) : null}

        <li className={`page-item${currentPage >= totalPages - 1 ? ' disabled' : ''}`}>
          {currentPage < totalPages - 1 ? (
            <PageLink page={currentPage + 1}>{t('pagination.next')}</PageLink>
          ) : (
            <span className="page-link">{t('pagination.next')}</span>
          )}
        </li>
      </ul>
    </nav>
  );
}
