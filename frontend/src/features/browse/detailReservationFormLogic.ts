/**
 * Port of {@code detailReservationForm.js} — bookable-segment range projection for car detail.
 */

export interface BookableSegment {
  from: string;
  to: string;
  dayPrice: number | null;
  checkInTime: string | null;
  checkOutTime: string | null;
  location: string;
  neighborhoodUri?: string;
}

export interface RangeProjection {
  fromDateTime: string;
  untilDateTime: string;
  pickupTimeLabel: string;
  returnTimeLabel: string;
  pickupLocationLabel: string;
  returnLocationLabel: string;
  total: number | null;
  showPricing: boolean;
  billableDays: number;
}

const DASH = '\u2014';

export function ymdFromDate(d: Date): string {
  const y = d.getFullYear();
  const m = d.getMonth() + 1;
  const dd = d.getDate();
  return `${y}-${m < 10 ? '0' : ''}${m}-${dd < 10 ? '0' : ''}${dd}`;
}

export function ymdCompare(a: string, b: string): number {
  return a < b ? -1 : a > b ? 1 : 0;
}

export function ymdPlusOne(ymd: string): string {
  const p = ymd.split('-');
  const d = new Date(Number(p[0]), Number(p[1]) - 1, Number(p[2]));
  d.setDate(d.getDate() + 1);
  return ymdFromDate(d);
}

function normalizeHm(value: string | null | undefined): string | null {
  if (!value) return null;
  const m = value.match(/^(\d{1,2}):(\d{2})/);
  if (!m) return null;
  const h = Number(m[1]);
  const mm = Number(m[2]);
  if (!Number.isFinite(h) || !Number.isFinite(mm) || h < 0 || h > 23 || mm < 0 || mm > 59) {
    return null;
  }
  return `${h < 10 ? '0' : ''}${h}:${mm < 10 ? '0' : ''}${mm}`;
}

export function findSegmentForYmd(ymd: string, segments: BookableSegment[]): BookableSegment | null {
  for (const s of segments) {
    if (ymdCompare(s.from, ymd) <= 0 && ymdCompare(ymd, s.to) <= 0) {
      return s;
    }
  }
  return null;
}

function wallYmdFromHidden(iso: string): string | null {
  if (!iso || iso.length < 10) return null;
  return iso.substring(0, 10);
}

export function billableDaysInclusive(fromIso: string, untilIso: string): number {
  const a = wallYmdFromHidden(fromIso);
  const b = wallYmdFromHidden(untilIso);
  if (!a || !b) return 0;
  const p = a.split('-');
  const q = b.split('-');
  if (p.length !== 3 || q.length !== 3) return 0;
  const d0 = new Date(Number(p[0]), Number(p[1]) - 1, Number(p[2]));
  const d1 = new Date(Number(q[0]), Number(q[1]) - 1, Number(q[2]));
  const diff = Math.round((d1.getTime() - d0.getTime()) / 86_400_000);
  return Math.max(1, diff + 1);
}

function computeSubtotalForRange(fromYmd: string, untilYmd: string, segments: BookableSegment[]) {
  let total = 0;
  let days = 0;
  let anyMissing = false;
  for (let d = fromYmd; ymdCompare(d, untilYmd) <= 0; d = ymdPlusOne(d)) {
    const s = findSegmentForYmd(d, segments);
    if (!s || s.dayPrice == null) {
      anyMissing = true;
      days++;
      continue;
    }
    total += s.dayPrice;
    days++;
  }
  return { total, days, complete: !anyMissing };
}

export function applyRangeProjection(
  selectedDates: Date[],
  segments: BookableSegment[],
): RangeProjection {
  const empty: RangeProjection = {
    fromDateTime: '',
    untilDateTime: '',
    pickupTimeLabel: DASH,
    returnTimeLabel: DASH,
    pickupLocationLabel: DASH,
    returnLocationLabel: DASH,
    total: null,
    showPricing: false,
    billableDays: 0,
  };

  if (!selectedDates.length) {
    return empty;
  }

  const fromYmd = ymdFromDate(selectedDates[0]);
  const pickupSeg = findSegmentForYmd(fromYmd, segments);
  const pickupHm = pickupSeg?.checkInTime ? normalizeHm(pickupSeg.checkInTime) : null;
  const fromDateTime = pickupHm ? `${fromYmd}T${pickupHm}` : fromYmd;

  if (selectedDates.length < 2) {
    return {
      ...empty,
      fromDateTime,
      pickupTimeLabel: pickupHm ?? DASH,
      pickupLocationLabel: pickupSeg?.location || DASH,
    };
  }

  const untilYmd = ymdFromDate(selectedDates[1]);
  const returnSeg = findSegmentForYmd(untilYmd, segments);
  const returnHm = returnSeg?.checkOutTime ? normalizeHm(returnSeg.checkOutTime) : null;
  const untilDateTime = returnHm ? `${untilYmd}T${returnHm}` : untilYmd;
  const subtotalInfo = computeSubtotalForRange(fromYmd, untilYmd, segments);
  const billableDays = billableDaysInclusive(fromDateTime, untilDateTime);

  return {
    fromDateTime,
    untilDateTime,
    pickupTimeLabel: pickupHm ?? DASH,
    returnTimeLabel: returnHm ?? DASH,
    pickupLocationLabel: pickupSeg?.location || DASH,
    returnLocationLabel: returnSeg?.location || DASH,
    total: subtotalInfo.complete && subtotalInfo.days > 0 ? subtotalInfo.total : null,
    showPricing: subtotalInfo.complete && subtotalInfo.days > 0,
    billableDays,
  };
}

export function hasCompleteRange(fromDateTime: string, untilDateTime: string): boolean {
  return !!fromDateTime && !!untilDateTime;
}

export function isMaxBillableViolated(
  fromDateTime: string,
  untilDateTime: string,
  maxBillableDays: number,
): boolean {
  if (!Number.isFinite(maxBillableDays) || maxBillableDays < 1 || !hasCompleteRange(fromDateTime, untilDateTime)) {
    return false;
  }
  return billableDaysInclusive(fromDateTime, untilDateTime) > maxBillableDays;
}

export function isMinRentalDaysViolated(
  fromDateTime: string,
  untilDateTime: string,
  minimumRentalDays: number | null | undefined,
): boolean {
  const minD = minimumRentalDays ?? 0;
  if (!Number.isFinite(minD) || minD < 2 || !hasCompleteRange(fromDateTime, untilDateTime)) {
    return false;
  }
  return billableDaysInclusive(fromDateTime, untilDateTime) < minD;
}

export function initialDefaultDates(
  segments: BookableSegment[],
  searchNeighborhoodIds: number[],
): Date[] {
  if (searchNeighborhoodIds.length === 0) return [];
  const matchSeg = segments.find(
    (seg) =>
      seg.neighborhoodUri != null &&
      searchNeighborhoodIds.some((id) => seg.neighborhoodUri!.endsWith(`/neighborhoods/${id}`)),
  );
  if (!matchSeg) return [];
  const d1 = dayStartFromYmd(matchSeg.from);
  const d2 = dayStartFromYmd(matchSeg.to);
  const out: Date[] = [];
  if (d1) out.push(d1);
  if (d2) out.push(d2);
  return out;
}

export function dayStartFromYmd(s: string): Date | null {
  if (!s || s.length < 10) return null;
  const p = s.substring(0, 10).split('-');
  if (p.length !== 3) return null;
  return new Date(Number(p[0]), Number(p[1]) - 1, Number(p[2]), 0, 0, 0, 0);
}

export function dayEndFromYmd(s: string): Date | null {
  if (!s || s.length < 10) return null;
  const p = s.substring(0, 10).split('-');
  if (p.length !== 3) return null;
  return new Date(Number(p[0]), Number(p[1]) - 1, Number(p[2]), 23, 59, 59, 999);
}
