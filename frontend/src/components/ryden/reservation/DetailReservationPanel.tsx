import type { FormEvent, ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { newReservation } from '../../../routes/paths';

export interface DetailReservationPanelProps {
  carId: number;
  carName: string;
  dailyPrice: number;
  priceFrom?: boolean;
  maxBillableDays: number;
  isOwnerRequesting: boolean;
  minimumRentalDays?: number | null;
  /** Slot for the inline Flatpickr calendar (rendered after the anchor input). */
  calendarSlot?: ReactNode;
  fromDateTime?: string;
  untilDateTime?: string;
  reservationTotal?: string;
  submitDisabled?: boolean;
  pickupTimeLabel?: string;
  returnTimeLabel?: string;
  pickupLocationLabel?: string;
  returnLocationLabel?: string;
  totalAmount?: string | null;
  showPricingSummary?: boolean;
  showDateAlert?: boolean;
  showMaxBillableAlert?: boolean;
  minRentalDaysAlert?: string | null;
  onSubmit?: (e: FormEvent<HTMLFormElement>) => void;
  actionHref?: string;
}

/**
 * Espejo estructural de {@code ryden:detailReservationPanel}.
 * La lógica de calendario/precios vive en el caller (p.ej. FlatpickrCalendar + hooks).
 */
export default function DetailReservationPanel({
  carId,
  carName,
  maxBillableDays,
  isOwnerRequesting,
  minimumRentalDays,
  calendarSlot,
  fromDateTime = '',
  untilDateTime = '',
  reservationTotal = '',
  submitDisabled = false,
  pickupTimeLabel = '—',
  returnTimeLabel = '—',
  pickupLocationLabel = '—',
  returnLocationLabel = '—',
  totalAmount,
  showPricingSummary = false,
  showDateAlert = false,
  showMaxBillableAlert = false,
  minRentalDaysAlert,
  onSubmit,
  actionHref,
}: DetailReservationPanelProps) {
  const { t } = useTranslation();
  const action = actionHref ?? newReservation(carId);

  return (
    <div className="detail-reservation-panel border rounded-4 p-4 bg-white shadow-sm">
      <form action={action} method="get" id="detailReservationForm" onSubmit={onSubmit}>
        <input type="hidden" name="carName" value={carName} />
        <input
          type="hidden"
          name="reservationTotal"
          id="detail_reservation_total_hint"
          value={reservationTotal}
        />

        {minimumRentalDays != null && minimumRentalDays > 1 ? (
          <p className="small text-secondary mb-3">
            <i className="bi bi-calendar-check me-1" aria-hidden="true"></i>
            {t('carDetail.minRentalDays', { count: minimumRentalDays })}
          </p>
        ) : null}

        <div className="mb-3">
          <label className="form-label small mb-1">{t('detailReservationPanel.pickupReturnDates')}</label>
          <p className="form-text small text-muted mb-2">
            {t('detailReservationPanel.maxBillableDays.hint', { days: maxBillableDays })}
          </p>
          <input
            type="text"
            id="detail_daterange"
            className="detail-daterange-anchor"
            readOnly
            aria-hidden="true"
            tabIndex={-1}
            aria-label={t('detailReservationPanel.dates.ariaLabel')}
          />
          {calendarSlot}
          <input type="hidden" name="fromDateTime" id="detail_from_hidden" value={fromDateTime} />
          <input type="hidden" name="untilDateTime" id="detail_until_hidden" value={untilDateTime} />
        </div>

        <h2 className="h6 fw-semibold mb-2">{t('detailReservationPanel.pickupReturn')}</h2>
        <p className="form-text small mb-2">
          <strong>{t('detailReservationPanel.pickupAt')}</strong>{' '}
          <span id="detail_pickup_time_label">{pickupTimeLabel}</span>
          <span className="text-muted mx-1">·</span>
          <strong>{t('detailReservationPanel.returnBy')}</strong>{' '}
          <span id="detail_return_time_label">{returnTimeLabel}</span>
        </p>

        <div className="d-flex align-items-start gap-2 mb-2">
          <i className="bi bi-geo-alt text-secondary mt-1" aria-hidden="true"></i>
          <div className="flex-grow-1 min-w-0">
            <small className="text-muted d-block mb-1">{t('detailReservationPanel.pickupLocation')}</small>
            <p className="mb-0 fw-medium ryden-text-break" id="detail_pickup_location_label">
              {pickupLocationLabel}
            </p>
          </div>
        </div>

        <div className="d-flex align-items-start gap-2 mb-3">
          <i className="bi bi-geo-alt-fill text-secondary mt-1" aria-hidden="true"></i>
          <div className="flex-grow-1 min-w-0">
            <small className="text-muted d-block mb-1">{t('detailReservationPanel.returnLocation')}</small>
            <p className="mb-0 fw-medium ryden-text-break" id="detail_return_location_label">
              {returnLocationLabel}
            </p>
          </div>
        </div>

        <div className="border-top pt-3 small" id="detail_pricing_summary" hidden={!showPricingSummary}>
          <div className="d-flex justify-content-between align-items-baseline mb-4">
            <span className="fw-semibold">{t('detailReservationPanel.total')}</span>
            <span className="detail-total-amount fw-bold fs-4" id="detail_total_amount">
              {totalAmount}
            </span>
          </div>
        </div>

        {!isOwnerRequesting ? (
          <>
            <div className="alert alert-danger mb-3" id="detail_date_alert" role="alert" hidden={!showDateAlert}>
              {t('detailReservationPanel.dateAlert')}
            </div>
            <div
              className="alert alert-danger mb-3"
              id="detail_max_billable_alert"
              role="alert"
              hidden={!showMaxBillableAlert}
            >
              {t('validation.reservationForm.maxBillableDays', { days: maxBillableDays })}
            </div>
            <div
              className="alert alert-warning mb-3"
              id="detail_min_rental_days_alert"
              role="alert"
              hidden={!minRentalDaysAlert}
            >
              {minRentalDaysAlert}
            </div>
            <button
              type="submit"
              className="btn btn-lg btn-primary w-100 py-3 rounded-3 mb-2"
              id="detailReservationSubmitBtn"
              disabled={submitDisabled}
            >
              {t('detailReservationPanel.startReservation')}
            </button>
            <p className="text-center text-secondary small mb-0 text-uppercase detail-reservation-disclaimer">
              {t('detailReservationPanel.disclaimer')}
            </p>
          </>
        ) : null}
      </form>
    </div>
  );
}
