import { CarReservationCard } from '../../../components/ryden';
import { idFromUri } from '../../../api/uri';
import { formatDateTime, formatPrice } from '../format';
import type { ReservationSummaryDto } from '../types';
import { myReservationDetailTo } from '../../../routes/navigationState';

/** Tarjeta de reserva — delega en {@link CarReservationCard} (tag JSP) con datos del teaser. */
export default function ReservationListCard({
  reservation,
  role,
}: {
  reservation: ReservationSummaryDto;
  role?: 'owner';
}) {
  const reservationId = idFromUri(reservation.links?.self);
  const carId = idFromUri(reservation.links?.car);
  const detailQuery =
    role === 'owner'
      ? { role: 'OWNER', ...(carId ? { fromCar: carId } : {}) }
      : undefined;
  const linkTarget = reservationId
    ? myReservationDetailTo(reservationId, reservation.links?.self, detailQuery)
    : '#';

  const coverUri = reservation.links?.cover ?? null;
  const brand = reservation.brandName?.trim() || '—';
  const model = reservation.modelName?.trim() ?? '';

  return (
    <CarReservationCard
      reservation={{
        statusKey: reservation.status,
        brand,
        model,
        coverUri,
        pickupDateTime: formatDateTime(reservation.startDate),
        returnDateTime: formatDateTime(reservation.endDate),
        totalPrice: formatPrice(reservation.totalPrice),
      }}
      to={linkTarget}
      showRefundBadge={false}
    />
  );
}
