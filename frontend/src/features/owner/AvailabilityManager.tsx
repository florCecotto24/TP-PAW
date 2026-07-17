import { useCallback, useEffect, useMemo, useRef, useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from 'react-bootstrap';
import flatpickr from 'flatpickr';
import type { Instance } from 'flatpickr/dist/types/instance';
import type { Options } from 'flatpickr/dist/types/options';
import FlatpickrCalendar, { compactPrice, dayStartFromYmd, dayEndFromYmd, ymd } from '../../components/FlatpickrCalendar';
import { NeighborhoodPicker } from '../../components/ryden';
import PriceMarketInsightCard from '../../components/ryden/car/PriceMarketInsightCard';
import { formatDateRange, flatpickrLocale, formatMonthYear, shiftMonth } from '../../i18n/dateFormat';
import { idFromUri } from '../../api/uri';
import { getClientConfig } from '../../api/clientConfig';
import { firstAvailabilityValidationError, minAvailabilityStartDateYmd } from './availabilityValidation';
import {
  createAvailability,
  deleteAvailability,
  fetchAvailabilities,
  fetchNeighborhoods,
  fetchPriceMarketInsight,
  updateAvailability,
} from './api';
import { useApiErrorMessage } from './hooks';
import type {
  AvailabilityCreateDto,
  AvailabilityDto,
  CarDto,
  NeighborhoodDto,
} from './types';

// Calendario de disponibilidad (reimplementa el viejo managePeriods/ownerCalendar):
// dibuja un calendario inline read-only del mes (vía Flatpickr) con los días
// reservables marcados y su precio por día; al clickear un día resalta la tarjeta
// del período que lo cubre. Debajo lista los períodos OFFERED efectivos del mes y
// permite crear / editar / retirar. La navegación de mes recarga ese mes.

function currentMonth(): string {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
}

const EMPTY_FORM: AvailabilityCreateDto = {
  startDate: '',
  endDate: '',
  dayPrice: 0,
  startPointStreet: '',
  startPointNumber: '',
  neighborhoodUri: '',
  checkInTime: '10:00',
  checkOutTime: '20:00',
};

export default function AvailabilityManager({ car }: { car: CarDto }) {
  const { t, i18n } = useTranslation();
  const errorMessage = useApiErrorMessage();
  const listingLimits = getClientConfig().listing;
  const pickupLeadHours = getClientConfig().pickupLeadHours;

  const [month, setMonth] = useState(currentMonth());
  const [periods, setPeriods] = useState<AvailabilityDto[]>([]);
  const [neighborhoods, setNeighborhoods] = useState<NeighborhoodDto[]>([]);
  const [form, setForm] = useState<AvailabilityCreateDto>(EMPTY_FORM);
  const [editing, setEditing] = useState<AvailabilityDto | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [reloadTick, setReloadTick] = useState(0);
  const [highlightedSelf, setHighlightedSelf] = useState<string | null>(null);
  const [priceInsight, setPriceInsight] = useState<{
    minPrice: number;
    maxPrice: number;
    averagePrice: number;
  } | null>(null);
  const rangePickerRef = useRef<HTMLDivElement>(null);
  const rangeFpRef = useRef<Instance | null>(null);

  const neighborhoodOptions = useMemo(
    () =>
      neighborhoods.map((n) => {
        const nid = n.links.self.split('?')[0].replace(/\/+$/, '').split('/').pop();
        return { id: Number(nid), name: n.name, uri: n.links.self };
      }),
    [neighborhoods],
  );

  const selectedNeighborhoodId = useMemo(() => {
    if (!form.neighborhoodUri) return null;
    const match = neighborhoodOptions.find((n) => n.uri === form.neighborhoodUri);
    return match?.id ?? null;
  }, [form.neighborhoodUri, neighborhoodOptions]);

  const minStartDate = useMemo(
    () => minAvailabilityStartDateYmd(form.checkInTime, pickupLeadHours),
    [form.checkInTime, pickupLeadHours],
  );
  const pickerMinStartDate = editing && editing.startDate < minStartDate ? editing.startDate : minStartDate;

  useEffect(() => {
    let active = true;
    const modelUri = car.links.model;
    const carId = idFromUri(car.links.self);
    if (!modelUri || !carId) {
      setPriceInsight(null);
      return undefined;
    }
    fetchPriceMarketInsight(modelUri, carId)
      .then((res) => {
        if (!active) return;
        const data = res.data;
        // Basta 1 peer del mismo modelo (min===max es válido;
        // PriceMarketInsightCard expande la barra a 0…2×mercado sin spread).
        if (data != null && data.sampleCount >= 1) {
          setPriceInsight({
            minPrice: data.minPrice,
            maxPrice: data.maxPrice,
            averagePrice: data.averagePrice,
          });
        } else {
          setPriceInsight(null);
        }
      })
      .catch(() => { if (active) setPriceInsight(null); });
    return () => { active = false; };
  }, [car]);

  useEffect(() => {
    const node = rangePickerRef.current;
    if (!node) return undefined;
    rangeFpRef.current?.destroy();
    const defaultDates: Date[] = [];
    if (form.startDate) {
      const start = dayStartFromYmd(form.startDate);
      if (start) defaultDates.push(start);
    }
    if (form.endDate) {
      const end = dayStartFromYmd(form.endDate);
      if (end) defaultDates.push(end);
    }
    rangeFpRef.current = flatpickr(node, {
      locale: flatpickrLocale(i18n.language),
      disableMobile: true,
      mode: 'range',
      inline: true,
      minDate: pickerMinStartDate,
      dateFormat: 'Y-m-d',
      defaultDate: defaultDates.length > 0 ? defaultDates : undefined,
      onChange: (dates) => {
        if (dates.length >= 1) update('startDate', ymd(dates[0]));
        if (dates.length >= 2) update('endDate', ymd(dates[1]));
        else if (dates.length === 1) update('endDate', '');
      },
    });
    return () => {
      rangeFpRef.current?.destroy();
      rangeFpRef.current = null;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [i18n.language, editing, pickerMinStartDate]);

  useEffect(() => {
    const fp = rangeFpRef.current;
    if (!fp) return;
    if (form.startDate && form.endDate) {
      const start = dayStartFromYmd(form.startDate);
      const end = dayStartFromYmd(form.endDate);
      if (start && end) fp.setDate([start, end], false);
    } else if (form.startDate) {
      const start = dayStartFromYmd(form.startDate);
      if (start) fp.setDate([start], false);
    } else {
      fp.clear(false);
    }
  }, [form.startDate, form.endDate]);

  useEffect(() => {
    let active = true;
    fetchNeighborhoods()
      .then((res) => { if (active) setNeighborhoods(res.data ?? []); })
      .catch(() => { /* opcional */ });
    return () => { active = false; };
  }, []);

  useEffect(() => {
    let active = true;
    setError(null);
    fetchAvailabilities(car, month)
      .then((res) => { if (active) setPeriods((res.data ?? []).filter((p) => p.kind === 'offered')); })
      .catch((err) => { if (active) setError(errorMessage(err)); });
    return () => { active = false; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [car, month, reloadTick]);

  function update<K extends keyof AvailabilityCreateDto>(key: K, value: AvailabilityCreateDto[K]) {
    setForm((f) => ({ ...f, [key]: value }));
  }

  // Calendario inline read-only del mes: marca los días reservables con su precio
  // por día y, al hacer clic en un día, resalta la tarjeta del período que lo cubre
  // (réplica de ownerCalendar.js). El cambio de mes recarga los períodos del mes.
  const ownerCalendarOptions = useMemo<Options>(() => {
    const enable = periods
      .map((p) => {
        const from = dayStartFromYmd(p.startDate);
        const to = dayEndFromYmd(p.endDate);
        return from && to ? { from, to } : null;
      })
      .filter((r): r is { from: Date; to: Date } => r != null);
    // OJO: flatpickr rompe si enable/disable llegan === undefined (parseDateRules
    // hace .slice sobre undefined). Sólo seteamos la clave que corresponde.
    const opts: Options = {
      inline: true,
      mode: 'single',
      clickOpens: false,
      showMonths: 1,
      defaultDate: `${month}-01`,
      onDayCreate: (_d, _s, _fp, dayElem) => {
        const date = (dayElem as HTMLElement & { dateObj?: Date }).dateObj;
        if (!date) return;
        const d = ymd(date);
        const seg = periods.find((p) => p.startDate <= d && d <= p.endDate);
        if (seg) {
          const priceEl = document.createElement('span');
          priceEl.className = 'fp-day-price';
          priceEl.textContent = compactPrice(seg.dayPrice);
          dayElem.appendChild(priceEl);
        }
      },
      onMonthChange: (_dates, _str, fp) => {
        const ym = `${fp.currentYear}-${String(fp.currentMonth + 1).padStart(2, '0')}`;
        setMonth(ym);
      },
    };
    if (enable.length > 0) opts.enable = enable;
    else opts.disable = [() => true];
    return opts;
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [periods, month]);

  const onCalendarDayClick = useCallback((dates: Date[]) => {
    if (dates.length === 0) return;
    const d = ymd(dates[0]);
    // Si varios períodos cubren el día, gana el más reciente (último en la lista).
    const covering = periods.filter((p) => p.startDate <= d && d <= p.endDate);
    const best = covering.length > 0 ? covering[covering.length - 1] : null;
    setHighlightedSelf(best ? best.links.self : null);
  }, [periods]);

  function startEdit(p: AvailabilityDto) {
    setEditing(p);
    // Repoblamos el barrio desde su link (antes quedaba undefined y al guardar la
    // edición se perdía el barrio salvo que se re-eligiera).
    const nbLink = p.links.neighborhood;
    setForm({
      startDate: p.startDate,
      endDate: p.endDate,
      dayPrice: p.dayPrice,
      startPointStreet: p.startPointStreet,
      startPointNumber: p.startPointNumber ?? '',
      neighborhoodUri: nbLink ?? '',
      checkInTime: p.checkInTime,
      checkOutTime: p.checkOutTime,
    });
  }

  function resetForm() {
    setEditing(null);
    setForm(EMPTY_FORM);
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    const leadMinStartDate = editing && form.startDate === editing.startDate ? undefined : minStartDate;
    const validationError = firstAvailabilityValidationError(form, leadMinStartDate);
    if (validationError) {
      setError(t(`owner.availability.errors.${validationError}`, {
        min: getClientConfig().listing.pricePerDayMin,
        integer: getClientConfig().listing.pricePerDayIntegerDigits,
        fraction: getClientConfig().listing.pricePerDayFractionDigits,
        minDate: minStartDate,
        hours: pickupLeadHours,
      }));
      return;
    }
    setBusy(true);
    try {
      if (editing) await updateAvailability(editing, car, form);
      else await createAvailability(car, form);
      resetForm();
      setNotice(editing ? t('owner.availability.updatedNotice') : t('owner.availability.createdNotice'));
      setReloadTick((n) => n + 1);
    } catch (err) {
      setError(errorMessage(err, 'owner.availability.errors.saveFailed'));
    } finally {
      setBusy(false);
    }
  }

  async function onWithdraw(p: AvailabilityDto) {
    setError(null);
    setBusy(true);
    try {
      await deleteAvailability(p);
      if (editing && editing.links.self === p.links.self) resetForm();
      setReloadTick((n) => n + 1);
    } catch (err) {
      setError(errorMessage(err, 'owner.availability.errors.withdrawFailed'));
    } finally {
      setBusy(false);
    }
  }

  return (
    <article className="card border-0 shadow-sm rounded-4 mb-4 bg-white" id="availabilityDisplaySection">
      <div className="card-body p-4">
        <div className="d-flex align-items-center justify-content-between gap-2 mb-3 flex-wrap">
          <h2 className="h5 fw-semibold mb-0">{t('owner.availability.title')}</h2>
          <div className="d-flex align-items-center gap-2" role="group" aria-labelledby="availMonthLabel">
            <span id="availMonthLabel" className="form-label small mb-0 text-secondary">
              {t('owner.availability.month')}
            </span>
            <div className="d-flex align-items-center gap-1">
              <button
                type="button"
                className="btn btn-sm btn-outline-secondary"
                aria-label={t('owner.availability.prevMonth')}
                onClick={() => setMonth((m) => shiftMonth(m, -1))}
              >
                <i className="bi bi-chevron-left" aria-hidden="true" />
              </button>
              <span className="small fw-semibold text-nowrap px-1" id="availMonth">
                {formatMonthYear(month, i18n.language)}
              </span>
              <button
                type="button"
                className="btn btn-sm btn-outline-secondary"
                aria-label={t('owner.availability.nextMonth')}
                onClick={() => setMonth((m) => shiftMonth(m, 1))}
              >
                <i className="bi bi-chevron-right" aria-hidden="true" />
              </button>
            </div>
          </div>
        </div>

        {error && <div className="alert alert-danger py-2 small" role="alert">{error}</div>}
        {notice && <div className="alert alert-success py-2 small" role="status">{notice}</div>}

        <div className="owner-cal-container owner-cal-readonly mb-3">
          <FlatpickrCalendar options={ownerCalendarOptions} onChange={onCalendarDayClick} />
        </div>

        {periods.length === 0 ? (
          <p className="text-secondary mb-4">{t('owner.availability.empty')}</p>
        ) : (
          <div className="d-flex flex-column gap-2 mb-4">
            {periods.map((p) => (
              <div
                key={p.links.self}
                className={`manage-period-card p-3 border rounded-3 bg-white d-flex align-items-center justify-content-between gap-2 flex-wrap${highlightedSelf === p.links.self ? ' period-highlighted' : ''}`}
              >
                <div className="d-flex align-items-center gap-2 min-w-0">
                  <i className="bi bi-calendar-range text-primary flex-shrink-0" aria-hidden="true" />
                  <div className="min-w-0">
                    <span className="fw-medium d-block">{formatDateRange(p.startDate, p.endDate, i18n.language)}</span>
                    <span className="small text-secondary">
                      ${p.dayPrice}/{t('owner.availability.perDay')} · {p.startPointStreet} {p.startPointNumber ?? ''} · {p.checkInTime}&ndash;{p.checkOutTime}
                    </span>
                  </div>
                </div>
                <div className="d-flex align-items-center gap-2 flex-shrink-0">
                  <button
                    type="button"
                    className="btn btn-sm btn-outline-secondary"
                    onClick={() => startEdit(p)}
                    disabled={busy}
                    aria-label={t('owner.availability.edit')}
                  >
                    <i className="bi bi-pencil" aria-hidden="true" />
                  </button>
                  <button
                    type="button"
                    className="btn btn-sm btn-outline-danger"
                    onClick={() => onWithdraw(p)}
                    disabled={busy}
                    aria-label={t('owner.availability.withdraw')}
                  >
                    <i className="bi bi-trash" aria-hidden="true" />
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}

        <div className="border-top pt-4">
          <h3 className="h6 fw-semibold mb-3">
            {editing ? t('owner.availability.editTitle') : t('owner.availability.createTitle')}
          </h3>
          <form onSubmit={onSubmit}>
            <div className="mb-3">
              <label className="form-label required-label" htmlFor="availFormRangePicker">
                {t('owner.availability.dateRange')}
              </label>
              <div id="availFormRangePicker" ref={rangePickerRef} className="owner-cal-container" />
              {(form.startDate || form.endDate) && (
                <p className="small text-secondary mt-2 mb-0">
                  {formatDateRange(form.startDate, form.endDate, i18n.language)}
                </p>
              )}
            </div>

            <div className="mb-3">
              <PriceMarketInsightCard
                insight={priceInsight}
                priceInputId="availPrice"
                value={form.dayPrice > 0 ? form.dayPrice : null}
                onPriceChange={(price) => update('dayPrice', price)}
              >
                <input
                  id="availPrice"
                  type="number"
                  min={0}
                  step="0.01"
                  className="form-control js-listing-price-decimal"
                  style={{ maxWidth: '12rem' }}
                  value={form.dayPrice === 0 ? '' : form.dayPrice}
                  onChange={(e) => {
                    const raw = e.target.value;
                    update('dayPrice', raw === '' ? 0 : Number(raw));
                  }}
                  required
                />
              </PriceMarketInsightCard>
            </div>

            <div className="mb-3">
              <label className="form-label" htmlFor="nb_dd_btn_availNeighborhood">
                {t('owner.availability.neighborhood')}
              </label>
              <NeighborhoodPicker
                pickerId="availNeighborhood"
                neighborhoodList={neighborhoodOptions}
                anyLabel={t('owner.publish.selectPlaceholder')}
                searchPlaceholder={t('search.filter.neighborhood.search')}
                selectFieldLabel={t('owner.availability.neighborhood')}
                toggleAriaLabel={t('owner.availability.neighborhood')}
                allowMultiple={false}
                selectedNeighborhoodId={selectedNeighborhoodId}
                onSelectionChange={(ids) => {
                  const raw = ids[0];
                  const match = neighborhoodOptions.find((n) => String(n.id) === String(raw));
                  update('neighborhoodUri', match?.uri ?? '');
                }}
              />
            </div>

            <div className="row g-3 mb-3">
              <div className="col-md-8">
                <label className="form-label required-label" htmlFor="availStreet">{t('owner.availability.street')}</label>
                <input id="availStreet" className="form-control" maxLength={listingLimits.addressStreetMaxLength} value={form.startPointStreet} onChange={(e) => update('startPointStreet', e.target.value)} required />
              </div>
              <div className="col-md-4">
                <label className="form-label" htmlFor="availNumber">{t('owner.availability.streetNumber')}</label>
                <input id="availNumber" className="form-control" maxLength={listingLimits.addressNumberMaxLength} value={form.startPointNumber ?? ''} onChange={(e) => update('startPointNumber', e.target.value)} />
              </div>
            </div>

            <div className="row g-3 mb-3">
              <div className="col-md-6">
                <label className="form-label required-label" htmlFor="availCheckIn">{t('owner.availability.checkIn')}</label>
                <input id="availCheckIn" type="time" className="form-control" value={form.checkInTime} onChange={(e) => update('checkInTime', e.target.value)} required />
              </div>
              <div className="col-md-6">
                <label className="form-label required-label" htmlFor="availCheckOut">{t('owner.availability.checkOut')}</label>
                <input id="availCheckOut" type="time" className="form-control" value={form.checkOutTime} onChange={(e) => update('checkOutTime', e.target.value)} required />
              </div>
            </div>

            <div className="d-flex justify-content-end gap-2 mt-4">
              {editing && (
                <Button type="button" variant="outline-secondary" onClick={resetForm} disabled={busy}>
                  {t('owner.availability.cancelEdit')}
                </Button>
              )}
              <Button type="submit" variant="primary" disabled={busy}>
                <i className="bi bi-check-lg me-1" aria-hidden="true" />
                {editing ? t('owner.availability.saveEdit') : t('owner.availability.create')}
              </Button>
            </div>
          </form>
        </div>
      </div>
    </article>
  );
}
