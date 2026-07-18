import { useEffect, useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useSearchParams } from 'react-router-dom';
import { Pagination, SortBar, LoadingBlock, AuthenticatedCoverMedia } from '../../components/ryden';
import { paths } from '../../routes/paths';
import { myCarDetailTo } from '../../routes/navigationState';
import { idFromUri } from './api';
import MyCarsFilterForm from './components/MyCarsFilterForm';
import { myCarsPageCount, useApiErrorMessage, useCurrentUserId, useMyCarsPage } from './hooks';
import {
  hasActiveMyCarsFilters,
  myCarsBasePath,
  myCarsPageIndex,
  parseMyCarsFilters,
  showMyCarsFilterClear,
  type MyCarsFilters,
} from './myCarsFilters';
import { STATUS_BADGE, type CarSummaryDto } from './types';

function OwnerCarCardMedia({ coverUri, alt }: { coverUri?: string; alt: string }) {
  const placeholder = (
    <div className="reservation-card__media reservation-card__media--placeholder d-flex align-items-center justify-content-center text-secondary">
      <i className="bi bi-car-front fs-1" aria-hidden="true" />
    </div>
  );
  if (!coverUri) return placeholder;
  return (
    <AuthenticatedCoverMedia
      src={coverUri}
      alt={alt}
      className="reservation-card__media"
      style={{ objectFit: 'cover', backgroundColor: '#212529' }}
      fallback={placeholder}
    />
  );
}

export default function MyCarsPage() {
  const { t } = useTranslation();
  const ownerId = useCurrentUserId();
  const errorMessage = useApiErrorMessage();
  const [searchParams, setSearchParams] = useSearchParams();

  const filters = useMemo(() => parseMyCarsFilters(searchParams), [searchParams]);
  const pageIndex = myCarsPageIndex(searchParams);
  const list = useMyCarsPage(filters, pageIndex);

  const items = list.data?.items ?? [];
  const total = list.data?.total;
  const totalPages = myCarsPageCount(total);

  useEffect(() => {
    if (list.isLoading || total == null) return;
    const maxPage = Math.max(0, totalPages - 1);
    if (pageIndex > maxPage) {
      const next = new URLSearchParams(searchParams);
      if (maxPage <= 0) next.delete('page');
      else next.set('page', String(maxPage));
      setSearchParams(next, { replace: true });
    }
  }, [list.isLoading, total, totalPages, pageIndex, searchParams, setSearchParams]);

  const activeFilters = hasActiveMyCarsFilters(searchParams);
  const showFilters = items.length > 0 || activeFilters;
  const baseUrl = myCarsBasePath(searchParams);
  const currentSort = filters.sort ?? 'date,desc';
  const firstItem = total != null && total > 0 ? pageIndex * 8 + 1 : 0;
  const lastItem = total != null ? Math.min((pageIndex + 1) * 8, total) : 0;

  const applyFilters = (next: MyCarsFilters) => {
    const params = new URLSearchParams();
    if (next.q) params.set('q', next.q);
    next.status?.forEach((s) => params.append('listingStatus', s));
    next.category?.forEach((c) => params.append('category', c));
    next.transmission?.forEach((tr) => params.append('transmission', tr));
    next.powertrain?.forEach((p) => params.append('powertrain', p));
    if (next.priceMin !== undefined) params.set('priceMin', String(next.priceMin));
    if (next.priceMax !== undefined) params.set('priceMax', String(next.priceMax));
    next.rating?.forEach((r) => params.append('rating', r));
    if (next.sort) params.set('sort', next.sort);
    params.set('page', '0');
    setSearchParams(params, { replace: true });
  };

  const onSortChange = (jspSort: string) => {
    applyFilters({ ...filters, sort: jspSort });
  };

  if (!ownerId) {
    return (
      <main className="container pt-5 pb-4">
        <section className="reservation-management-header mb-4">
          <h1 className="h3 fw-bold mb-2">{t('owner.myCars.title')}</h1>
        </section>
        <div className="alert alert-warning" role="alert">{t('owner.errors.notAuthenticated')}</div>
      </main>
    );
  }

  return (
    <main className="container pt-5 pb-4">
      <section className="reservation-management-header mb-4 d-flex flex-wrap align-items-end justify-content-between gap-3">
        <div>
          <h1 className="h3 fw-bold mb-2">{t('owner.myCars.title')}</h1>
          <p className="text-secondary mb-0">{t('owner.myCars.subheading')}</p>
        </div>
        <Link to={paths.publishCar} className="btn btn-primary">
          <i className="bi bi-plus-lg me-1" aria-hidden="true" />
          {t('owner.myCars.publishCta')}
        </Link>
      </section>

      {list.isError ? <div className="alert alert-danger" role="alert">{errorMessage(list.error)}</div> : null}
      {list.isLoading ? <LoadingBlock variant="page" className="py-4" /> : null}

      {showFilters ? (
        <>
          <MyCarsFilterForm
            initial={filters}
            showClear={showMyCarsFilterClear(searchParams)}
            onSubmit={applyFilters}
          />
          <div className="mb-3 d-flex flex-wrap align-items-center justify-content-between gap-2">
            <h3 className="h6 mb-0">
              {total != null && total > 0
                ? t('myCars.resultsRange', { from: firstItem, to: lastItem, total })
                : t('myCars.resultsCount', { count: 0 })}
            </h3>
            {items.length > 0 ? (
              <SortBar
                baseUrl={baseUrl}
                currentSort={currentSort}
                wrapperClass="d-flex align-items-center gap-2 flex-wrap"
                onSortChange={onSortChange}
              />
            ) : null}
          </div>
        </>
      ) : null}

      {!list.isLoading && items.length === 0 && (
        <div className="search-empty-state text-center py-5">
          {activeFilters ? (
            <>
              <h2 className="h4 fw-semibold mb-2">{t('myCars.noResults.title')}</h2>
              <div className="search-empty-state__actions mt-4">
                <Link to={paths.myCars} className="btn btn-outline-secondary">
                  {t('search.filters.clear')}
                </Link>
              </div>
            </>
          ) : (
            <>
              <i className="bi bi-car-front fs-1 text-secondary d-block mb-3" aria-hidden="true" />
              <h2 className="h4 fw-semibold mb-2">{t('myCars.empty.title')}</h2>
              <p className="text-secondary mb-0 search-empty-state__text">{t('myCars.empty.description')}</p>
              <div className="mt-4">
                <Link to={paths.publishCar} className="btn btn-primary">{t('owner.myCars.publishCta')}</Link>
              </div>
            </>
          )}
        </div>
      )}

      {items.length > 0 ? (
        <>
          <div className="d-flex flex-column gap-3 mb-4">
            {items.map((car: CarSummaryDto) => {
              const id = idFromUri(car.links.self);
              if (!id) return null;
              const detailLink = myCarDetailTo(id, car.links.self);
              return (
                <Link
                  key={car.links.self}
                  to={detailLink.pathname}
                  state={detailLink.state}
                  className="reservation-card text-decoration-none text-reset"
                >
                  <article className="card border-0 shadow-sm rounded-4 overflow-hidden reservation-card__surface position-relative">
                    <span className={`position-absolute top-0 end-0 m-3 badge ${STATUS_BADGE[car.status]}`}>
                      {t(`owner.enums.status.${car.status}`)}
                    </span>
                    <div className="row g-0 align-items-stretch">
                      <div className="col-12 col-md-3 reservation-card__media-wrap">
                        <OwnerCarCardMedia
                          coverUri={car.links.cover}
                          alt={car.year != null ? `${car.brandName} ${car.modelName} (${car.year})` : `${car.brandName} ${car.modelName}`}
                        />
                      </div>
                      <div className="col-12 col-md-9 min-w-0">
                        <div className="card-body p-3 p-md-4 h-100 d-flex flex-column justify-content-between gap-2">
                          <div className="min-w-0" style={{ paddingRight: '8rem' }}>
                            <h3 className="h5 fw-semibold mb-1 ryden-text-clamp-2">
                              {car.brandName} {car.modelName}
                              {car.year != null ? <span className="text-secondary fw-normal"> ({car.year})</span> : null}
                            </h3>
                          </div>
                          {!car.modelValidated && (
                            <span className="small text-info-emphasis">
                              <i className="bi bi-clock me-1" aria-hidden="true" />
                              {t('owner.detail.modelPending')}
                            </span>
                          )}
                        </div>
                      </div>
                    </div>
                  </article>
                </Link>
              );
            })}
          </div>

          {totalPages > 1 ? (
            <Pagination currentPage={pageIndex} totalPages={totalPages} baseUrl={baseUrl} sortParam={currentSort} />
          ) : null}
        </>
      ) : null}
    </main>
  );
}
