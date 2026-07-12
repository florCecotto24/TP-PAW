import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { getClientConfig } from '../../../api/clientConfig';
import { formatCurrency } from '../../../api/format';
import { DetailReservationPanel, LoadingBlock } from '../../../components/ryden';
import { newReservation } from '../../../routes/paths';
import type { NewReservationLocationState } from '../../../routes/navigationState';
import {
  applyRangeProjection,
  dayStartFromYmd,
  hasCompleteRange,
  initialDefaultDates,
  isMaxBillableViolated,
  isMinRentalDaysViolated,
  type BookableSegment,
} from '../detailReservationFormLogic';
import { useCarBookableSegments } from '../hooks';
import DetailReservationInlineCalendar from './DetailReservationInlineCalendar';

export interface DetailReservationFormProps {
  bookableSegmentsLink?: string;
  carId: number;
  carSelf?: string;
  carName: string;
  dailyPrice: number;
  priceFrom?: boolean;
  isOwnerRequesting: boolean;
  minimumRentalDays?: number | null;
}

function parseInitialDates(fromParam?: string, untilParam?: string): Date[] {
  const out: Date[] = [];
  const d1 = fromParam ? dayStartFromYmd(fromParam) : null;
  const d2 = untilParam ? dayStartFromYmd(untilParam) : null;
  if (d1) out.push(d1);
  if (d2) out.push(d2);
  return out;
}

function mapSegments(raw: ReturnType<typeof useCarBookableSegments>['data']): BookableSegment[] {
  return (raw ?? []).map((s) => ({
    from: s.from,
    to: s.to,
    dayPrice: s.dayPrice != null ? Number(s.dayPrice) : null,
    checkInTime: s.checkInTime,
    checkOutTime: s.checkOutTime,
    location: s.location ?? '',
    neighborhoodId: s.neighborhoodId,
  }));
}

export default function DetailReservationForm({
  bookableSegmentsLink,
  carId,
  carSelf,
  carName,
  isOwnerRequesting,
  minimumRentalDays,
}: DetailReservationFormProps) {
  const { t, i18n } = useTranslation();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const maxBillableDays = getClientConfig().maxBillableDays;
  const segmentsQuery = useCarBookableSegments(bookableSegmentsLink);

  const fromParam = searchParams.get('from') ?? undefined;
  const untilParam = searchParams.get('until') ?? undefined;
  const searchNbIds = useMemo(() => {
    const raw = searchParams.getAll('searchNbId');
    return raw.map(Number).filter((n) => Number.isFinite(n));
  }, [searchParams]);

  const segments = useMemo(() => mapSegments(segmentsQuery.data), [segmentsQuery.data]);

  const urlDefaultDates = useMemo(
    () => parseInitialDates(fromParam, untilParam),
    [fromParam, untilParam],
  );

  const nbDefaultDates = useMemo(
    () => (urlDefaultDates.length === 0 ? initialDefaultDates(segments, searchNbIds) : []),
    [segments, searchNbIds, urlDefaultDates.length],
  );

  const initialDates = urlDefaultDates.length > 0 ? urlDefaultDates : nbDefaultDates;

  const [projection, setProjection] = useState(() =>
    applyRangeProjection([], segments),
  );
  const [showDateAlert, setShowDateAlert] = useState(false);

  useEffect(() => {
    if (initialDates.length > 0 && segments.length > 0) {
      setProjection(applyRangeProjection(initialDates, segments));
    }
  }, [initialDates, segments]);

  const onCalendarChange = useCallback(
    (dates: Date[]) => {
      setProjection(applyRangeProjection(dates, segments));
      setShowDateAlert(false);
    },
    [segments],
  );

  const maxViolated = isMaxBillableViolated(
    projection.fromDateTime,
    projection.untilDateTime,
    maxBillableDays,
  );
  const minViolated = isMinRentalDaysViolated(
    projection.fromDateTime,
    projection.untilDateTime,
    minimumRentalDays,
  );
  const submitDisabled = maxViolated || minViolated;

  const minRentalDaysAlert =
    minViolated && minimumRentalDays != null && minimumRentalDays > 1
      ? t('validation.reservationForm.minRentalDays', { count: minimumRentalDays })
      : null;

  const totalLabel =
    projection.total != null
      ? formatCurrency(projection.total)
      : null;

  const onSubmit = (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (
      !hasCompleteRange(projection.fromDateTime, projection.untilDateTime) ||
      maxViolated ||
      minViolated
    ) {
      setShowDateAlert(!hasCompleteRange(projection.fromDateTime, projection.untilDateTime));
      return;
    }
    const params = new URLSearchParams({
      carName,
      fromDateTime: projection.fromDateTime,
      untilDateTime: projection.untilDateTime,
    });
    if (projection.total != null) {
      params.set('reservationTotal', String(projection.total));
    }
    navigate(
      { pathname: newReservation(carId), search: `?${params.toString()}` },
      { state: carSelf ? ({ carSelf } satisfies NewReservationLocationState) : undefined },
    );
  };

  const defaultMonth = searchParams.get('flexMonth') ?? undefined;

  return (
    <DetailReservationPanel
      carId={carId}
      carName={carName}
      dailyPrice={0}
      maxBillableDays={maxBillableDays}
      isOwnerRequesting={isOwnerRequesting}
      minimumRentalDays={minimumRentalDays}
      calendarSlot={
        segmentsQuery.isLoading ? (
          <LoadingBlock variant="inline" />
        ) : (
          <DetailReservationInlineCalendar
            segments={segments}
            defaultDates={initialDates}
            defaultMonth={defaultMonth}
            locale={i18n.language}
            onChange={onCalendarChange}
          />
        )
      }
      fromDateTime={projection.fromDateTime}
      untilDateTime={projection.untilDateTime}
      reservationTotal={projection.total != null ? String(projection.total) : ''}
      pickupTimeLabel={projection.pickupTimeLabel}
      returnTimeLabel={projection.returnTimeLabel}
      pickupLocationLabel={projection.pickupLocationLabel}
      returnLocationLabel={projection.returnLocationLabel}
      totalAmount={totalLabel}
      showPricingSummary={projection.showPricing}
      showDateAlert={showDateAlert}
      showMaxBillableAlert={maxViolated}
      minRentalDaysAlert={minRentalDaysAlert}
      submitDisabled={submitDisabled}
      onSubmit={onSubmit}
    />
  );
}
