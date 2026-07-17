import { useCallback, useMemo, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { collectionQueryPath } from '../../../api/apiDiscovery';
import { MediaTypes } from '../../../api/mediaTypes';
import { pageIndexFromParams, withPageIndex } from '../../../api/pageParam';
import { EmptyState, LoadingBlock } from '../../../components/ryden';
import { patchCarStatus } from '../api';
import {
  ADMIN_CAR_STATUS_FILTERS,
  parseAdminCarStatus,
  withAdminCarStatus,
  type AdminCarStatusFilter,
} from '../adminListFilters';
import AdminPageHeader from '../components/AdminPageHeader';
import AdminPagination from '../components/AdminPagination';
import { idFromSelf, type CarDto } from '../types';
import { useAdminErrorMessage } from '../useAdminErrorMessage';
import { usePagedList } from '../usePagedList';
import { carDetailTo } from '../../../routes/navigationState';

type PendingCarAction = { car: CarDto; mode: 'pause' | 'resume' };

export default function AdminCarsPage() {
  const { t } = useTranslation();
  const errorMessage = useAdminErrorMessage();

  const [actionError, setActionError] = useState<string | null>(null);
  const [busyLink, setBusyLink] = useState<string | null>(null);
  const [pending, setPending] = useState<PendingCarAction | null>(null);

  // Filtros + página en la URL (?status=&page=N) — bookmarkeable, F5-safe, sanitizados (V-10).
  const [searchParams, setSearchParams] = useSearchParams();
  const pageIndex = pageIndexFromParams(searchParams);
  const statusFilter = parseAdminCarStatus(searchParams);
  const goToPage = useCallback(
    (next: number) => setSearchParams(withPageIndex(searchParams, next)),
    [searchParams, setSearchParams],
  );

  const listPath = useMemo(() => {
    return collectionQueryPath('cars', { status: statusFilter || 'all' });
  }, [statusFilter]);

  const list = usePagedList<CarDto>(listPath, MediaTypes.car, pageIndex + 1, [statusFilter]);

  const onStatusFilterChange = (value: AdminCarStatusFilter) => {
    setSearchParams(withPageIndex(withAdminCarStatus(searchParams, value), 0));
  };

  const confirmAction = async () => {
    if (!pending) return;
    const { car, mode } = pending;
    setActionError(null);
    setBusyLink(car.links.self);
    try {
      await patchCarStatus(car.links.self, mode === 'pause' ? 'admin_paused' : 'active');
      setPending(null);
      list.reload();
    } catch (err) {
      setActionError(errorMessage(err));
    } finally {
      setBusyLink(null);
    }
  };

  const displayError = actionError ?? (list.error ? errorMessage(list.error) : null);

  return (
    <>
      <AdminPageHeader title={t('admin.cars.title')} subtitle={t('admin.cars.subtitle')} />

      <div className="mb-3">
        <label className="form-label small mb-1" htmlFor="adminCarStatus">
          {t('admin.cars.filter.status')}
        </label>
        <select
          id="adminCarStatus"
          className="form-select form-select-sm w-auto"
          value={statusFilter}
          onChange={(e) => onStatusFilterChange(e.target.value as AdminCarStatusFilter)}
        >
          {ADMIN_CAR_STATUS_FILTERS.map((s) => (
            <option key={s || 'all'} value={s}>
              {s ? t(`admin.cars.statuses.${s}`) : t('admin.cars.filter.all')}
            </option>
          ))}
        </select>
      </div>

      {displayError ? <div className="alert alert-danger" role="alert">{displayError}</div> : null}
      {list.loading ? <LoadingBlock variant="page" className="py-4" /> : null}

      {!list.loading && list.items.length === 0 ? (
        <EmptyState icon="car-front" title={t('admin.cars.empty')} inCard />
      ) : null}

      {!list.loading && list.items.length > 0 ? (
        <div className="card border-0 shadow-sm bg-white overflow-hidden">
          <div className="table-responsive">
            <table className="table table-hover align-middle mb-0 admin-table admin-table--cars">
              <thead className="table-light">
                <tr>
                  <th scope="col">{t('admin.cars.col.car')}</th>
                  <th scope="col">{t('admin.cars.col.plate')}</th>
                  <th scope="col">{t('admin.cars.col.status')}</th>
                  <th scope="col" className="text-end admin-table__cell--wrap">{t('admin.cars.col.actions')}</th>
                </tr>
              </thead>
              <tbody>
                {list.items.map((car) => {
                  const carId = idFromSelf(car.links);
                  const carDetailLink = carDetailTo(carId, car.links.self);
                  const busy = busyLink === car.links.self;
                  const canPause = car.status === 'active' || car.status === 'paused';
                  const canUnpause = car.status === 'admin_paused';
                  return (
                    <tr key={car.links.self}>
                      <td>
                        {car.brandName} {car.modelName}
                        {car.year ? <span className="text-secondary"> ({car.year})</span> : null}
                      </td>
                      <td>{car.plate}</td>
                      <td>{t(`admin.cars.statuses.${car.status}`)}</td>
                      <td className="text-end admin-table__cell--wrap">
                        <div className="d-flex flex-wrap gap-1 justify-content-end">
                          <Link
                            to={carDetailLink.pathname}
                            state={carDetailLink.state}
                            className="btn btn-outline-secondary btn-sm"
                          >
                            {t('admin.cars.actions.view')}
                          </Link>
                          {canPause ? (
                            <button
                              type="button"
                              className="btn btn-outline-danger btn-sm"
                              disabled={busy}
                              onClick={() => setPending({ car, mode: 'pause' })}
                            >
                              {t('admin.cars.actions.pause')}
                            </button>
                          ) : null}
                          {canUnpause ? (
                            <button
                              type="button"
                              className="btn btn-outline-success btn-sm"
                              disabled={busy}
                              onClick={() => setPending({ car, mode: 'resume' })}
                            >
                              {t('admin.cars.actions.unpause')}
                            </button>
                          ) : null}
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      ) : null}

      <AdminPagination page={list.page} currentPage={pageIndex} onPageChange={goToPage} />

      {pending ? (
        <div className="modal d-block" tabIndex={-1} role="dialog" aria-modal="true">
          <div className="modal-dialog">
            <div className="modal-content bg-white">
              <div className="modal-header">
                <h2 className="modal-title h5">
                  {t(pending.mode === 'pause' ? 'admin.cars.pauseModal.title' : 'admin.cars.resumeModal.title')}
                </h2>
                <button
                  type="button"
                  className="btn-close"
                  aria-label={t('common.close')}
                  onClick={() => setPending(null)}
                />
              </div>
              <div className="modal-body">
                <p className="mb-0">
                  {t(
                    pending.mode === 'pause' ? 'admin.cars.pauseModal.message' : 'admin.cars.resumeModal.message',
                    { plate: pending.car.plate },
                  )}
                </p>
              </div>
              <div className="modal-footer">
                <button type="button" className="btn btn-outline-secondary" onClick={() => setPending(null)}>
                  {t(pending.mode === 'pause' ? 'admin.cars.pauseModal.back' : 'admin.cars.resumeModal.back')}
                </button>
                <button
                  type="button"
                  className={`btn ${pending.mode === 'pause' ? 'btn-danger' : 'btn-success'}`}
                  disabled={busyLink != null}
                  onClick={() => void confirmAction()}
                >
                  {t(pending.mode === 'pause' ? 'admin.cars.pauseModal.confirm' : 'admin.cars.resumeModal.confirm')}
                </button>
              </div>
            </div>
          </div>
          <div className="modal-backdrop show" aria-hidden="true" />
        </div>
      ) : null}
    </>
  );
}
