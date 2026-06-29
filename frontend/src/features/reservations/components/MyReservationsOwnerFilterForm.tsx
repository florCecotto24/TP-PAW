import { type FormEvent, useState } from 'react';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ExploreFilterDropdown } from '../../../components/ryden';
import { ownerReservationsCar, paths } from '../../../routes/paths';
import {
  ownerCarFilterOptions,
  type OwnerReservationFilters,
} from '../ownerReservationFilters';

export interface MyReservationsOwnerFilterFormProps {
  initial: OwnerReservationFilters;
  showClear: boolean;
  carId?: string | null;
  onSubmit: (filters: OwnerReservationFilters) => void;
}

type MultiFilters = Record<string, string[]>;

function toMulti(filters: OwnerReservationFilters): MultiFilters {
  const out: MultiFilters = {};
  if (filters.ownerStatus?.length) out.ownerStatus = [...filters.ownerStatus];
  if (filters.ownerCategory?.length) out.ownerCategory = [...filters.ownerCategory];
  if (filters.ownerTransmission?.length) out.ownerTransmission = [...filters.ownerTransmission];
  if (filters.ownerPowertrain?.length) out.ownerPowertrain = [...filters.ownerPowertrain];
  if (filters.ownerRating?.length) out.ownerRating = [...filters.ownerRating];
  return out;
}

function fromMulti(base: OwnerReservationFilters, multi: MultiFilters): OwnerReservationFilters {
  return {
    ...base,
    ownerStatus: multi.ownerStatus?.length
      ? (multi.ownerStatus as OwnerReservationFilters['ownerStatus'])
      : undefined,
    ownerCategory: multi.ownerCategory?.length
      ? (multi.ownerCategory as OwnerReservationFilters['ownerCategory'])
      : undefined,
    ownerTransmission: multi.ownerTransmission?.length
      ? (multi.ownerTransmission as OwnerReservationFilters['ownerTransmission'])
      : undefined,
    ownerPowertrain: multi.ownerPowertrain?.length
      ? (multi.ownerPowertrain as OwnerReservationFilters['ownerPowertrain'])
      : undefined,
    ownerRating: multi.ownerRating?.length ? multi.ownerRating : undefined,
  };
}

export default function MyReservationsOwnerFilterForm({
  initial,
  showClear,
  carId,
  onSubmit,
}: MyReservationsOwnerFilterFormProps) {
  const { t } = useTranslation();
  const carOptions = ownerCarFilterOptions(t);
  const scopedToCar = carId != null && carId !== '';
  const formAction = scopedToCar ? ownerReservationsCar(carId) : paths.ownerReservations;
  const clearHref = formAction;

  const [query, setQuery] = useState(initial.ownerQ ?? '');
  const [priceMin, setPriceMin] = useState(
    initial.ownerPriceMin != null ? String(initial.ownerPriceMin) : '',
  );
  const [priceMax, setPriceMax] = useState(
    initial.ownerPriceMax != null ? String(initial.ownerPriceMax) : '',
  );
  const [selectedFilters, setSelectedFilters] = useState<MultiFilters>(() => toMulti(initial));

  const hasActivePrice = Boolean(priceMin || priceMax);

  const buildFilters = (): OwnerReservationFilters =>
    fromMulti(
      {
        ownerQ: query.trim() || undefined,
        ownerPriceMin: priceMin ? Number(priceMin) : undefined,
        ownerPriceMax: priceMax ? Number(priceMax) : undefined,
        ownerSort: initial.ownerSort,
      },
      selectedFilters,
    );

  const handleSubmit = (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    onSubmit(buildFilters());
  };

  return (
    <form
      id="ownerResAllFilterForm"
      className="mb-3"
      method="get"
      action={formAction}
      onSubmit={handleSubmit}
    >
      {!scopedToCar ? (
        <div className="d-flex justify-content-center mb-3">
          <div className="d-flex align-items-center gap-2 w-100" style={{ maxWidth: 600 }}>
            <div className="d-flex align-items-center ryden-search-pill rounded-4 px-3 py-1 flex-grow-1 gap-2">
              <i className="bi bi-search text-secondary flex-shrink-0" aria-hidden="true"></i>
              <input
                type="search"
                className="form-control"
                id="ownerRes_q"
                name="ownerQ"
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder={t('myCars.filter.query.placeholder')}
              />
            </div>
            <button type="submit" className="btn btn-primary rounded-3 flex-shrink-0">
              {t('myCars.filter.search')}
            </button>
            {showClear ? (
              <Link to={clearHref} className="btn btn-outline-secondary flex-shrink-0">
                {t('search.filters.clear')}
              </Link>
            ) : null}
          </div>
        </div>
      ) : null}

      <div className="d-flex justify-content-center mb-3">
        <div className="d-flex flex-wrap align-items-center justify-content-center gap-0 pt-1">
          <ExploreFilterDropdown
            filterLabel={t('myReservations.filter.status')}
            paramName="ownerStatus"
            ariaGroup="own-status"
            options={carOptions.status}
            selectedKeys={selectedFilters.ownerStatus ?? []}
            onChange={(param, values) => setSelectedFilters((prev) => ({ ...prev, [param]: values }))}
          />
          {!scopedToCar ? (
            <>
              <ExploreFilterDropdown
                filterLabel={t('search.filter.category.label')}
                paramName="ownerCategory"
                ariaGroup="own-category"
                options={carOptions.category}
                selectedKeys={selectedFilters.ownerCategory ?? []}
                onChange={(param, values) =>
                  setSelectedFilters((prev) => ({ ...prev, [param]: values }))
                }
              />
              <ExploreFilterDropdown
                filterLabel={t('search.filter.transmission.label')}
                paramName="ownerTransmission"
                ariaGroup="own-transmission"
                options={carOptions.transmission}
                selectedKeys={selectedFilters.ownerTransmission ?? []}
                onChange={(param, values) =>
                  setSelectedFilters((prev) => ({ ...prev, [param]: values }))
                }
              />
              <ExploreFilterDropdown
                filterLabel={t('search.filter.powertrain.label')}
                paramName="ownerPowertrain"
                ariaGroup="own-powertrain"
                options={carOptions.powertrain}
                selectedKeys={selectedFilters.ownerPowertrain ?? []}
                onChange={(param, values) =>
                  setSelectedFilters((prev) => ({ ...prev, [param]: values }))
                }
              />
            </>
          ) : null}
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
                <label className="form-label small mb-1" htmlFor="ownerRes_priceMin">
                  {t('search.filter.price.min')}
                </label>
                <input
                  type="number"
                  className="form-control form-control-sm"
                  id="ownerRes_priceMin"
                  name="ownerPriceMin"
                  min={0}
                  step={1}
                  value={priceMin}
                  onChange={(e) => setPriceMin(e.target.value)}
                />
              </div>
              <div>
                <label className="form-label small mb-1" htmlFor="ownerRes_priceMax">
                  {t('search.filter.price.max')}
                </label>
                <input
                  type="number"
                  className="form-control form-control-sm"
                  id="ownerRes_priceMax"
                  name="ownerPriceMax"
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
            paramName="ownerRating"
            ariaGroup="own-rating"
            options={carOptions.rating}
            selectedKeys={selectedFilters.ownerRating ?? []}
            onChange={(param, values) => setSelectedFilters((prev) => ({ ...prev, [param]: values }))}
          />
          {scopedToCar ? (
            <>
              <button type="submit" className="btn btn-primary rounded-4 mx-1 my-1 flex-shrink-0">
                {t('myCars.filter.search')}
              </button>
              {showClear ? (
                <Link
                  to={clearHref}
                  className="btn btn-outline-secondary rounded-4 mx-1 my-1 flex-shrink-0"
                >
                  {t('search.filters.clear')}
                </Link>
              ) : null}
            </>
          ) : null}
        </div>
      </div>
    </form>
  );
}
