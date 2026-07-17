import { type ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { isAppLinkTarget, type AppLinkTarget } from '../../../routes/navigationState';
import AuthenticatedImg from '../media/AuthenticatedImg';

export interface CarReservationCardData {
  statusKey: string;
  brand: string;
  model: string;
  /** API cover URI (loaded with Bearer via AuthenticatedImg). */
  coverUri?: string | null;
  pickupDateTime: string;
  returnDateTime: string;
  totalPrice: string;
}

export interface CarReservationCardProps {
  reservation: CarReservationCardData;
  /** SPA path or AppLinkTarget with hypermedia state. */
  to: string | AppLinkTarget;
  showRefundBadge?: boolean;
}

function statusBadgeClass(statusKey: string): string {
  if (statusKey === 'accepted') return 'bg-success';
  if (statusKey.startsWith('cancelled')) return 'bg-danger';
  if (statusKey === 'started') return 'bg-info';
  if (statusKey === 'pending') return 'bg-warning text-dark';
  return 'bg-secondary';
}

/** Espejo del tag ryden:carReservationCard. */
export default function CarReservationCard({
  reservation,
  to,
  showRefundBadge = false,
}: CarReservationCardProps) {
  const { t } = useTranslation();
  const pathname = isAppLinkTarget(to) ? to.pathname : to;
  const linkState = isAppLinkTarget(to) ? to.state : undefined;
  const href = pathname;
  const internal = href.startsWith('/') && !href.startsWith('//');

  const cover = reservation.coverUri;
  const placeholder = (
    <div className="reservation-card__media reservation-card__media--placeholder d-flex align-items-center justify-content-center text-secondary">
      <i className="bi bi-car-front fs-1" aria-hidden="true"></i>
    </div>
  );
  const media: ReactNode = cover ? (
    <AuthenticatedImg
      src={cover}
      alt={`${reservation.brand} ${reservation.model}`}
      className="reservation-card__media"
      fallback={placeholder}
    />
  ) : (
    placeholder
  );

  const content: ReactNode = (
    <article className="card border-0 shadow-sm rounded-4 overflow-hidden reservation-card__surface position-relative">
      <div
        className="position-absolute top-0 end-0 m-2 z-1 d-flex flex-column align-items-end gap-2"
        style={{ maxWidth: '55%' }}
      >
        <span className={`badge ${statusBadgeClass(reservation.statusKey)}`}>
          {t(`enum.reservation.status.${reservation.statusKey}`)}
        </span>
        {showRefundBadge ? (
          <span
            className="d-inline-flex align-items-center gap-2 text-end small fw-semibold"
            style={{
              backgroundColor: '#b91c1c',
              color: '#ffffff',
              padding: '.4rem .75rem',
              borderRadius: '.5rem',
              maxWidth: '100%',
              whiteSpace: 'normal',
              wordBreak: 'break-word',
              lineHeight: 1.25,
            }}
          >
            <i className="bi bi-cash-coin flex-shrink-0" aria-hidden="true"></i>
            <span>{t('ownerReservations.badge.refundProofPending')}</span>
          </span>
        ) : null}
      </div>
      <div className="row g-0 align-items-stretch">
        <div className="col-12 col-md-3 reservation-card__media-wrap">{media}</div>
        <div className="col-12 col-md-9 min-w-0">
          <div className="card-body p-3 p-md-4 h-100 d-flex flex-column justify-content-between gap-3">
            <div className="d-flex flex-wrap align-items-start gap-2">
              <div className="min-w-0 flex-grow-1">
                <h3 className="h5 fw-semibold mb-1 ryden-text-break">
                  {reservation.brand} {reservation.model}
                </h3>
              </div>
            </div>
            <div className="row g-3">
              <div className="col-12 col-sm-6">
                <p className="reservation-card__meta-label mb-1">{t('myReservations.card.pickup')}</p>
                <p className="mb-0 fw-medium">{reservation.pickupDateTime}</p>
              </div>
              <div className="col-12 col-sm-6">
                <p className="reservation-card__meta-label mb-1">{t('myReservations.card.return')}</p>
                <p className="mb-0 fw-medium">{reservation.returnDateTime}</p>
              </div>
            </div>
            <div className="pt-1">
              <div className="reservation-price-compact">
                <span className="reservation-card__meta-label mb-0">{t('myReservations.card.totalPrice')}</span>
                <span className="h5 mb-0 fw-bold text-primary">{reservation.totalPrice}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </article>
  );

  if (internal) {
    return (
      <Link to={pathname} state={linkState} className="reservation-card text-decoration-none text-reset">
        {content}
      </Link>
    );
  }

  return (
    <a href={href} className="reservation-card text-decoration-none text-reset">
      {content}
    </a>
  );
}
