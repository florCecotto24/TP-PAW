import { useCallback } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { formatDateTime } from '../../../i18n/dateFormat';
import { collectionQueryPath } from '../../../api/apiDiscovery';
import { MediaTypes } from '../../../api/mediaTypes';
import { pageIndexFromParams, withPageIndex } from '../../../api/pageParam';
import { EmptyState, LoadingBlock } from '../../../components/ryden';
import AdminPageHeader from '../components/AdminPageHeader';
import AdminPagination from '../components/AdminPagination';
import { idFromSelf, type ReservationSummaryDto } from '../types';
import { useAdminErrorMessage } from '../useAdminErrorMessage';
import { useAdminGuard } from '../useAdminGuard';
import { usePagedList } from '../usePagedList';
import { adminReservationChat } from '../../../routes/paths';
import type { AdminReservationChatLocationState } from '../../../routes/navigationState';

export default function AdminReservationsPage() {
  const { t, i18n } = useTranslation();
  const errorMessage = useAdminErrorMessage();
  const { isAdmin, loading: guardLoading } = useAdminGuard();
  // La página vive en la URL (?page=N, 0-based como SearchPage) -> bookmarkeable y resiste refresh.
  const [searchParams, setSearchParams] = useSearchParams();
  const pageIndex = pageIndexFromParams(searchParams);
  const goToPage = useCallback(
    (next: number) => setSearchParams(withPageIndex(searchParams, next)),
    [searchParams, setSearchParams],
  );
  const list = usePagedList<ReservationSummaryDto>(
    guardLoading || !isAdmin ? '' : collectionQueryPath('reservations'),
    MediaTypes.reservationLinks,
    pageIndex + 1,
    [],
    MediaTypes.reservationSummary,
  );

  const displayError = list.error ? errorMessage(list.error) : null;

  return (
    <>
      <AdminPageHeader
        title={t('admin.reservations.title')}
        subtitle={t('admin.reservations.subtitle')}
      />

      {displayError ? <div className="alert alert-danger" role="alert">{displayError}</div> : null}
      {guardLoading || list.loading ? <LoadingBlock variant="page" className="py-4" /> : null}

      {!list.loading && list.items.length === 0 ? (
        <EmptyState icon="calendar-check" title={t('admin.reservations.empty')} inCard />
      ) : null}

      {!list.loading && list.items.length > 0 ? (
        <div className="card border-0 shadow-sm bg-white overflow-hidden">
          <div className="table-responsive">
            <table className="table table-hover align-middle mb-0 admin-table admin-table--reservations">
              <thead className="table-light">
                <tr>
                  <th scope="col">{t('admin.reservations.col.start')}</th>
                  <th scope="col">{t('admin.reservations.col.end')}</th>
                  <th scope="col">{t('admin.reservations.col.status')}</th>
                  <th scope="col">{t('admin.reservations.col.total')}</th>
                  <th scope="col" className="text-end admin-table__cell--wrap">{t('admin.reservations.col.actions')}</th>
                </tr>
              </thead>
              <tbody>
                {list.items.map((reservation) => {
                  const reservationId = idFromSelf(reservation.links);
                  return (
                    <tr key={reservation.links.self}>
                      <td>{formatDateTime(reservation.startDate, i18n.language)}</td>
                      <td>{formatDateTime(reservation.endDate, i18n.language)}</td>
                      <td>{t(`admin.reservations.statuses.${reservation.status}`)}</td>
                      <td>{reservation.totalPrice}</td>
                      <td className="text-end admin-table__cell--wrap">
                        <Link
                          to={adminReservationChat(reservationId)}
                          state={
                            {
                              messagesLink: reservation.links.messages,
                              reservationSelf: reservation.links.self,
                            } satisfies AdminReservationChatLocationState
                          }
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

      <AdminPagination page={list.page} currentPage={pageIndex} onPageChange={goToPage} />
    </>
  );
}
