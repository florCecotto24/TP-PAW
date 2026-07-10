import { useRef, type FormEvent, type ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import ExploreFilterDropdown, { type ExploreFilterOption } from './ExploreFilterDropdown';
import NeighborhoodPicker, { type NeighborhoodOption } from './NeighborhoodPicker';
import { useAnimatedSearchPlaceholder } from './useAnimatedSearchPlaceholder';
import { paths } from '../../../routes/paths';

export interface SearchWithFiltersProps {
  formId?: string;
  formClass?: string;
  actionPath?: string;
  showFilters?: boolean;
  categoryFilterOptions: ExploreFilterOption[];
  transmissionFilterOptions: ExploreFilterOption[];
  powertrainFilterOptions: ExploreFilterOption[];
  ratingFilterOptions?: ExploreFilterOption[];
  priceMarketFilterOptions?: ExploreFilterOption[];
  neighborhoodList?: NeighborhoodOption[];
  query?: string;
  from?: string;
  until?: string;
  selectedFilters?: Record<string, string[]>;
  selectedNeighborhoodIds?: Array<number | string>;
  priceMin?: string;
  priceMax?: string;
  flexible?: boolean;
  flexMonth?: string;
  flexDays?: string;
  allowFlexibleSearch?: boolean;
  showClearFilters?: boolean;
  clearFiltersHref?: string;
  onClearFilters?: () => void;
  datePickerSlot?: ReactNode;
  flexControlsSlot?: ReactNode;
  onSubmit?: (e: FormEvent<HTMLFormElement>) => void;
  onQueryChange?: (value: string) => void;
  onFilterChange?: (paramName: string, values: string[]) => void;
  onNeighborhoodChange?: (ids: Array<number | string>) => void;
  onPriceMinChange?: (value: string) => void;
  onPriceMaxChange?: (value: string) => void;
  onFlexibleChange?: (enabled: boolean) => void;
}

/**
 * Espejo estructural de {@code ryden-search:searchWithFilters}.
 * Los date pickers y controles flexibles se inyectan vía slots para reutilizar Flatpickr.
 */
export default function SearchWithFilters({
  formId = 'exploreSearchForm',
  formClass = 'search-menu w-100',
  actionPath = paths.search,
  showFilters = true,
  categoryFilterOptions,
  transmissionFilterOptions,
  powertrainFilterOptions,
  ratingFilterOptions = [],
  priceMarketFilterOptions = [],
  neighborhoodList = [],
  query = '',
  from = '',
  until = '',
  selectedFilters = {},
  selectedNeighborhoodIds = [],
  priceMin = '',
  priceMax = '',
  flexible = false,
  allowFlexibleSearch = false,
  showClearFilters = false,
  clearFiltersHref,
  onClearFilters,
  datePickerSlot,
  flexControlsSlot,
  onSubmit,
  onQueryChange,
  onFilterChange,
  onNeighborhoodChange,
  onPriceMinChange,
  onPriceMaxChange,
  onFlexibleChange,
}: SearchWithFiltersProps) {
  const { t } = useTranslation();
  const hasActivePrice = Boolean(priceMin || priceMax);
  const queryRef = useRef<HTMLInputElement>(null);
  const { placeholder, animating } = useAnimatedSearchPlaceholder(queryRef, query);

  return (
    <form id={formId} className={formClass} method="get" action={actionPath} onSubmit={onSubmit}>
      <div className="container">
        <div className="bg-white rounded-4 px-3 py-2 shadow-sm">
          <div className="d-flex align-items-stretch gap-2 flex-wrap">
            <div
              className={`flex-grow-1${animating ? ' placeholder-anim' : ''}`}
              style={{ minWidth: '12rem' }}
            >
              <label className="form-label small text-secondary text-uppercase mb-1" htmlFor={`search_query_${formId}`}>
                {t('searchBar.query.label')}
              </label>
              <input
                ref={queryRef}
                type="text"
                className="form-control form-control-sm border-0 shadow-none"
                aria-label={t('searchBar.query.ariaLabel')}
                id={`search_query_${formId}`}
                name="query"
                value={query}
                placeholder={placeholder}
                onChange={(e) => onQueryChange?.(e.target.value)}
              />
            </div>

            {showFilters && neighborhoodList.length > 0 ? (
              <>
                <div className="vr flex-shrink-0 d-none d-md-block" />
                <div className="flex-grow-1" style={{ minWidth: '10rem', maxWidth: '16rem' }}>
                  <NeighborhoodPicker
                    pickerId={formId}
                    neighborhoodList={neighborhoodList}
                    anyLabel={t('search.filter.neighborhood.any')}
                    searchPlaceholder={t('search.filter.neighborhood.search')}
                    selectFieldLabel={t('search.filter.neighborhood.label')}
                    toggleAriaLabel={t('search.filter.neighborhood.label')}
                    allowMultiple
                    selectedNeighborhoodIds={selectedNeighborhoodIds}
                    formId={formId}
                    searchBarInline
                    wrapExtraClass="w-100"
                    onSelectionChange={onNeighborhoodChange}
                  />
                </div>
              </>
            ) : null}

            <div className="vr flex-shrink-0 d-none d-md-block" />

            <div className="flex-grow-1" style={{ minWidth: '14rem' }}>
              <div className="ryden-date-stack">
                <div className={flexible ? 'js-exact-date-range js-date-mode-hidden' : 'js-exact-date-range'}>
                  {datePickerSlot}
                </div>
                {allowFlexibleSearch ? (
                  <div className={flexible ? 'js-flexible-controls' : 'js-flexible-controls js-date-mode-hidden'}>
                    {flexControlsSlot}
                  </div>
                ) : null}
              </div>
              <input type="hidden" name="from" id={`search_from_hidden_${formId}`} value={from} readOnly />
              <input type="hidden" name="until" id={`search_until_hidden_${formId}`} value={until} readOnly />
              {allowFlexibleSearch ? (
                <div className="form-check form-switch d-flex align-items-center gap-2 mt-2 ps-0">
                  <input
                    className="form-check-input flex-shrink-0 ms-0 js-flexible-toggle"
                    type="checkbox"
                    role="switch"
                    id={`search_flex_${formId}`}
                    name="flexible"
                    value="true"
                    style={{ cursor: 'pointer' }}
                    checked={flexible}
                    onChange={(e) => onFlexibleChange?.(e.target.checked)}
                  />
                  <label className="form-check-label small text-secondary mb-0" htmlFor={`search_flex_${formId}`}>
                    {t('search.flexible.toggle')}
                  </label>
                </div>
              ) : null}
            </div>

            <div className="vr flex-shrink-0 d-none d-md-block" />

            <div className="ryden-search-submit ms-md-auto flex-shrink-0 align-self-center">
              <button
                type="submit"
                className="btn btn-primary rounded-3 p-2"
                aria-label={t('searchBar.submit.ariaLabel')}
              >
                <i className="bi bi-search fs-5 search-btn" aria-hidden="true"></i>
              </button>
            </div>
          </div>

          {showFilters ? (
            <div
              className="border-top mt-2 pt-2 d-flex flex-wrap align-items-center gap-2"
              role="navigation"
              aria-label={t('search.filters.ariaLabel')}
            >
              {showClearFilters && (onClearFilters || clearFiltersHref) ? (
                onClearFilters ? (
                  <button
                    type="button"
                    className="btn btn-outline-primary btn-action btn-action-md flex-shrink-0"
                    onClick={onClearFilters}
                  >
                    {t('search.filters.clear')}
                  </button>
                ) : (
                  <a
                    href={clearFiltersHref}
                    className="btn btn-outline-primary btn-action btn-action-md flex-shrink-0"
                  >
                    {t('search.filters.clear')}
                  </a>
                )
              ) : null}
              <div className="d-flex flex-wrap align-items-center justify-content-center gap-1 flex-grow-1">
              <ExploreFilterDropdown
                filterLabel={t('search.filter.category.label')}
                helperText={t('search.filter.category.helper')}
                paramName="category"
                ariaGroup="category"
                options={categoryFilterOptions}
                selectedKeys={selectedFilters.category ?? []}
                onChange={onFilterChange}
              />
              <ExploreFilterDropdown
                filterLabel={t('search.filter.transmission.label')}
                helperText={t('search.filter.transmission.helper')}
                paramName="transmission"
                ariaGroup="transmission"
                options={transmissionFilterOptions}
                selectedKeys={selectedFilters.transmission ?? []}
                onChange={onFilterChange}
              />
              <ExploreFilterDropdown
                filterLabel={t('search.filter.powertrain.label')}
                helperText={t('search.filter.powertrain.helper')}
                paramName="powertrain"
                ariaGroup="powertrain"
                options={powertrainFilterOptions}
                selectedKeys={selectedFilters.powertrain ?? []}
                onChange={onFilterChange}
              />
              <div className="dropdown explore-filter-dropdown mx-1 my-1">
                <button
                  className="btn btn-light border dropdown-toggle rounded-4 d-inline-flex align-items-center gap-1"
                  type="button"
                  data-bs-toggle="dropdown"
                  data-bs-auto-close="outside"
                  aria-expanded="false"
                >
                  <span className="explore-filter-dropdown__label">{t('search.filter.price.label')}</span>
                  <span
                    className={`badge text-bg-primary rounded-pill${!hasActivePrice ? ' d-none' : ''}`}
                    data-filter-count="true"
                  >
                    1
                  </span>
                </button>
                <div className="dropdown-menu p-3" style={{ minWidth: 200 }}>
                  <div className="mb-2">
                    <label className="form-label small mb-1">{t('search.filter.price.min')}</label>
                    <input
                      type="number"
                      className="form-control form-control-sm js-price-input"
                      name="priceMin"
                      min={0}
                      step={1}
                      value={priceMin}
                      onChange={(e) => onPriceMinChange?.(e.target.value)}
                    />
                  </div>
                  <div>
                    <label className="form-label small mb-1">{t('search.filter.price.max')}</label>
                    <input
                      type="number"
                      className="form-control form-control-sm js-price-input"
                      name="priceMax"
                      min={0}
                      step={1}
                      value={priceMax}
                      onChange={(e) => onPriceMaxChange?.(e.target.value)}
                    />
                  </div>
                </div>
              </div>
              {priceMarketFilterOptions.length > 0 ? (
                <ExploreFilterDropdown
                  filterLabel={t('search.filter.priceMarket.label')}
                  helperText={t('search.filter.priceMarket.helper')}
                  paramName="priceMarket"
                  ariaGroup="priceMarket"
                  options={priceMarketFilterOptions}
                  selectedKeys={selectedFilters.priceMarket ?? []}
                  onChange={onFilterChange}
                />
              ) : null}
              {ratingFilterOptions.length > 0 ? (
                <ExploreFilterDropdown
                  filterLabel={t('search.filter.rating')}
                  paramName="rating"
                  ariaGroup="rating"
                  options={ratingFilterOptions}
                  selectedKeys={selectedFilters.rating ?? []}
                  onChange={onFilterChange}
                />
              ) : null}
              </div>
            </div>
          ) : null}
        </div>
      </div>
    </form>
  );
}
