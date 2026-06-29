import { useTranslation } from 'react-i18next';

export interface SearchBarFieldProps {
  query?: string;
  from?: string;
  until?: string;
  onQueryChange?: (value: string) => void;
  onUntilChange?: (value: string) => void;
  onSubmit?: () => void;
}

/**
 * Espejo de {@code ryden-search:searchBar}: barra compacta con query + fechas + botón buscar.
 * Los pickers de fecha se conectan vía props (Flatpickr en el caller).
 */
export default function SearchBar({
  query = '',
  from = '',
  until = '',
  onQueryChange,
  onUntilChange,
  onSubmit,
}: SearchBarFieldProps) {
  const { t } = useTranslation();

  return (
    <div className="container mb-4">
      <form
        className="d-flex align-items-center bg-white rounded-4 px-3 py-2 shadow gap-2 flex-wrap"
        onSubmit={(e) => {
          e.preventDefault();
          onSubmit?.();
        }}
      >
        <div className="form-floating flex-grow-1" style={{ minWidth: '12rem' }}>
          <input
            type="text"
            className="form-control border-0 shadow-none"
            aria-label={t('searchBar.query.ariaLabel')}
            id="search_query"
            name="query"
            value={query}
            placeholder=" "
            onChange={(e) => onQueryChange?.(e.target.value)}
          />
          <label htmlFor="search_query">{t('searchBar.query.label')}</label>
        </div>

        <div className="vr flex-shrink-0 d-none d-md-block" />

        <div className="d-flex flex-wrap gap-2 flex-grow-1 align-items-end" style={{ minWidth: '14rem' }}>
          <div className="flex-grow-1" style={{ minWidth: '7rem' }}>
            <label className="form-label small text-secondary mb-1" htmlFor="search_from_picker">
              {t('searchBar.from.label')}
            </label>
            <input
              type="text"
              className="form-control form-control-sm border-0 shadow-none"
              id="search_from_picker"
              readOnly
              placeholder={t('searchBar.date.placeholder')}
              aria-label={t('searchBar.from.ariaLabel')}
            />
            <input type="hidden" name="from" id="search_from_hidden" value={from} readOnly />
          </div>
          <div className="flex-grow-1" style={{ minWidth: '7rem' }}>
            <label className="form-label small text-secondary mb-1" htmlFor="search_until_picker">
              {t('searchBar.until.label')}
            </label>
            <input
              type="text"
              className="form-control form-control-sm border-0 shadow-none"
              id="search_until_picker"
              readOnly
              placeholder={t('searchBar.date.placeholder')}
              aria-label={t('searchBar.until.ariaLabel')}
            />
            <input
              type="hidden"
              name="until"
              id="search_until_hidden"
              value={until}
              onChange={(e) => onUntilChange?.(e.target.value)}
            />
          </div>
        </div>

        <div className="vr flex-shrink-0 d-none d-md-block" />

        <button
          type="submit"
          className="btn btn-primary rounded-3 ms-md-3 p-2 flex-shrink-0"
          aria-label={t('searchBar.submit.ariaLabel')}
        >
          <i className="bi bi-search fs-5 search-btn" aria-hidden="true"></i>
        </button>
      </form>
    </div>
  );
}
