import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { DetailReservationPanel, LoadingBlock } from '../../../components/ryden';
import { newReservation } from '../../../routes/paths';
import {
  applyRangeProjection,
  dayStartFromYmd,
  formatReservationCurrency,
  hasCompleteRange,
  initialDefaultDates,
  isMaxBillableViolated,
  isMinRentalDaysViolated,
  type BookableSegment,
} from '../detailReservationFormLogic';
import { useCarBookableSegments } from '../hooks';
import DetailReservationInlineCalendar from './DetailReservationInlineCalendar';

const MAX_BILLABLE_DAYS = 30;

export interface DetailReservationFormProps {
  carId: number;
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
  carId,
  carName,
  isOwnerRequesting,
  minimumRentalDays,
}: DetailReservationFormProps) {
  const { t, i18n } = useTranslation();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const segmentsQuery = useCarBookableSegments(String(carId));

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
    MAX_BILLABLE_DAYS,
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
      ? formatReservationCurrency(projection.total, i18n.language)
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
    navigate(`${newReservation(carId)}?${params.toString()}`);
  };

  const defaultMonth = searchParams.get('flexMonth') ?? undefined;

  return (
    <DetailReservationPanel
      carId={carId}
      carName={carName}
      dailyPrice={0}
      maxBillableDays={MAX_BILLABLE_DAYS}
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
