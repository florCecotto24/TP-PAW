import { useMemo } from 'react';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { BreadcrumbTrail, EmptyState, LoadingBlock, Pagination, SortBar } from '../../../components/ryden';
import { appBasePath } from '../../../appBasePath';
import { useCar } from '../../browse/hooks';

const CAR_EMPTY_ILLUSTRATION_URL = `${appBasePath()}/assets/images/filmore-cars.png`;
import MyReservationsOwnerFilterForm from '../components/MyReservationsOwnerFilterForm';
import MyReservationsRiderFilterForm from '../components/MyReservationsRiderFilterForm';
import ReservationListCard from '../components/ReservationListCard';
import {
  ownerReservationsPageCount,
  useOwnerReservationsPage,
} from '../hooks/useOwnerReservationsPage';
import {
  riderReservationsPageCount,
  useRiderReservationsPage,
} from '../hooks/useRiderReservationsPage';
import { currentJspReservationSort } from '../reservationListSort';
import {
  OWNER_RESERVATIONS_PAGE_SIZE,
  filtersToOwnerSearchParams,
  hasActiveOwnerReservationFilters,
  ownerPageIndex,
  ownerReservationsBasePath,
  parseOwnerReservationFilters,
  showOwnerFilterClear,
  type OwnerReservationFilters,
} from '../ownerReservationFilters';
import {
  RIDER_RESERVATIONS_PAGE_SIZE,
  filtersToRiderSearchParams,
  hasActiveRiderReservationFilters,
  parseRiderReservationFilters,
  riderPageIndex,
  riderReservationsBasePath,
  showRiderFilterClear,
  type RiderReservationFilters,
} from '../riderReservationFilters';
import { useCurrentUser } from '../useCurrentUser';
import { paths } from '../../../routes/paths';

type MyReservationsPageProps = {
  scope?: 'rider' | 'owner';
};

export default function MyReservationsPage({ scope = 'rider' }: MyReservationsPageProps) {
  const { t } = useTranslation();
  const { id, isAuthenticated } = useCurrentUser();

  if (!isAuthenticated) {
    return (
      <main className="container py-4">
        <div className="alert alert-warning">{t('res.list.needLogin')}</div>
        <Link to={paths.login} className="btn btn-primary">
          {t('res.list.login')}
        </Link>
      </main>
    );
  }

  if (scope === 'owner') {
    return <OwnerReservationsView ownerId={id} />;
  }

  return <RiderReservationsView riderId={id} />;
}

function RiderReservationsView({ riderId }: { riderId: string | null }) {
  const { t } = useTranslation();
  const [searchParams, setSearchParams] = useSearchParams();
  const filters = useMemo(() => parseRiderReservationFilters(searchParams), [searchParams]);
  const pageIndex = riderPageIndex(searchParams);
  const list = useRiderReservationsPage(riderId, filters, pageIndex);

  const items = list.data?.items ?? [];
  const total = list.data?.total;
  const totalPages = riderReservationsPageCount(total);
  const activeFilters = hasActiveRiderReservationFilters(searchParams);
  const showFilters = items.length > 0 || activeFilters;
  const baseUrl = riderReservationsBasePath(searchParams);
  const currentSort = currentJspReservationSort(filters.sort);
  const firstItem = total != null && total > 0 ? pageIndex * RIDER_RESERVATIONS_PAGE_SIZE + 1 : 0;
  const lastItem = total != null ? Math.min((pageIndex + 1) * RIDER_RESERVATIONS_PAGE_SIZE, total) : 0;

  const applyFilters = (next: RiderReservationFilters) => {
    const params = filtersToRiderSearchParams(next);
    params.set('riderPage', '0');
    setSearchParams(params, { replace: true });
  };

  const onSortChange = (jspSort: string) => {
    const params = filtersToRiderSearchParams({
      ...filters,
      sort: jspSort,
    });
    params.set('riderPage', '0');
    setSearchParams(params, { replace: true });
  };

  const heading = t('myReservations.heading');

  return (
    <main className="container pt-5 pb-4">
      <BreadcrumbTrail currentLabel={heading} />

      <section className="reservation-management-header mt-4 pt-5 mb-4">
        <h1 className="h3 fw-bold mb-2">{heading}</h1>
        <p className="text-secondary mb-0">{t('myReservations.subheading')}</p>
      </section>

      {list.isError ? <div className="alert alert-danger">{t('res.list.error')}</div> : null}
      {list.isLoading ? <LoadingBlock variant="page" className="py-4" /> : null}

      {showFilters ? (
        <>
          <MyReservationsRiderFilterForm
            initial={filters}
            showClear={showRiderFilterClear(searchParams)}
            onSubmit={applyFilters}
          />
          <div className="mb-3 d-flex flex-wrap align-items-center justify-content-between gap-2">
            <h3 className="h6 mb-0">
              {total != null && total > 0
                ? t('myReservations.resultsRange', { from: firstItem, to: lastItem, total })
                : t('myReservations.resultsCount', { count: 0 })}
            </h3>
            {items.length > 0 ? (
              <SortBar
                baseUrl={baseUrl}
                currentSort={currentSort}
                wrapperClass="d-flex align-items-center gap-2 flex-wrap"
                onSortChange={onSortChange}
                pageParamName="riderPage"
              />
            ) : null}
          </div>
        </>
      ) : null}

      {!list.isLoading && items.length === 0 ? (
        <EmptyState
          icon={activeFilters ? 'search' : 'calendar-event'}
          title={
            activeFilters
              ? t('myReservations.noResults.title')
              : t('myReservations.empty.title')
          }
          description={activeFilters ? undefined : t('myReservations.empty.description')}
          actions={
            activeFilters ? (
              <Link to={paths.myReservations} className="btn btn-outline-secondary">
                {t('search.filters.clear')}
              </Link>
            ) : (
              <Link to={paths.search} className="btn btn-primary btn-action btn-action-md">
                {t('myReservations.empty.reserve')}
              </Link>
            )
          }
        />
      ) : null}

      {items.length > 0 ? (
        <>
          <div className="d-flex flex-column gap-3 mb-4">
            {items.map((reservation, index) => (
              <ReservationListCard
                key={reservation.links?.self ?? `reservation-${index}`}
                reservation={reservation}
              />
            ))}
          </div>
          {totalPages > 1 ? (
            <Pagination
              currentPage={pageIndex}
              totalPages={totalPages}
              baseUrl={baseUrl}
              pageParam="riderPage"
              sortParam={currentSort}
            />
          ) : null}
        </>
      ) : null}
    </main>
  );
}

function OwnerReservationsView({ ownerId }: { ownerId: string | null }) {
  const { t } = useTranslation();
  const { carId: carIdParam } = useParams<{ carId?: string }>();
  const [searchParams, setSearchParams] = useSearchParams();
  const filters = useMemo(() => parseOwnerReservationFilters(searchParams), [searchParams]);
  const pageIndex = ownerPageIndex(searchParams);
  const scopedToCar = carIdParam != null && carIdParam !== '';
  const carQuery = useCar(scopedToCar ? carIdParam : undefined);
  const selectedCar = scopedToCar ? carQuery.data : null;

  const list = useOwnerReservationsPage(ownerId, filters, pageIndex, carIdParam);

  const items = list.data?.items ?? [];
  const total = list.data?.total;
  const totalPages = ownerReservationsPageCount(total);
  const activeFilters = hasActiveOwnerReservationFilters(searchParams, scopedToCar);
  const showFilters = items.length > 0 || activeFilters;
  const baseUrl = ownerReservationsBasePath(searchParams, carIdParam);
  const currentSort = currentJspReservationSort(filters.ownerSort);
  const firstItem =
    total != null && total > 0 ? pageIndex * OWNER_RESERVATIONS_PAGE_SIZE + 1 : 0;
  const lastItem =
    total != null ? Math.min((pageIndex + 1) * OWNER_RESERVATIONS_PAGE_SIZE, total) : 0;

  const heading = t('res.list.ownerTitle');
  const carTitle =
    selectedCar != null ? `${selectedCar.brandName} ${selectedCar.modelName}`.trim() : null;

  const applyFilters = (next: OwnerReservationFilters) => {
    const params = filtersToOwnerSearchParams(next);
    params.set('page', '0');
    setSearchParams(params, { replace: true });
  };

  const onSortChange = (jspSort: string) => {
    const params = filtersToOwnerSearchParams({
      ...filters,
      ownerSort: jspSort,
    });
    params.set('page', '0');
    setSearchParams(params, { replace: true });
  };

  const clearHref = scopedToCar && carIdParam ? paths.ownerReservations : baseUrl.split('?')[0];

  return (
    <main className="container pt-5 pb-4">
      {scopedToCar && carTitle ? (
        <BreadcrumbTrail
          homeLabel={heading}
          homeHref={paths.ownerReservations}
          currentLabel={carTitle}
        />
      ) : (
        <BreadcrumbTrail currentLabel={heading} />
      )}

      <section className="reservation-management-header mt-4 pt-5 mb-4">
        <h1 className="h3 fw-bold mb-2">{heading}</h1>
        <p className="text-secondary mb-0">{carTitle ?? t('res.list.ownerSubheading')}</p>
      </section>

      {list.isError ? <div className="alert alert-danger">{t('res.list.error')}</div> : null}
      {list.isLoading ? <LoadingBlock variant="page" className="py-4" /> : null}

      {showFilters ? (
        <>
          <MyReservationsOwnerFilterForm
            initial={filters}
            showClear={showOwnerFilterClear(searchParams, scopedToCar)}
            carId={carIdParam}
            onSubmit={applyFilters}
          />
          <div className="mb-3 d-flex flex-wrap align-items-center justify-content-between gap-2">
            <h3 className="h6 mb-0">
              {total != null && total > 0
                ? t('res.list.ownerResultsRange', { from: firstItem, to: lastItem, total })
                : t('res.list.ownerResultsCount', { count: 0 })}
            </h3>
            {items.length > 0 ? (
              <SortBar
                baseUrl={baseUrl}
                currentSort={currentSort}
                sortParamName="ownerSort"
                pageParamName="page"
                wrapperClass="d-flex align-items-center gap-2 flex-wrap"
                onSortChange={onSortChange}
              />
            ) : null}
          </div>
        </>
      ) : null}

      {!list.isLoading && items.length === 0 ? (
        activeFilters ? (
          <EmptyState
            icon="search"
            title={t('myReservations.noResults.title')}
            actions={(
              <Link to={clearHref} className="btn btn-outline-secondary">
                {t('search.filters.clear')}
              </Link>
            )}
          />
        ) : scopedToCar ? (
          <div className="search-empty-state text-center">
            <img
              src={CAR_EMPTY_ILLUSTRATION_URL}
              alt=""
              className="mb-4 img-fluid"
              style={{ maxWidth: 260 }}
            />
            <h2 className="h5 fw-semibold mb-2">{t('res.list.ownerCarEmpty.title')}</h2>
            <p className="text-secondary mb-0 search-empty-state__text">
              {t('res.list.ownerCarEmpty.description')}
            </p>
          </div>
        ) : (
          <EmptyState
            icon="calendar-event"
            title={t('res.list.ownerEmpty.title')}
            description={t('res.list.ownerEmpty.description')}
          />
        )
      ) : null}

      {items.length > 0 ? (
        <>
          <div className="d-flex flex-column gap-3 mb-4">
            {items.map((reservation, index) => (
              <ReservationListCard
                key={reservation.links?.self ?? `owner-reservation-${index}`}
                reservation={reservation}
                role="owner"
              />
            ))}
          </div>
          {totalPages > 1 ? (
            <Pagination
              currentPage={pageIndex}
              totalPages={totalPages}
              baseUrl={baseUrl}
              pageParam="page"
              sortParam={currentSort}
              sortParamName="ownerSort"
            />
          ) : null}
        </>
      ) : null}
    </main>
  );
}
