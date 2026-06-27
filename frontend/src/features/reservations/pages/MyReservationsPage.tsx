import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { MediaTypes } from '../../../api/mediaTypes';
import { usePagedList } from '../../admin/usePagedList';
import ReservationCard from '../components/ReservationCard';
import { useCurrentUser } from '../useCurrentUser';
import type { ReservationDto } from '../types';

type MyReservationsPageProps = {
  scope?: 'rider' | 'owner';
};

export default function MyReservationsPage({ scope = 'rider' }: MyReservationsPageProps) {
  const { t } = useTranslation();
  const { id, isAuthenticated } = useCurrentUser();

  const initialPath =
    id != null
      ? scope === 'owner'
        ? `/reservations?ownerId=${id}&page=1`
        : `/reservations?riderId=${id}&page=1`
      : '';

  const list = usePagedList<ReservationDto>(initialPath, MediaTypes.reservation, [id, scope]);

  if (!isAuthenticated) {
    return (
      <main className="container py-4">
        <div className="alert alert-warning">{t('res.list.needLogin')}</div>
        <Link to="/ingresar" className="btn btn-primary">
          {t('res.list.login')}
        </Link>
      </main>
    );
  }

  const title = scope === 'owner' ? t('res.list.ownerTitle') : t('res.list.title');
  const subheading = scope === 'owner' ? t('res.list.ownerSubheading') : t('res.list.subheading');

  return (
    <main className="container py-4">
      <header className="mb-4">
        <h1 className="h3 fw-semibold mb-1">{title}</h1>
        <p className="text-secondary mb-0">{subheading}</p>
      </header>

      <nav className="mb-4">
        <div className="btn-group" role="group" aria-label={t('res.list.tab.rider')}>
          <Link
            to="/mis-reservas"
            className={`btn btn-sm ${scope === 'rider' ? 'btn-primary' : 'btn-outline-primary'}`}
          >
            {t('res.list.tab.rider')}
          </Link>
          <Link
            to="/mis-autos/reservas"
            className={`btn btn-sm ${scope === 'owner' ? 'btn-primary' : 'btn-outline-primary'}`}
          >
            {t('res.list.tab.owner')}
          </Link>
        </div>
      </nav>

      {list.error ? <div className="alert alert-danger">{t('res.list.error')}</div> : null}
      {list.loading ? <p className="text-secondary">{t('res.list.loading')}</p> : null}

      {!list.loading && list.items.length === 0 ? (
        <p className="text-secondary">{t('res.list.empty')}</p>
      ) : null}

      {!list.loading && list.items.length > 0 ? (
        <div className="row g-3">
          {list.items.map((reservation) => (
            <div key={reservation.links.self} className="col-md-6 col-lg-4">
              <ReservationCard reservation={reservation} />
            </div>
          ))}
        </div>
      ) : null}

      {(list.page.prev || list.page.next) ? (
        <nav
          className="d-flex justify-content-end gap-2 mt-4"
          aria-label={t('res.list.pagination')}
        >
          <button
            type="button"
            className="btn btn-outline-secondary btn-sm"
            disabled={!list.page.prev}
            onClick={() => list.goTo(list.page.prev)}
          >
            {t('res.list.prev')}
          </button>
          <button
            type="button"
            className="btn btn-outline-secondary btn-sm"
            disabled={!list.page.next}
            onClick={() => list.goTo(list.page.next)}
          >
            {t('res.list.next')}
          </button>
        </nav>
      ) : null}
    </main>
  );
}
