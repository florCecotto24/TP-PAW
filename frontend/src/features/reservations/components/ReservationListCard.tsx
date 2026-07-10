import { CarReservationCard } from '../../../components/ryden';
import { apiAssetUrl, idFromUri } from '../../../api/uri';
import { formatDateTime, formatPrice } from '../format';
import type { ReservationSummaryDto } from '../types';
import { myReservationDetail } from '../../../routes/paths';

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
  const href = reservationId ? myReservationDetail(reservationId, detailQuery) : '#';

  const imageUrl = reservation.links?.cover ? apiAssetUrl(reservation.links.cover) : null;
  const brand = reservation.brandName?.trim() || '—';
  const model = reservation.modelName?.trim() ?? '';

  return (
    <CarReservationCard
      reservation={{
        statusKey: reservation.status,
        brand,
        model,
        imageUrl,
        pickupDateTime: formatDateTime(reservation.startDate),
        returnDateTime: formatDateTime(reservation.endDate),
        totalPrice: formatPrice(reservation.totalPrice),
      }}
      href={href}
      showRefundBadge={false}
    />
  );
}
