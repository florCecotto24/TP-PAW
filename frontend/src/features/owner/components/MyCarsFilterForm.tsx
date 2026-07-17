import { type FormEvent, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ExploreFilterDropdown } from '../../../components/ryden';
import { paths } from '../../../routes/paths';
import { myCarsFilterOptions, type MyCarsFilters } from '../myCarsFilters';

export interface MyCarsFilterFormProps {
  initial: MyCarsFilters;
  showClear: boolean;
  onSubmit: (filters: MyCarsFilters) => void;
}

type MultiFilters = Record<string, string[]>;

function toMulti(filters: MyCarsFilters): MultiFilters {
  const out: MultiFilters = {};
  if (filters.status?.length) out.listingStatus = [...filters.status];
  if (filters.category?.length) out.category = [...filters.category];
  if (filters.transmission?.length) out.transmission = [...filters.transmission];
  if (filters.powertrain?.length) out.powertrain = [...filters.powertrain];
  if (filters.rating?.length) out.rating = [...filters.rating];
  return out;
}

function fromMulti(base: MyCarsFilters, multi: MultiFilters): MyCarsFilters {
  return {
    ...base,
    status: multi.listingStatus?.length ? (multi.listingStatus as MyCarsFilters['status']) : undefined,
    category: multi.category?.length ? (multi.category as MyCarsFilters['category']) : undefined,
    transmission: multi.transmission?.length
      ? (multi.transmission as MyCarsFilters['transmission'])
      : undefined,
    powertrain: multi.powertrain?.length ? (multi.powertrain as MyCarsFilters['powertrain']) : undefined,
    rating: multi.rating?.length ? multi.rating : undefined,
  };
}

export default function MyCarsFilterForm({ initial, showClear, onSubmit }: MyCarsFilterFormProps) {
  const { t } = useTranslation();
  const options = myCarsFilterOptions(t);

  const [query, setQuery] = useState(initial.q ?? '');
  const [priceMin, setPriceMin] = useState(initial.priceMin != null ? String(initial.priceMin) : '');
  const [priceMax, setPriceMax] = useState(initial.priceMax != null ? String(initial.priceMax) : '');
  const [selectedFilters, setSelectedFilters] = useState<MultiFilters>(() => toMulti(initial));

  const initialKey = JSON.stringify(initial);
  useEffect(() => {
    setQuery(initial.q ?? '');
    setPriceMin(initial.priceMin != null ? String(initial.priceMin) : '');
    setPriceMax(initial.priceMax != null ? String(initial.priceMax) : '');
    setSelectedFilters(toMulti(initial));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [initialKey]);

  const hasActivePrice = Boolean(priceMin || priceMax);

  const buildFilters = (): MyCarsFilters =>
    fromMulti(
      {
        q: query.trim() || undefined,
        priceMin: priceMin ? Number(priceMin) : undefined,
        priceMax: priceMax ? Number(priceMax) : undefined,
        sort: initial.sort,
      },
      selectedFilters,
    );

  const handleSubmit = (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    onSubmit(buildFilters());
  };

  return (
    <form id="myListingsFilterForm" className="mb-4" method="get" action={paths.myCars} onSubmit={handleSubmit}>
      <div className="d-flex justify-content-center mb-3">
        <div className="d-flex align-items-center gap-2 w-100" style={{ maxWidth: 600 }}>
          <div className="d-flex align-items-center ryden-search-pill rounded-4 px-3 py-1 flex-grow-1 gap-2">
            <i className="bi bi-search text-secondary flex-shrink-0" aria-hidden="true"></i>
            <input
              type="search"
              className="form-control"
              id="myListings_q"
              name="q"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder={t('myCars.filter.query.placeholder')}
            />
          </div>
          <button type="submit" className="btn btn-primary rounded-3 flex-shrink-0">
            {t('myCars.filter.search')}
          </button>
          {showClear ? (
            <Link to={paths.myCars} className="btn btn-outline-secondary flex-shrink-0">
              {t('search.filters.clear')}
            </Link>
          ) : null}
        </div>
      </div>

      <div className="d-flex justify-content-center">
        <div className="d-flex flex-wrap align-items-center justify-content-center gap-0 pt-1">
          <ExploreFilterDropdown
            filterLabel={t('myCars.filter.status')}
            paramName="listingStatus"
            ariaGroup="lst-status"
            options={options.status}
            selectedKeys={selectedFilters.listingStatus ?? []}
            onChange={(param, values) => setSelectedFilters((prev) => ({ ...prev, [param]: values }))}
          />
          <ExploreFilterDropdown
            filterLabel={t('search.filter.category.label')}
            paramName="category"
            ariaGroup="lst-category"
            options={options.category}
            selectedKeys={selectedFilters.category ?? []}
            onChange={(param, values) => setSelectedFilters((prev) => ({ ...prev, [param]: values }))}
          />
          <ExploreFilterDropdown
            filterLabel={t('search.filter.transmission.label')}
            paramName="transmission"
            ariaGroup="lst-transmission"
            options={options.transmission}
            selectedKeys={selectedFilters.transmission ?? []}
            onChange={(param, values) => setSelectedFilters((prev) => ({ ...prev, [param]: values }))}
          />
          <ExploreFilterDropdown
            filterLabel={t('search.filter.powertrain.label')}
            paramName="powertrain"
            ariaGroup="lst-powertrain"
            options={options.powertrain}
            selectedKeys={selectedFilters.powertrain ?? []}
            onChange={(param, values) => setSelectedFilters((prev) => ({ ...prev, [param]: values }))}
          />
          <div className="dropdown explore-filter-dropdown mx-1 my-1">
            <button
              className="btn btn-light border dropdown-toggle rounded-4 d-inline-flex align-items-center gap-1"
              type="button"
              data-bs-toggle="dropdown"
              data-bs-auto-close="outside"
            >
              <span className="explore-filter-dropdown__label">{t('search.filter.price.label')}</span>
              <span
                className={`badge text-bg-primary rounded-pill${hasActivePrice ? '' : ' d-none'}`}
                data-filter-count="true"
              >
                1
              </span>
            </button>
            <div className="dropdown-menu p-3" style={{ minWidth: 200 }}>
              <div className="mb-2">
                <label className="form-label small mb-1" htmlFor="myListings_priceMin">
                  {t('search.filter.price.min')}
                </label>
                <input
                  type="number"
                  className="form-control form-control-sm"
                  id="myListings_priceMin"
                  name="priceMin"
                  min={0}
                  step={1}
                  value={priceMin}
                  onChange={(e) => setPriceMin(e.target.value)}
                />
              </div>
              <div>
                <label className="form-label small mb-1" htmlFor="myListings_priceMax">
                  {t('search.filter.price.max')}
                </label>
                <input
                  type="number"
                  className="form-control form-control-sm"
                  id="myListings_priceMax"
                  name="priceMax"
                  min={0}
                  step={1}
                  value={priceMax}
                  onChange={(e) => setPriceMax(e.target.value)}
                />
              </div>
            </div>
          </div>
          <ExploreFilterDropdown
            filterLabel={t('search.filter.rating')}
            paramName="rating"
            ariaGroup="lst-rating"
            options={options.rating}
            selectedKeys={selectedFilters.rating ?? []}
            onChange={(param, values) => setSelectedFilters((prev) => ({ ...prev, [param]: values }))}
          />
        </div>
      </div>
    </form>
  );
}
