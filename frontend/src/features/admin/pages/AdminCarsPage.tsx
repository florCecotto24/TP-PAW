import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { MediaTypes } from '../../../api/mediaTypes';
import { patchCarStatus } from '../api';
import AdminPageHeader from '../components/AdminPageHeader';
import AdminPagination from '../components/AdminPagination';
import { idFromSelf, type CarDto, type CarStatus } from '../types';
import { useAdminErrorMessage } from '../useAdminErrorMessage';
import { usePagedList } from '../usePagedList';

const STATUS_FILTERS: Array<CarStatus | ''> = [
  '', 'active', 'paused', 'admin_paused', 'lack_doc', 'unavailable', 'deactivated',
];

type PendingCarAction = { car: CarDto; mode: 'pause' | 'resume' };

export default function AdminCarsPage() {
  const { t } = useTranslation();
  const errorMessage = useAdminErrorMessage();

  const [statusFilter, setStatusFilter] = useState<CarStatus | ''>('');
  const [actionError, setActionError] = useState<string | null>(null);
  const [busyLink, setBusyLink] = useState<string | null>(null);
  const [pending, setPending] = useState<PendingCarAction | null>(null);

  const listPath = useMemo(() => {
    const params = new URLSearchParams({ scope: 'admin', page: '1' });
    if (statusFilter) params.set('status', statusFilter);
    return `/cars?${params.toString()}`;
  }, [statusFilter]);

  const list = usePagedList<CarDto>(listPath, MediaTypes.car, [statusFilter]);

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
          onChange={(e) => setStatusFilter(e.target.value as CarStatus | '')}
        >
          {STATUS_FILTERS.map((s) => (
            <option key={s || 'all'} value={s}>
              {s ? t(`admin.cars.statuses.${s}`) : t('admin.cars.filter.all')}
            </option>
          ))}
        </select>
      </div>

      {displayError ? <div className="alert alert-danger" role="alert">{displayError}</div> : null}
      {list.loading ? <p className="text-secondary" role="status">{t('app.loading')}</p> : null}

      {!list.loading && list.items.length === 0 ? (
        <p className="text-secondary">{t('admin.cars.empty')}</p>
      ) : null}

      {!list.loading && list.items.length > 0 ? (
        <div className="card border-0 shadow-sm bg-white overflow-hidden">
          <div className="table-responsive">
            <table className="table table-hover align-middle mb-0">
              <thead className="table-light">
                <tr>
                  <th scope="col">{t('admin.cars.col.car')}</th>
                  <th scope="col">{t('admin.cars.col.plate')}</th>
                  <th scope="col">{t('admin.cars.col.status')}</th>
                  <th scope="col" className="text-end">{t('admin.cars.col.actions')}</th>
                </tr>
              </thead>
              <tbody>
                {list.items.map((car) => {
                  const carId = idFromSelf(car.links);
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
                      <td className="text-end">
                        <div className="d-flex flex-wrap gap-1 justify-content-end">
                          <Link to={`/autos/${carId}`} className="btn btn-outline-secondary btn-sm">
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

      <AdminPagination page={list.page} onGo={list.goTo} />

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
                  aria-label="Close"
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
