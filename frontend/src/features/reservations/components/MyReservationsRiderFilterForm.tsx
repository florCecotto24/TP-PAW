import { type FormEvent, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ExploreFilterDropdown } from '../../../components/ryden';
import { paths } from '../../../routes/paths';
import {
  riderCarFilterOptions,
  reservationStatusFilterOptions,
  type RiderReservationFilters,
} from '../riderReservationFilters';

export interface MyReservationsRiderFilterFormProps {
  initial: RiderReservationFilters;
  showClear: boolean;
  onSubmit: (filters: RiderReservationFilters) => void;
}

type MultiFilters = Record<string, string[]>;

function toMulti(filters: RiderReservationFilters): MultiFilters {
  const out: MultiFilters = {};
  if (filters.riderStatus?.length) out.riderStatus = [...filters.riderStatus];
  if (filters.category?.length) out.category = [...filters.category];
  if (filters.transmission?.length) out.transmission = [...filters.transmission];
  if (filters.powertrain?.length) out.powertrain = [...filters.powertrain];
  if (filters.rating?.length) out.rating = [...filters.rating];
  return out;
}

function fromMulti(base: RiderReservationFilters, multi: MultiFilters): RiderReservationFilters {
  return {
    ...base,
    riderStatus: multi.riderStatus?.length
      ? (multi.riderStatus as RiderReservationFilters['riderStatus'])
      : undefined,
    category: multi.category?.length ? (multi.category as RiderReservationFilters['category']) : undefined,
    transmission: multi.transmission?.length
      ? (multi.transmission as RiderReservationFilters['transmission'])
      : undefined,
    powertrain: multi.powertrain?.length
      ? (multi.powertrain as RiderReservationFilters['powertrain'])
      : undefined,
    rating: multi.rating?.length ? multi.rating : undefined,
  };
}

export default function MyReservationsRiderFilterForm({
  initial,
  showClear,
  onSubmit,
}: MyReservationsRiderFilterFormProps) {
  const { t } = useTranslation();
  const carOptions = riderCarFilterOptions(t);

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

  const buildFilters = (): RiderReservationFilters =>
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
    <form id="myReservationsRiderFilterForm" className="mb-3" method="get" action={paths.myReservations} onSubmit={handleSubmit}>
      <div className="d-flex justify-content-center mb-3">
        <div className="d-flex align-items-center gap-2 w-100" style={{ maxWidth: 600 }}>
          <div className="d-flex align-items-center ryden-search-pill rounded-4 px-3 py-1 flex-grow-1 gap-2">
            <i className="bi bi-search text-secondary flex-shrink-0" aria-hidden="true"></i>
            <input
              type="search"
              className="form-control"
              id="myReservations_q"
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
            <Link to={paths.myReservations} className="btn btn-outline-secondary flex-shrink-0">
              {t('search.filters.clear')}
            </Link>
          ) : null}
        </div>
      </div>
      <div className="d-flex justify-content-center mb-3">
        <div className="d-flex flex-wrap align-items-center justify-content-center gap-0 pt-1">
          <ExploreFilterDropdown
            filterLabel={t('myReservations.filter.status')}
            paramName="riderStatus"
            ariaGroup="rider-status"
            options={reservationStatusFilterOptions(t)}
            selectedKeys={selectedFilters.riderStatus ?? []}
            onChange={(param, values) => setSelectedFilters((prev) => ({ ...prev, [param]: values }))}
          />
          <ExploreFilterDropdown
            filterLabel={t('search.filter.category.label')}
            paramName="category"
            ariaGroup="rider-category"
            options={carOptions.category}
            selectedKeys={selectedFilters.category ?? []}
            onChange={(param, values) => setSelectedFilters((prev) => ({ ...prev, [param]: values }))}
          />
          <ExploreFilterDropdown
            filterLabel={t('search.filter.transmission.label')}
            paramName="transmission"
            ariaGroup="rider-transmission"
            options={carOptions.transmission}
            selectedKeys={selectedFilters.transmission ?? []}
            onChange={(param, values) => setSelectedFilters((prev) => ({ ...prev, [param]: values }))}
          />
          <ExploreFilterDropdown
            filterLabel={t('search.filter.powertrain.label')}
            paramName="powertrain"
            ariaGroup="rider-powertrain"
            options={carOptions.powertrain}
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
                <label className="form-label small mb-1" htmlFor="myReservations_priceMin">
                  {t('search.filter.price.min')}
                </label>
                <input
                  type="number"
                  className="form-control form-control-sm"
                  id="myReservations_priceMin"
                  name="priceMin"
                  min={0}
                  step={1}
                  value={priceMin}
                  onChange={(e) => setPriceMin(e.target.value)}
                />
              </div>
              <div>
                <label className="form-label small mb-1" htmlFor="myReservations_priceMax">
                  {t('search.filter.price.max')}
                </label>
                <input
                  type="number"
                  className="form-control form-control-sm"
                  id="myReservations_priceMax"
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
            ariaGroup="rider-rating"
            options={carOptions.rating}
            selectedKeys={selectedFilters.rating ?? []}
            onChange={(param, values) => setSelectedFilters((prev) => ({ ...prev, [param]: values }))}
          />
        </div>
      </div>
    </form>
  );
}
