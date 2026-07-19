import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { SearchWithFilters } from '../../../components/ryden';
import { idFromUri } from '../../../api/uri';
import { paths } from '../../../routes/paths';
import {
  categoryFilterOptions,
  powertrainFilterOptions,
  priceMarketFilterOptions,
  ratingFilterOptions,
  transmissionFilterOptions,
} from '../exploreSearch';
import { useNeighborhoods } from '../hooks';
import { useSearchDatePickers } from '../hooks/useSearchDatePickers';
import FlexibleDateControls from './FlexibleDateControls';
import { filtersToSearchParams, isEmptyFilters, type SearchFilters } from '../searchFilters';
import type { CarType, Powertrain, Transmission } from '../types';

export interface ExploreSearchFormProps {
  formId: string;
  formClass?: string;
  actionPath?: string;
  allowFlexibleSearch?: boolean;
  clearFiltersHref?: string;
  /** Initial values (e.g. from URL on /search). */
  initial?: SearchFilters;
  /** Si se define, no navega: actualiza filtros en el caller (página de búsqueda). */
  onApply?: (filters: SearchFilters) => void;
}

type MultiFilters = Record<string, string[]>;

const EMPTY_INITIAL: SearchFilters = {};

function toMulti(filters: SearchFilters): MultiFilters {
  const out: MultiFilters = {};
  if (filters.category?.length) out.category = [...filters.category];
  if (filters.transmission?.length) out.transmission = [...filters.transmission];
  if (filters.powertrain?.length) out.powertrain = [...filters.powertrain];
  if (filters.rating?.length) out.rating = [...filters.rating];
  if (filters.priceMarket?.length) out.priceMarket = [...filters.priceMarket];
  return out;
}

function fromMulti(
  base: SearchFilters,
  multi: MultiFilters,
  neighborhoodIds: Array<number | string>,
): SearchFilters {
  const priceMarket = (multi.priceMarket ?? []).filter((v): v is NonNullable<SearchFilters['priceMarket']>[number] =>
    (['below_market', 'at_market', 'above_market'] as const).includes(
      v as 'below_market' | 'at_market' | 'above_market',
    ),
  );
  const ids = neighborhoodIds
    .map((id) => Number(id))
    .filter((id) => Number.isInteger(id) && id > 0);
  return {
    ...base,
    category: multi.category?.length ? (multi.category as CarType[]) : undefined,
    transmission: multi.transmission?.length ? (multi.transmission as Transmission[]) : undefined,
    powertrain: multi.powertrain?.length ? (multi.powertrain as Powertrain[]) : undefined,
    rating: multi.rating?.length ? multi.rating : undefined,
    priceMarket: priceMarket.length ? priceMarket : undefined,
    neighborhoodIds: ids.length ? ids : undefined,
  };
}

/**
 * Formulario de exploración idéntico al tag {@code searchWithFilters} del JSP.
 */
export default function ExploreSearchForm({
  formId,
  formClass = 'search-menu sticky-top w-100',
  actionPath = paths.search,
  allowFlexibleSearch = true,
  clearFiltersHref = paths.search,
  initial = EMPTY_INITIAL,
  onApply,
}: ExploreSearchFormProps) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const neighborhoods = useNeighborhoods();

  const [query, setQuery] = useState(initial.q ?? '');
  const [from, setFrom] = useState(initial.from ?? '');
  const [until, setUntil] = useState(initial.until ?? '');
  const [priceMin, setPriceMin] = useState(initial.priceMin != null ? String(initial.priceMin) : '');
  const [priceMax, setPriceMax] = useState(initial.priceMax != null ? String(initial.priceMax) : '');
  const [selectedFilters, setSelectedFilters] = useState<MultiFilters>(() => toMulti(initial));
  const [neighborhoodIds, setNeighborhoodIds] = useState<Array<number | string>>(
    () => initial.neighborhoodIds ?? [],
  );
  const [flexible, setFlexible] = useState(Boolean(initial.flexible));
  const [flexMonth, setFlexMonth] = useState(initial.flexMonth ?? '');
  const [flexDays, setFlexDays] = useState(
    initial.flexDays != null ? String(initial.flexDays) : '',
  );

  // Keep draft fields aligned with URL/applied filters (back/forward, parent updates).
  const initialKey = JSON.stringify(initial);
  useEffect(() => {
    setQuery(initial.q ?? '');
    setFrom(initial.from ?? '');
    setUntil(initial.until ?? '');
    setPriceMin(initial.priceMin != null ? String(initial.priceMin) : '');
    setPriceMax(initial.priceMax != null ? String(initial.priceMax) : '');
    setSelectedFilters(toMulti(initial));
    setNeighborhoodIds(initial.neighborhoodIds ?? []);
    setFlexible(Boolean(initial.flexible));
    setFlexMonth(initial.flexMonth ?? '');
    setFlexDays(initial.flexDays != null ? String(initial.flexDays) : '');
    // eslint-disable-next-line react-hooks/exhaustive-deps -- sync when URL/applied filters change
  }, [initialKey]);

  const onFromChange = useCallback((v: string) => setFrom(v), []);
  const onUntilChange = useCallback((v: string) => setUntil(v), []);
  const { clearPickers } = useSearchDatePickers(
    formId,
    flexible ? '' : from,
    flexible ? '' : until,
    onFromChange,
    onUntilChange,
  );

  const neighborhoodList = useMemo(
    () =>
      (neighborhoods.data ?? []).map((n) => ({
        id: idFromUri(n.links.self) ?? n.links.self,
        name: n.name,
      })),
    [neighborhoods.data],
  );

  const datePickerSlot = (
    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', columnGap: '.5rem' }}>
      <div>
        <label className="form-label small text-secondary mb-1" htmlFor={`search_from_picker_${formId}`}>
          {t('searchBar.from.label')}
        </label>
        <input
          type="text"
          className="form-control form-control-sm border-0 shadow-none"
          id={`search_from_picker_${formId}`}
          readOnly
          placeholder={t('searchBar.date.placeholder')}
          aria-label={t('searchBar.from.ariaLabel')}
        />
      </div>
      <div>
        <label className="form-label small text-secondary mb-1" htmlFor={`search_until_picker_${formId}`}>
          {t('searchBar.until.label')}
        </label>
        <input
          type="text"
          className="form-control form-control-sm border-0 shadow-none"
          id={`search_until_picker_${formId}`}
          readOnly
          placeholder={t('searchBar.date.placeholder')}
          aria-label={t('searchBar.until.ariaLabel')}
        />
      </div>
    </div>
  );

  const buildFilters = (): SearchFilters =>
    fromMulti(
      {
        q: query.trim() || undefined,
        from: flexible ? undefined : from || undefined,
        until: flexible ? undefined : until || undefined,
        flexible: flexible || undefined,
        flexMonth: flexible && flexMonth ? flexMonth : undefined,
        flexDays: flexible && flexDays ? Number(flexDays) : undefined,
        priceMin: priceMin ? Number(priceMin) : undefined,
        priceMax: priceMax ? Number(priceMax) : undefined,
        sort: initial.sort,
      },
      selectedFilters,
      neighborhoodIds,
    );

  const onFlexibleChange = (enabled: boolean) => {
    setFlexible(enabled);
    if (enabled) {
      setFrom('');
      setUntil('');
    } else {
      setFlexDays('');
    }
  };

  const handleSubmit = (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const filters = buildFilters();
    if (onApply) {
      onApply(filters);
      return;
    }
    navigate(`${actionPath}?${filtersToSearchParams(filters)}`);
  };

  const resetFilters = () => {
    setQuery('');
    setPriceMin('');
    setPriceMax('');
    setSelectedFilters({});
    setNeighborhoodIds([]);
    setFlexible(false);
    setFlexMonth('');
    setFlexDays('');
    clearPickers();
    if (onApply) {
      onApply({});
      return;
    }
    navigate(clearFiltersHref);
  };

  // Misma regla que el JSP: solo filtros con valor real. buildFilters() ya descarta flexMonth/
  // flexDays cuando flexible=false (FlexibleDateControls pre-selecciona un mes en state local
  // aunque el toggle esté apagado — no debe contar como filtro activo).
  const showClear = !isEmptyFilters(buildFilters());

  return (
    <SearchWithFilters
      formId={formId}
      formClass={formClass}
      actionPath={actionPath}
      allowFlexibleSearch={allowFlexibleSearch}
      showClearFilters={showClear}
      clearFiltersHref={clearFiltersHref}
      onClearFilters={resetFilters}
      categoryFilterOptions={categoryFilterOptions(t)}
      transmissionFilterOptions={transmissionFilterOptions(t)}
      powertrainFilterOptions={powertrainFilterOptions(t)}
      ratingFilterOptions={ratingFilterOptions()}
      priceMarketFilterOptions={priceMarketFilterOptions(t)}
      neighborhoodList={neighborhoodList}
      query={query}
      from={from}
      until={until}
      selectedFilters={selectedFilters}
      selectedNeighborhoodIds={neighborhoodIds}
      priceMin={priceMin}
      priceMax={priceMax}
      flexible={flexible}
      datePickerSlot={datePickerSlot}
      flexControlsSlot={
        <FlexibleDateControls
          formId={formId}
          flexMonth={flexMonth}
          flexDays={flexDays}
          onFlexMonthChange={setFlexMonth}
          onFlexDaysChange={setFlexDays}
        />
      }
      onSubmit={handleSubmit}
      onQueryChange={setQuery}
      onFilterChange={(param, values) => setSelectedFilters((prev) => ({ ...prev, [param]: values }))}
      onNeighborhoodChange={setNeighborhoodIds}
      onPriceMinChange={setPriceMin}
      onPriceMaxChange={setPriceMax}
      onFlexibleChange={onFlexibleChange}
    />
  );
}
