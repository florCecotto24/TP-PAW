import { useQuery } from '@tanstack/react-query';
import { CarReservationCard } from '../../../components/ryden';
import { MediaTypes } from '../../../api/mediaTypes';
import { apiAssetUrl, idFromUri } from '../../../api/uri';
import { sessionClient } from '../../../session/sessionStore';
import { formatDateTime, formatPrice } from '../format';
import type { CarDto } from '../../browse/types';
import type { ReservationDto } from '../types';
import { myReservationDetail } from '../../../routes/paths';

/** Tarjeta de reserva — delega en {@link CarReservationCard} (tag JSP) con datos del auto vía link. */
export default function ReservationListCard({
  reservation,
  role,
}: {
  reservation: ReservationDto;
  role?: 'owner';
}) {
  const reservationId = idFromUri(reservation.links.self);
  const carId = idFromUri(reservation.links.car);
  const detailQuery =
    role === 'owner'
      ? { role: 'OWNER', ...(carId ? { fromCar: carId } : {}) }
      : undefined;
  const href = reservationId ? myReservationDetail(reservationId, detailQuery) : '#';
  const carLink = reservation.links.car;

  const { data: car } = useQuery({
    queryKey: ['reservation-card-car', carLink],
    enabled: Boolean(carLink),
    queryFn: async () => {
      const res = await sessionClient.follow<CarDto>(carLink as string, { accept: MediaTypes.car });
      return res.data;
    },
    staleTime: 60_000,
  });

  const imageUrl = reservation.links.cover
    ? apiAssetUrl(reservation.links.cover)
    : (car?.links.cover ? apiAssetUrl(car.links.cover) : null);

  return (
    <CarReservationCard
      reservation={{
        statusKey: reservation.status,
        brand: car?.brandName ?? '—',
        model: car?.modelName ?? '',
        imageUrl,
        pickupDateTime: formatDateTime(reservation.startDate),
        returnDateTime: formatDateTime(reservation.endDate),
        totalPrice: formatPrice(reservation.totalPrice),
      }}
      href={href}
      showRefundBadge={reservation.paymentRefundRequired}
    />
  );
}
