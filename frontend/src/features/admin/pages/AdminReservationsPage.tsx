import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { MediaTypes } from '../../../api/mediaTypes';
import AdminPageHeader from '../components/AdminPageHeader';
import AdminPagination from '../components/AdminPagination';
import { idFromSelf, type ReservationDto } from '../types';
import { useAdminErrorMessage } from '../useAdminErrorMessage';
import { usePagedList } from '../usePagedList';

function formatDate(iso: string): string {
  try {
    return new Date(iso).toLocaleString();
  } catch {
    return iso;
  }
}

export default function AdminReservationsPage() {
  const { t } = useTranslation();
  const errorMessage = useAdminErrorMessage();
  const list = usePagedList<ReservationDto>('/reservations?page=1', MediaTypes.reservation);

  const displayError = list.error ? errorMessage(list.error) : null;

  return (
    <>
      <AdminPageHeader
        title={t('admin.reservations.title')}
        subtitle={t('admin.reservations.subtitle')}
      />

      {displayError ? <div className="alert alert-danger" role="alert">{displayError}</div> : null}
      {list.loading ? <p className="text-secondary" role="status">{t('app.loading')}</p> : null}

      {!list.loading && list.items.length === 0 ? (
        <p className="text-secondary">{t('admin.reservations.empty')}</p>
      ) : null}

      {!list.loading && list.items.length > 0 ? (
        <div className="card border-0 shadow-sm bg-white overflow-hidden">
          <div className="table-responsive">
            <table className="table table-hover align-middle mb-0">
              <thead className="table-light">
                <tr>
                  <th scope="col">{t('admin.reservations.col.start')}</th>
                  <th scope="col">{t('admin.reservations.col.end')}</th>
                  <th scope="col">{t('admin.reservations.col.status')}</th>
                  <th scope="col">{t('admin.reservations.col.total')}</th>
                  <th scope="col" className="text-end">{t('admin.reservations.col.actions')}</th>
                </tr>
              </thead>
              <tbody>
                {list.items.map((reservation) => {
                  const reservationId = idFromSelf(reservation.links);
                  return (
                    <tr key={reservation.links.self}>
                      <td>{formatDate(reservation.startDate)}</td>
                      <td>{formatDate(reservation.endDate)}</td>
                      <td>{t(`admin.reservations.statuses.${reservation.status}`)}</td>
                      <td>{reservation.totalPrice}</td>
                      <td className="text-end">
                        <Link
                          to={`/admin/reservas/${reservationId}/chat`}
                          className="btn btn-outline-primary btn-sm"
                        >
                          {t('admin.reservations.actions.viewChat')}
                        </Link>
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
    </>
  );
}
