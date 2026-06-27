import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from 'react-bootstrap';
import type { Options } from 'flatpickr/dist/types/options';
import FlatpickrCalendar, { compactPrice, dayStartFromYmd, dayEndFromYmd, ymd } from '../../components/FlatpickrCalendar';
import {
  createAvailability,
  deleteAvailability,
  fetchAvailabilities,
  fetchNeighborhoods,
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
  neighborhoodId: undefined,
  checkInTime: '10:00',
  checkOutTime: '20:00',
};

export default function AvailabilityManager({ car }: { car: CarDto }) {
  const { t } = useTranslation();
  const errorMessage = useApiErrorMessage();

  const [month, setMonth] = useState(currentMonth());
  const [periods, setPeriods] = useState<AvailabilityDto[]>([]);
  const [neighborhoods, setNeighborhoods] = useState<NeighborhoodDto[]>([]);
  const [form, setForm] = useState<AvailabilityCreateDto>(EMPTY_FORM);
  const [editing, setEditing] = useState<AvailabilityDto | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [reloadTick, setReloadTick] = useState(0);
  const [highlightedSelf, setHighlightedSelf] = useState<string | null>(null);

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
    const nbId = nbLink
      ? Number(nbLink.split('?')[0].replace(/\/+$/, '').split('/').pop())
      : undefined;
    setForm({
      startDate: p.startDate,
      endDate: p.endDate,
      dayPrice: p.dayPrice,
      startPointStreet: p.startPointStreet,
      startPointNumber: p.startPointNumber ?? '',
      neighborhoodId: nbId && !Number.isNaN(nbId) ? nbId : undefined,
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
    // Validaciones client-side que el JSP mostraba inline (form:errors).
    if (!form.startDate || !form.endDate) {
      setError(t('owner.availability.errors.datesRequired'));
      return;
    }
    if (form.endDate < form.startDate) {
      setError(t('owner.availability.errors.invalidDateRange'));
      return;
    }
    if (!(form.dayPrice > 0)) {
      setError(t('owner.availability.errors.priceInvalid'));
      return;
    }
    if (!form.startPointStreet?.trim()) {
      setError(t('owner.availability.errors.streetRequired'));
      return;
    }
    setBusy(true);
    try {
      if (editing) await updateAvailability(editing, form);
      else await createAvailability(car, form);
      resetForm();
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
          <div className="d-flex align-items-center gap-2">
            <label className="form-label small mb-0 text-secondary" htmlFor="availMonth">
              {t('owner.availability.month')}
            </label>
            <input
              id="availMonth"
              type="month"
              className="form-control form-control-sm"
              style={{ width: 'auto' }}
              value={month}
              onChange={(e) => setMonth(e.target.value)}
            />
          </div>
        </div>

        {error && <div className="alert alert-danger py-2 small" role="alert">{error}</div>}

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
                    <span className="fw-medium d-block">{p.startDate} &ndash; {p.endDate}</span>
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
            <div className="row g-3 mb-3">
              <div className="col-md-6">
                <label className="form-label required-label" htmlFor="availStart">{t('owner.availability.startDate')}</label>
                <input id="availStart" type="date" className="form-control" value={form.startDate} onChange={(e) => update('startDate', e.target.value)} required />
              </div>
              <div className="col-md-6">
                <label className="form-label required-label" htmlFor="availEnd">{t('owner.availability.endDate')}</label>
                <input id="availEnd" type="date" className="form-control" value={form.endDate} onChange={(e) => update('endDate', e.target.value)} required />
              </div>
            </div>

            <div className="mb-3">
              <label className="form-label required-label" htmlFor="availPrice">{t('owner.availability.dayPrice')}</label>
              <input
                id="availPrice"
                type="number"
                min={0}
                step="0.01"
                className="form-control"
                style={{ maxWidth: '12rem' }}
                value={form.dayPrice}
                onChange={(e) => update('dayPrice', Number(e.target.value))}
                required
              />
            </div>

            <div className="mb-3">
              <label className="form-label" htmlFor="availNeighborhood">{t('owner.availability.neighborhood')}</label>
              <select
                id="availNeighborhood"
                className="form-select"
                value={form.neighborhoodId ?? ''}
                onChange={(e) => update('neighborhoodId', e.target.value ? Number(e.target.value) : undefined)}
              >
                <option value="">{t('owner.publish.selectPlaceholder')}</option>
                {neighborhoods.map((n) => {
                  const nid = n.links.self.split('?')[0].replace(/\/+$/, '').split('/').pop();
                  return <option key={n.links.self} value={nid}>{n.name}</option>;
                })}
              </select>
            </div>

            <div className="row g-3 mb-3">
              <div className="col-md-8">
                <label className="form-label required-label" htmlFor="availStreet">{t('owner.availability.street')}</label>
                <input id="availStreet" className="form-control" value={form.startPointStreet} onChange={(e) => update('startPointStreet', e.target.value)} required />
              </div>
              <div className="col-md-4">
                <label className="form-label" htmlFor="availNumber">{t('owner.availability.streetNumber')}</label>
                <input id="availNumber" className="form-control" value={form.startPointNumber ?? ''} onChange={(e) => update('startPointNumber', e.target.value)} />
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
