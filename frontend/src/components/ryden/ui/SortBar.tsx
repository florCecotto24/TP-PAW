import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';

export type SortValue =
  | 'date,desc'
  | 'date,asc'
  | 'price,asc'
  | 'price,desc'
  | 'rating,desc'
  | 'rating,asc';

export interface SortBarProps {
  baseUrl: string;
  currentSort?: SortValue | string;
  sortParamName?: string;
  pageParamName?: string;
  wrapperClass?: string;
  dateOnly?: boolean;
  onSortChange?: (sort: SortValue) => void;
}

const SORT_OPTIONS: SortValue[] = [
  'date,desc',
  'date,asc',
  'price,asc',
  'price,desc',
  'rating,desc',
  'rating,asc',
];

function sortLabelKey(sort: SortValue): string {
  const map: Record<SortValue, string> = {
    'date,desc': 'search.sort.dateDesc',
    'date,asc': 'search.sort.dateAsc',
    'price,asc': 'search.sort.priceAsc',
    'price,desc': 'search.sort.priceDesc',
    'rating,desc': 'search.sort.ratingDesc',
    'rating,asc': 'search.sort.ratingAsc',
  };
  return map[sort];
}

/** Espejo de {@code ryden:sortBar}. */
export default function SortBar({
  baseUrl,
  currentSort,
  sortParamName = 'sort',
  pageParamName = 'page',
  wrapperClass = 'd-flex align-items-center gap-2 flex-wrap mb-3',
  dateOnly = false,
  onSortChange,
}: SortBarProps) {
  const { t } = useTranslation();
  const active = (currentSort || 'date,desc') as SortValue;
  const activeLabel = t(sortLabelKey(active));
  const visibleOptions = dateOnly ? SORT_OPTIONS.filter((s) => s.startsWith('date')) : SORT_OPTIONS;

  const buildHref = (sort: SortValue) => {
    const sep = baseUrl.includes('?') ? '&' : '?';
    return `${baseUrl}${sep}${sortParamName}=${sort}&${pageParamName}=0`;
  };

  return (
    <div className={wrapperClass}>
      <span className="text-secondary small fw-medium flex-shrink-0">{t('search.sort.label')}:</span>
      <div className="dropdown">
        <button
          type="button"
          className="form-select form-select-sm dropdown-toggle ryden-select-btn text-start"
          style={{ minWidth: 190 }}
          data-bs-toggle="dropdown"
          data-bs-auto-close="true"
          aria-expanded="false"
          aria-label={t('search.sort.label')}
        >
          {activeLabel}
        </button>
        <ul className="dropdown-menu shadow ryden-select-menu p-1" style={{ minWidth: 200 }}>
          {visibleOptions.map((sort) => {
            const isAct = active === sort || (!currentSort && sort === 'date,desc');
            return (
              <li key={sort}>
                {onSortChange ? (
                  <button
                    type="button"
                    className={`dropdown-item ryden-select-item${isAct ? ' ryden-select-item--active' : ''}`}
                    onClick={() => onSortChange(sort)}
                  >
                    <i
                      className={`bi bi-check2 ryden-sel-check${isAct ? '' : ' invisible'}`}
                      aria-hidden="true"
                    ></i>
                    {t(sortLabelKey(sort))}
                  </button>
                ) : (
                  <Link
                    to={buildHref(sort)}
                    className={`dropdown-item ryden-select-item${isAct ? ' ryden-select-item--active' : ''}`}
                  >
                    <i
                      className={`bi bi-check2 ryden-sel-check${isAct ? '' : ' invisible'}`}
                      aria-hidden="true"
                    ></i>
                    {t(sortLabelKey(sort))}
                  </Link>
                )}
              </li>
            );
          })}
        </ul>
      </div>
    </div>
  );
}
