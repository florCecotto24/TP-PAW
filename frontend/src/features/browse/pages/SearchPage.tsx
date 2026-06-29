import { useEffect, useMemo } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Pagination, SortBar } from '../../../components/ryden';
import BrowseCarCard from '../components/BrowseCarCard';
import ExploreSearchForm from '../components/ExploreSearchForm';
import {
  SEARCH_PAGE_SIZE,
  apiSortToJspSort,
  hasActiveSearchFilters,
  jspSortToApiSort,
  searchBasePath,
} from '../exploreSearch';
import { pageCount, useSearchCarsPage } from '../hooks';
import { filtersToSearchParams, parseFilters } from '../searchFilters';
import { paths } from '../../../routes/paths';

export default function SearchPage() {
  const { t } = useTranslation();
  const [searchParams, setSearchParams] = useSearchParams();
  const filters = useMemo(() => parseFilters(searchParams), [searchParams]);
  const pageIndex = Math.max(0, Number(searchParams.get('page') ?? '0') || 0);
  const search = useSearchCarsPage(filters, pageIndex + 1, SEARCH_PAGE_SIZE);
  const cars = search.data?.items ?? [];
  const total = search.data?.page.total;
  const totalPages = pageCount(total, SEARCH_PAGE_SIZE);

  useEffect(() => {
    document.body.classList.add('has-fixed-navbar');
    return () => {
      document.body.classList.remove('has-fixed-navbar');
    };
  }, []);

  const applyFilters = (next: typeof filters) => {
    const params = filtersToSearchParams(next);
    params.set('page', '0');
    setSearchParams(params, { replace: true });
  };

  const onSortChange = (jspSort: string) => {
    const params = filtersToSearchParams({ ...filters, sort: jspSortToApiSort(jspSort) });
    params.set('page', '0');
    setSearchParams(params, { replace: true });
  };

  const baseUrl = searchBasePath(searchParams);
  const activeFilters = hasActiveSearchFilters(searchParams);
  const firstItem = total != null && total > 0 ? pageIndex * SEARCH_PAGE_SIZE + 1 : 0;
  const lastItem = total != null ? Math.min((pageIndex + 1) * SEARCH_PAGE_SIZE, total) : 0;

  return (
    <>
      <ExploreSearchForm
        formId="exploreSearchForm"
        formClass="search-menu sticky-top w-100"
        actionPath={paths.search}
        allowFlexibleSearch
        showClearFilters={activeFilters}
        clearFiltersHref={paths.search}
        initial={filters}
        onApply={applyFilters}
      />

      <div className="container">
        <div className="mb-3 pt-5 d-flex flex-wrap align-items-center justify-content-between gap-2">
          <h4 className="font-semibold mb-0">
            {total != null && total > 0
              ? t('search.resultsRange', { from: firstItem, to: lastItem, total })
              : t('search.resultsCount', { count: 0 })}
          </h4>
          {cars.length > 0 ? (
            <SortBar
              baseUrl={baseUrl}
              currentSort={apiSortToJspSort(filters.sort)}
              onSortChange={onSortChange}
            />
          ) : null}
        </div>

        {search.isError ? (
          <div className="alert alert-danger" role="alert">
            {t('browse.search.loadError')}
          </div>
        ) : null}
        {search.isLoading ? (
          <p className="text-secondary" role="status">
            {t('app.loading')}
          </p>
        ) : null}

        {!search.isLoading && cars.length === 0 ? (
          <div className="search-empty-state text-center">
            <div className="search-empty-state__icon" aria-hidden="true">
              <i className="bi bi-search"></i>
            </div>
            {activeFilters ? (
              <>
                <h2 className="h4 fw-semibold mb-2">{t('search.empty.title')}</h2>
                <p className="text-secondary mb-2 search-empty-state__text">{t('search.empty.description')}</p>
                <div className="search-empty-state__actions">
                  <Link to={paths.search} className="btn btn-primary btn-action btn-action-md">
                    {t('search.empty.reset')}
                  </Link>
                </div>
              </>
            ) : (
              <>
                <h2 className="h4 fw-semibold mb-2">{t('search.empty.noCars.title')}</h2>
                <p className="text-secondary mb-0 search-empty-state__text">
                  {t('search.empty.noCars.description')}
                </p>
                <div className="search-empty-state__actions mt-4">
                  <Link to={paths.search} className="btn btn-primary btn-action btn-action-md">
                    {t('search.empty.noCars.cta')}
                  </Link>
                </div>
              </>
            )}
          </div>
        ) : null}

        {cars.length > 0 ? (
          <div className="text-center">
            <div className="row row-cols-1 row-cols-md-2 row-cols-lg-4 pt-4 g-3">
              {cars.map((car) => (
                <div key={car.links.self} className="col d-flex justify-content-center">
                  <BrowseCarCard
                    car={car}
                    searchQuery={
                      filters.flexible && filters.flexMonth
                        ? { flexMonth: filters.flexMonth }
                        : {
                            ...(filters.from ? { from: filters.from } : {}),
                            ...(filters.until ? { until: filters.until } : {}),
                          }
                    }
                  />
                </div>
              ))}
            </div>
          </div>
        ) : null}

        {totalPages > 1 ? (
          <Pagination
            currentPage={pageIndex}
            totalPages={totalPages}
            baseUrl={baseUrl}
            sortParam={apiSortToJspSort(filters.sort)}
            pageParam="page"
            sortParamName="sort"
          />
        ) : null}
      </div>
    </>
  );
}
