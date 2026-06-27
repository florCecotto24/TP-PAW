import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { idFromUri } from '../api';
import { formatDateTime, formatPrice, statusLabelKey } from '../format';
import type { ReservationDto } from '../types';

export default function ReservationCard({ reservation }: { reservation: ReservationDto }) {
  const { t } = useTranslation();
  const reservationId = idFromUri(reservation.links.self);
  const href = reservationId ? `/reservas/${reservationId}` : null;

  return (
    <article className="card border-0 shadow-sm bg-white h-100">
      <div className="card-body">
        <div className="d-flex justify-content-between align-items-start gap-2 mb-2">
          <span className="badge text-bg-secondary">{t(statusLabelKey(reservation.status))}</span>
          {reservation.totalPrice != null ? (
            <span className="fw-semibold">{formatPrice(reservation.totalPrice)}</span>
          ) : null}
        </div>
        <dl className="small mb-0">
          <div className="mb-1">
            <dt className="text-secondary d-inline">{t('res.list.card.pickup')}: </dt>
            <dd className="d-inline mb-0">{formatDateTime(reservation.startDate)}</dd>
          </div>
          <div>
            <dt className="text-secondary d-inline">{t('res.list.card.return')}: </dt>
            <dd className="d-inline mb-0">{formatDateTime(reservation.endDate)}</dd>
          </div>
        </dl>
        {href ? (
          <Link to={href} className="stretched-link" aria-label={t('res.detail.title')}></Link>
        ) : null}
      </div>
    </article>
  );
}
