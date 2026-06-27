import { useCallback, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useSearchParams } from 'react-router-dom';
import { idFromUri } from '../../../api/uri';
import BrowseCarCard from '../components/BrowseCarCard';
import {
  flattenCars,
  searchTotal,
  useNeighborhoods,
  useSearchCars,
} from '../hooks';
import {
  filtersToSearchParams,
  parseFilters,
  type SearchFilters,
} from '../searchFilters';
import {
  CAR_SORTS,
  CAR_TYPES,
  POWERTRAINS,
  TRANSMISSIONS,
  type CarSort,
  type CarType,
  type Powertrain,
  type Transmission,
} from '../types';

export default function SearchPage() {
  const { t } = useTranslation();
  const [searchParams, setSearchParams] = useSearchParams();
  const filters = useMemo(() => parseFilters(searchParams), [searchParams]);

  const [draft, setDraft] = useState<SearchFilters>(() => ({ ...filters }));
  const syncDraftFromUrl = useCallback(() => setDraft({ ...filters }), [filters]);

  const search = useSearchCars(filters);
  const neighborhoods = useNeighborhoods();
  const cars = flattenCars(search.data);
  const total = searchTotal(search.data);

  const applyFilters = (next: SearchFilters) => {
    setSearchParams(filtersToSearchParams(next), { replace: true });
  };

  const onSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    applyFilters(draft);
  };

  const onClear = () => {
    setDraft({});
    setSearchParams({}, { replace: true });
  };

  const displayError = search.isError ? t('browse.search.loadError') : null;

  return (
    <main className="container py-4">
      <h1 className="h3 fw-semibold mb-4">{t('browse.search.title')}</h1>

      <section className="bg-white rounded-4 shadow-sm p-4 mb-4">
        <h2 className="h6 fw-semibold mb-3">{t('browse.search.filters')}</h2>
        <form onSubmit={onSubmit} onReset={onClear}>
          <div className="row g-3">
            <div className="col-12 col-md-6 col-lg-4">
              <label className="form-label small" htmlFor="searchQ">{t('browse.search.q')}</label>
              <input
                id="searchQ"
                type="search"
                className="form-control form-control-sm"
                value={draft.q ?? ''}
                onChange={(e) => setDraft((d) => ({ ...d, q: e.target.value || undefined }))}
              />
            </div>
            <div className="col-6 col-md-3 col-lg-2">
              <label className="form-label small" htmlFor="searchFrom">{t('browse.search.from')}</label>
              <input
                id="searchFrom"
                type="date"
                className="form-control form-control-sm"
                value={draft.from ?? ''}
                onChange={(e) => setDraft((d) => ({ ...d, from: e.target.value || undefined }))}
              />
            </div>
            <div className="col-6 col-md-3 col-lg-2">
              <label className="form-label small" htmlFor="searchUntil">{t('browse.search.until')}</label>
              <input
                id="searchUntil"
                type="date"
                className="form-control form-control-sm"
                value={draft.until ?? ''}
                onChange={(e) => setDraft((d) => ({ ...d, until: e.target.value || undefined }))}
              />
            </div>
            <div className="col-6 col-md-3 col-lg-2">
              <label className="form-label small" htmlFor="searchCategory">{t('browse.search.category')}</label>
              <select
                id="searchCategory"
                className="form-select form-select-sm"
                value={draft.category ?? ''}
                onChange={(e) =>
                  setDraft((d) => ({
                    ...d,
                    category: (e.target.value || undefined) as CarType | undefined,
                  }))
                }
              >
                <option value="">{t('browse.search.any')}</option>
                {CAR_TYPES.map((c) => (
                  <option key={c} value={c}>{t(`browse.carType.${c}`)}</option>
                ))}
              </select>
            </div>
            <div className="col-6 col-md-3 col-lg-2">
              <label className="form-label small" htmlFor="searchTransmission">{t('browse.search.transmission')}</label>
              <select
                id="searchTransmission"
                className="form-select form-select-sm"
                value={draft.transmission ?? ''}
                onChange={(e) =>
                  setDraft((d) => ({
                    ...d,
                    transmission: (e.target.value || undefined) as Transmission | undefined,
                  }))
                }
              >
                <option value="">{t('browse.search.any')}</option>
                {TRANSMISSIONS.map((tr) => (
                  <option key={tr} value={tr}>{t(`browse.transmission.${tr}`)}</option>
                ))}
              </select>
            </div>
            <div className="col-6 col-md-3 col-lg-2">
              <label className="form-label small" htmlFor="searchPowertrain">{t('browse.search.powertrain')}</label>
              <select
                id="searchPowertrain"
                className="form-select form-select-sm"
                value={draft.powertrain ?? ''}
                onChange={(e) =>
                  setDraft((d) => ({
                    ...d,
                    powertrain: (e.target.value || undefined) as Powertrain | undefined,
                  }))
                }
              >
                <option value="">{t('browse.search.any')}</option>
                {POWERTRAINS.map((p) => (
                  <option key={p} value={p}>{t(`browse.powertrain.${p}`)}</option>
                ))}
              </select>
            </div>
            <div className="col-6 col-md-3 col-lg-2">
              <label className="form-label small" htmlFor="searchNeighborhood">{t('browse.search.neighborhood')}</label>
              <select
                id="searchNeighborhood"
                className="form-select form-select-sm"
                value={draft.neighborhoodId ?? ''}
                onChange={(e) =>
                  setDraft((d) => ({
                    ...d,
                    neighborhoodId: e.target.value ? Number(e.target.value) : undefined,
                  }))
                }
              >
                <option value="">{t('browse.search.any')}</option>
                {(neighborhoods.data ?? []).map((n) => {
                  const nid = idFromUri(n.links.self);
                  return nid ? (
                    <option key={n.links.self} value={nid}>{n.name}</option>
                  ) : null;
                })}
              </select>
            </div>
            <div className="col-6 col-md-3 col-lg-2">
              <label className="form-label small" htmlFor="searchPriceMin">{t('browse.search.priceMin')}</label>
              <input
                id="searchPriceMin"
                type="number"
                min={0}
                className="form-control form-control-sm"
                value={draft.priceMin ?? ''}
                onChange={(e) =>
                  setDraft((d) => ({
                    ...d,
                    priceMin: e.target.value ? Number(e.target.value) : undefined,
                  }))
                }
              />
            </div>
            <div className="col-6 col-md-3 col-lg-2">
              <label className="form-label small" htmlFor="searchPriceMax">{t('browse.search.priceMax')}</label>
              <input
                id="searchPriceMax"
                type="number"
                min={0}
                className="form-control form-control-sm"
                value={draft.priceMax ?? ''}
                onChange={(e) =>
                  setDraft((d) => ({
                    ...d,
                    priceMax: e.target.value ? Number(e.target.value) : undefined,
                  }))
                }
              />
            </div>
            <div className="col-6 col-md-3 col-lg-2">
              <label className="form-label small" htmlFor="searchRating">{t('browse.search.rating')}</label>
              <input
                id="searchRating"
                type="number"
                min={0}
                max={5}
                step={0.5}
                className="form-control form-control-sm"
                value={draft.rating ?? ''}
                onChange={(e) =>
                  setDraft((d) => ({
                    ...d,
                    rating: e.target.value ? Number(e.target.value) : undefined,
                  }))
                }
              />
            </div>
            <div className="col-6 col-md-3 col-lg-2">
              <label className="form-label small" htmlFor="searchSort">{t('browse.search.sort')}</label>
              <select
                id="searchSort"
                className="form-select form-select-sm"
                value={draft.sort ?? 'recent'}
                onChange={(e) =>
                  setDraft((d) => ({ ...d, sort: (e.target.value || undefined) as CarSort | undefined }))
                }
              >
                {CAR_SORTS.map((s) => (
                  <option key={s} value={s}>{t(`browse.sort.${s}`)}</option>
                ))}
              </select>
            </div>
          </div>
          <div className="d-flex flex-wrap gap-2 mt-3">
            <button type="submit" className="btn btn-primary btn-sm">{t('browse.home.searchCta')}</button>
            <button type="reset" className="btn btn-outline-secondary btn-sm">{t('browse.search.clear')}</button>
            <button type="button" className="btn btn-link btn-sm" onClick={syncDraftFromUrl}>
              {t('app.cancel', { defaultValue: 'Descartar cambios' })}
            </button>
          </div>
        </form>
      </section>

      {total != null ? (
        <p className="text-secondary small mb-3">{t('browse.search.results', { count: total })}</p>
      ) : null}

      {displayError ? <div className="alert alert-danger" role="alert">{displayError}</div> : null}
      {search.isLoading ? <p className="text-secondary" role="status">{t('app.loading')}</p> : null}

      {!search.isLoading && cars.length === 0 ? (
        <p className="text-secondary">{t('browse.search.empty')}</p>
      ) : null}

      {cars.length > 0 ? (
        <div className="row row-cols-1 row-cols-md-2 row-cols-lg-3 g-3 gy-4">
          {cars.map((car) => (
            <div key={car.links.self} className="col d-flex justify-content-center">
              <BrowseCarCard car={car} />
            </div>
          ))}
        </div>
      ) : null}

      {search.hasNextPage ? (
        <div className="text-center mt-4">
          <button
            type="button"
            className="btn btn-outline-primary"
            disabled={search.isFetchingNextPage}
            onClick={() => void search.fetchNextPage()}
          >
            {search.isFetchingNextPage ? t('app.loading') : t('browse.search.loadMore')}
          </button>
        </div>
      ) : null}
    </main>
  );
}
