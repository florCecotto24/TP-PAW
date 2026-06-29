import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { sessionClient } from '../../session/sessionStore';
import { MediaTypes } from '../../api/mediaTypes';
import { apiAssetUrl } from '../../api/uri';
import { fetchOwnerCars, idFromUri } from './api';
import { useApiErrorMessage, useCurrentUserId } from './hooks';
import { STATUS_BADGE, type CarDto, type CarStatus } from './types';
import { paths, myCarDetail } from '../../routes/paths';

const STATUS_FILTERS: Array<CarStatus | ''> = [
  '', 'active', 'paused', 'admin_paused', 'lack_doc', 'unavailable', 'deactivated',
];
const SORTS = ['recent', 'name', 'rating_desc'] as const;

export default function MyCarsPage() {
  const { t } = useTranslation();
  const ownerId = useCurrentUserId();
  const errorMessage = useApiErrorMessage();

  const [cars, setCars] = useState<CarDto[]>([]);
  const [status, setStatus] = useState<CarStatus | ''>('');
  const [sort, setSort] = useState<string>('recent');
  const [nextLink, setNextLink] = useState<string | undefined>();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!ownerId) return;
    let active = true;
    setLoading(true);
    setError(null);
    fetchOwnerCars(ownerId, { status, sort })
      .then((res) => {
        if (!active) return;
        setCars(res.data ?? []);
        setNextLink(res.page.next);
      })
      .catch((err) => { if (active) setError(errorMessage(err)); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [ownerId, status, sort]);

  async function loadMore() {
    if (!nextLink) return;
    try {
      const res = await sessionClient.follow<CarDto[]>(nextLink, { accept: MediaTypes.car });
      setCars((prev) => [...prev, ...(res.data ?? [])]);
      setNextLink(res.page.next);
    } catch (err) {
      setError(errorMessage(err));
    }
  }

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

      <div className="d-flex flex-wrap align-items-end gap-3 mb-4">
        <div>
          <label className="form-label small mb-1" htmlFor="myCarsStatus">{t('owner.myCars.filterStatus')}</label>
          <select
            id="myCarsStatus"
            className="form-select form-select-sm"
            value={status}
            onChange={(e) => setStatus(e.target.value as CarStatus | '')}
          >
            {STATUS_FILTERS.map((s) => (
              <option key={s || 'all'} value={s}>
                {s ? t(`owner.enums.status.${s}`) : t('owner.myCars.allStatuses')}
              </option>
            ))}
          </select>
        </div>
        <div>
          <label className="form-label small mb-1" htmlFor="myCarsSort">{t('owner.myCars.sort')}</label>
          <select
            id="myCarsSort"
            className="form-select form-select-sm"
            value={sort}
            onChange={(e) => setSort(e.target.value)}
          >
            {SORTS.map((s) => (
              <option key={s} value={s}>{t(`owner.myCars.sortOptions.${s}`)}</option>
            ))}
          </select>
        </div>
      </div>

      {error && <div className="alert alert-danger" role="alert">{error}</div>}
      {loading && <p className="text-secondary" role="status">{t('app.loading')}</p>}

      {!loading && cars.length === 0 && (
        <div className="search-empty-state text-center py-5">
          <i className="bi bi-car-front fs-1 text-secondary d-block mb-3" aria-hidden="true" />
          <h2 className="h4 fw-semibold mb-2">{t('owner.myCars.empty')}</h2>
          <div className="mt-4">
            <Link to={paths.publishCar} className="btn btn-primary">{t('owner.myCars.publishCta')}</Link>
          </div>
        </div>
      )}

      <div className="d-flex flex-column gap-3">
        {cars.map((car) => {
          const id = idFromUri(car.links.self);
          if (!id) return null;
          const coverUrl = car.links.cover ? apiAssetUrl(car.links.cover) : null;
          return (
            <Link
              key={car.links.self}
              to={myCarDetail(id)}
              className="reservation-card text-decoration-none text-reset"
            >
              <article className="card border-0 shadow-sm rounded-4 overflow-hidden reservation-card__surface position-relative">
                <span className={`position-absolute top-0 end-0 m-3 badge ${STATUS_BADGE[car.status]}`}>
                  {t(`owner.enums.status.${car.status}`)}
                </span>
                <div className="row g-0 align-items-stretch">
                  <div className="col-12 col-md-3 reservation-card__media-wrap">
                    {coverUrl ? (
                      <img
                        src={coverUrl}
                        alt={`${car.brandName} ${car.modelName}`}
                        className="reservation-card__media"
                      />
                    ) : (
                      <div className="reservation-card__media reservation-card__media--placeholder d-flex align-items-center justify-content-center text-secondary">
                        <i className="bi bi-car-front fs-1" aria-hidden="true" />
                      </div>
                    )}
                  </div>
                  <div className="col-12 col-md-9 min-w-0">
                    <div className="card-body p-3 p-md-4 h-100 d-flex flex-column justify-content-between gap-2">
                      <div className="min-w-0" style={{ paddingRight: '8rem' }}>
                        <h3 className="h5 fw-semibold mb-1 ryden-text-clamp-2">
                          {car.brandName} {car.modelName}
                          {car.year ? <span className="text-secondary fw-normal"> ({car.year})</span> : null}
                        </h3>
                        <p className="small text-secondary mb-0">{car.plate}</p>
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

      {nextLink && (
        <div className="text-center mt-4">
          <button type="button" className="btn btn-outline-secondary" onClick={loadMore}>
            {t('owner.myCars.loadMore')}
          </button>
        </div>
      )}
    </main>
  );
}
