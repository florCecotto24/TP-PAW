import { useEffect, useRef } from 'react';
import flatpickr from 'flatpickr';
import type { Instance } from 'flatpickr/dist/types/instance';
import { compactPrice } from '../../../components/FlatpickrCalendar';
import { flatpickrLocale } from '../../../i18n/dateFormat';
import {
  dayEndFromYmd,
  dayStartFromYmd,
  findSegmentForYmd,
  ymdFromDate,
  type BookableSegment,
} from '../detailReservationFormLogic';

export interface DetailReservationInlineCalendarProps {
  segments: BookableSegment[];
  defaultDates: Date[];
  defaultMonth?: string;
  locale: string;
  onChange: (dates: Date[]) => void;
}

/** Inline range calendar bound to {@code #detail_daterange} (mirrors detailReservationForm.js). */
export default function DetailReservationInlineCalendar({
  segments,
  defaultDates,
  defaultMonth,
  locale,
  onChange,
}: DetailReservationInlineCalendarProps) {
  const onChangeRef = useRef(onChange);
  onChangeRef.current = onChange;
  const segmentsKey = JSON.stringify(segments);

  useEffect(() => {
    const anchor = document.getElementById('detail_daterange');
    if (!anchor) return;

    const enable = segments
      .map((s) => ({
        from: dayStartFromYmd(s.from),
        to: dayEndFromYmd(s.to),
      }))
      .filter((r): r is { from: Date; to: Date } => r.from != null && r.to != null);

    const instance: Instance = flatpickr(anchor, {
      mode: 'range',
      inline: true,
      showMonths: 1,
      dateFormat: 'Y-m-d',
      minDate: 'today',
      defaultDate: defaultDates.length > 0 ? defaultDates : undefined,
      locale: flatpickrLocale(locale),
      disableMobile: true,
      enable: enable.length > 0 ? enable : [() => false],
      onDayCreate: (_dObj, _dStr, _fp, dayElem) => {
        const date = dayElem.dateObj;
        if (!date) return;
        const dayYmd = ymdFromDate(date);
        const seg = findSegmentForYmd(dayYmd, segments);
        if (seg?.dayPrice != null) {
          const priceEl = document.createElement('span');
          priceEl.className = 'fp-day-price';
          priceEl.textContent = compactPrice(seg.dayPrice);
          dayElem.appendChild(priceEl);
        }
      },
      onChange: (dates) => onChangeRef.current(dates as Date[]),
    });

    if (!defaultDates.length && defaultMonth) {
      const parts = defaultMonth.split('-');
      if (parts.length === 3) {
        const jump = new Date(Number(parts[0]), Number(parts[1]) - 1, Number(parts[2]));
        if (!Number.isNaN(jump.getTime())) {
          instance.jumpToDate(jump, false);
        }
      }
    }

    return () => {
      instance.destroy();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [segmentsKey, defaultMonth, locale, defaultDates.length]);

  return null;
}
