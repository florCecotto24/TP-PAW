import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import {
  createReservation,
  getCarSummary,
  getUser,
  idFromUri,
  listCarAvailabilities,
  uploadUserDocument,
  type AvailabilityView,
} from '../api';
import { formatDateTime, formatPrice } from '../format';
import { reservationActionError } from '../reservationError';
import { useCurrentUser } from '../useCurrentUser';
import { useSessionStore } from '../../../session/sessionStore';
import { paths, carDetail, myReservationDetail } from '../../../routes/paths';

function isoFromDateAndTime(date: string, time: string): string {
  if (!date || !time) return '';
  const local = `${date}T${time.length === 5 ? time : time.slice(0, 5)}`;
  const parsed = new Date(local);
  return Number.isNaN(parsed.getTime()) ? local : parsed.toISOString();
}

function billableDays(startIso: string, endIso: string): number {
  const a = new Date(startIso).getTime();
  const b = new Date(endIso).getTime();
  if (Number.isNaN(a) || Number.isNaN(b) || b <= a) return 0;
  return Math.max(1, Math.ceil((b - a) / (24 * 60 * 60 * 1000)));
}

export default function NewReservationPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { carId } = useParams<{ carId: string }>();
  const [searchParams] = useSearchParams();
  const { id: myId, isAuthenticated } = useCurrentUser();
  const userUri = useSessionStore((s) => s.currentUserUri);

  const carUri = carId ? `/cars/${carId}` : null;

  const carQuery = useQuery({
    queryKey: ['reservations', 'new', 'car', carUri],
    queryFn: () => getCarSummary(carUri as string).then((r) => r.data),
    enabled: !!carUri,
  });

  const availQuery = useQuery({
    queryKey: ['reservations', 'new', 'availabilities', carUri],
    queryFn: async () => {
      const res = await listCarAvailabilities(carUri as string);
      return (res.data ?? []).filter((a) => a.kind === 'offered' || a.kind == null);
    },
    enabled: !!carUri,
  });

  const userQuery = useQuery({
    queryKey: ['reservations', 'new', 'user', userUri],
    queryFn: () => getUser(userUri as string).then((r) => r.data),
    enabled: !!userUri,
  });

  const offered: AvailabilityView[] = availQuery.data ?? [];
  const initialAvailabilityId = searchParams.get('availabilityId') ?? '';
  const fromDateTimeParam =
    searchParams.get('fromDateTime') ?? searchParams.get('from') ?? '';
  const untilDateTimeParam =
    searchParams.get('untilDateTime') ?? searchParams.get('until') ?? '';
  const [availabilityUri, setAvailabilityUri] = useState('');
  const [startDate, setStartDate] = useState(fromDateTimeParam.slice(0, 10));
  const [endDate, setEndDate] = useState(untilDateTimeParam.slice(0, 10));
  const [submitting, setSubmitting] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);
  const [docBusy, setDocBusy] = useState<'license' | 'identity' | null>(null);

  const selectedAvailability = useMemo(() => {
    if (availabilityUri) {
      return offered.find((a) => a.links.self === availabilityUri) ?? null;
    }
    if (initialAvailabilityId) {
      return offered.find((a) => idFromUri(a.links.self) === initialAvailabilityId) ?? offered[0] ?? null;
    }
    return offered[0] ?? null;
  }, [offered, availabilityUri, initialAvailabilityId]);

  const effectiveAvailabilityUri = availabilityUri || selectedAvailability?.links.self || '';

  const startIso = fromDateTimeParam.includes('T')
    ? fromDateTimeParam
    : selectedAvailability
      ? isoFromDateAndTime(startDate, selectedAvailability.checkInTime ?? '10:00')
      : '';
  const endIso = untilDateTimeParam.includes('T')
    ? untilDateTimeParam
    : selectedAvailability
      ? isoFromDateAndTime(endDate, selectedAvailability.checkOutTime ?? '18:00')
      : '';

  const estimatedTotal =
    selectedAvailability && startIso && endIso
      ? selectedAvailability.dayPrice * billableDays(startIso, endIso)
      : null;

  const docsOk =
    userQuery.data?.licenseUploaded === true &&
    userQuery.data?.identityUploaded === true;

  const isOwnCar = Boolean(
    carQuery.data?.links.owner
    && myId
    && idFromUri(carQuery.data.links.owner) === myId,
  );

  const ownCarMessage = t('error.byCode.reservation.rider.cannotReserveOwnListing');

  const onUploadDoc = async (type: 'license' | 'identity', file: File) => {
    if (!userUri) return;
    setDocBusy(type);
    try {
      await uploadUserDocument(userUri, type, file);
      await queryClient.invalidateQueries({ queryKey: ['reservations', 'new', 'user', userUri] });
    } catch {
      setActionError(t('res.new.docs.saveFailed'));
    } finally {
      setDocBusy(null);
    }
  };

  const onConfirm = async () => {
    if (isOwnCar) {
      setActionError(ownCarMessage);
      return;
    }
    if (!carUri || !effectiveAvailabilityUri || !startIso || !endIso) {
      setActionError(t('res.new.missingParams'));
      return;
    }
    setSubmitting(true);
    setActionError(null);
    try {
      const res = await createReservation({
        carUri,
        availabilityUri: effectiveAvailabilityUri,
        startDate: startIso,
        endDate: endIso,
      });
      const reservationId = idFromUri(res.data?.links.self ?? res.location);
      if (reservationId) {
        navigate(myReservationDetail(reservationId));
      } else {
        navigate(paths.myReservations);
      }
    } catch (err) {
      setActionError(reservationActionError(t, err, 'new'));
    } finally {
      setSubmitting(false);
    }
  };

  if (!isAuthenticated) {
    return (
      <main className="container py-4">
        <div className="alert alert-warning">{t('res.list.needLogin')}</div>
        <Link to={paths.login} className="btn btn-primary">{t('res.list.login')}</Link>
      </main>
    );
  }

  if (!carId) {
    return (
      <main className="container py-4">
        <div className="alert alert-warning">{t('res.new.missingParams')}</div>
        <Link to={paths.search} className="btn btn-link">{t('res.new.backToSearch')}</Link>
      </main>
    );
  }

  const car = carQuery.data;
  const loading = carQuery.isLoading || availQuery.isLoading || userQuery.isLoading;

  return (
    <main className="container py-4">
      <div className="mb-3">
        <Link to={carDetail(carId)} className="btn btn-link ps-0">{t('res.new.back')}</Link>
      </div>

      <h1 className="h3 fw-semibold mb-4">{t('res.new.heading')}</h1>

      {loading ? <p className="text-secondary">{t('res.detail.loading')}</p> : null}
      {carQuery.isError || availQuery.isError ? (
        <div className="alert alert-danger">{t('res.detail.error')}</div>
      ) : null}

      {car && (
        <div className="row g-4">
          <div className="col-lg-7">
            <section className="bg-white rounded-4 shadow-sm p-4 mb-4">
              <h2 className="h5 fw-semibold mb-3">{t('res.new.datesTitle')}</h2>
              {offered.length === 0 ? (
                <p className="text-secondary mb-0">{t('browse.detail.noAvailability')}</p>
              ) : (
                <>
                  <div className="mb-3">
                    <label className="form-label small" htmlFor="availabilityPick">{t('browse.detail.availability')}</label>
                    <select
                      id="availabilityPick"
                      className="form-select form-select-sm"
                      value={effectiveAvailabilityUri}
                      onChange={(e) => setAvailabilityUri(e.target.value)}
                    >
                      {offered.map((a) => (
                        <option key={a.links.self} value={a.links.self}>
                          {a.startDate} — {a.endDate} · {formatPrice(a.dayPrice)} / {t('browse.detail.perDay')}
                        </option>
                      ))}
                    </select>
                  </div>
                  <div className="row g-3">
                    <div className="col-md-6">
                      <label className="form-label small" htmlFor="startDate">{t('res.new.from')}</label>
                      <input
                        id="startDate"
                        type="date"
                        className="form-control form-control-sm"
                        value={startDate}
                        onChange={(e) => setStartDate(e.target.value)}
                      />
                    </div>
                    <div className="col-md-6">
                      <label className="form-label small" htmlFor="endDate">{t('res.new.until')}</label>
                      <input
                        id="endDate"
                        type="date"
                        className="form-control form-control-sm"
                        value={endDate}
                        onChange={(e) => setEndDate(e.target.value)}
                      />
                    </div>
                  </div>
                  {selectedAvailability ? (
                    <p className="small text-secondary mt-3 mb-0">
                      {t('res.new.handover', {
                        checkIn: selectedAvailability.checkInTime ?? '—',
                        checkOut: selectedAvailability.checkOutTime ?? '—',
                      })}
                    </p>
                  ) : null}
                </>
              )}
            </section>

            <section className="bg-white rounded-4 shadow-sm p-4">
              <h2 className="h5 fw-semibold mb-3">{t('res.new.docs.title')}</h2>
              <p className="small text-secondary">{t('res.new.docs.intro')}</p>
              {(['license', 'identity'] as const).map((type) => {
                const uploaded = userQuery.data?.[`${type}Uploaded` as 'licenseUploaded'] === true;
                return (
                  <div key={type} className="mb-3">
                    <p className="form-label small mb-1">{t(`res.new.docs.${type}`)}</p>
                    {uploaded ? (
                      <p className="small text-success mb-0">✓ {t('res.new.docs.uploaded')}</p>
                    ) : (
                      <input
                        id={`doc-${type}`}
                        type="file"
                        className="form-control form-control-sm"
                        accept="image/*,application/pdf"
                        disabled={docBusy === type}
                        onChange={(e) => {
                          const f = e.target.files?.[0];
                          if (f) void onUploadDoc(type, f);
                        }}
                      />
                    )}
                  </div>
                );
              })}
            </section>
          </div>

          <div className="col-lg-5">
            <aside className="bg-white rounded-4 shadow-sm p-4 sticky-top" style={{ top: '1rem' }}>
              <h2 className="h5 fw-semibold mb-3">{t('res.new.summaryTitle')}</h2>
              <p className="mb-1">
                <span className="text-secondary small">{t('res.new.car')}: </span>
                <span className="fw-semibold">{car.brandName} {car.modelName}</span>
              </p>
              {selectedAvailability ? (
                <p className="small text-secondary mb-2">
                  {t('res.new.location')}: {selectedAvailability.startPointStreet}
                  {selectedAvailability.startPointNumber ? ` ${selectedAvailability.startPointNumber}` : ''}
                </p>
              ) : null}
              {startIso ? (
                <p className="small mb-1"><strong>{t('res.new.from')}:</strong> {formatDateTime(startIso)}</p>
              ) : null}
              {endIso ? (
                <p className="small mb-3"><strong>{t('res.new.until')}:</strong> {formatDateTime(endIso)}</p>
              ) : null}
              {estimatedTotal != null ? (
                <p className="fs-5 fw-bold mb-3">{t('res.new.total')}: {formatPrice(estimatedTotal)}</p>
              ) : null}
              {userQuery.data ? (
                <div className="small mb-3 pb-3 border-bottom">
                  <p className="fw-semibold mb-1">{t('res.new.account.title')}</p>
                  <p className="mb-0">{userQuery.data.forename} {userQuery.data.surname}</p>
                  <p className="mb-0 text-secondary">{userQuery.data.email}</p>
                </div>
              ) : null}
              <p className="small text-secondary">{t('res.new.paymentNotice')}</p>
              {isOwnCar || actionError ? (
                <div className="alert alert-danger py-2">{isOwnCar ? ownCarMessage : actionError}</div>
              ) : null}
              <button
                type="button"
                className="btn btn-primary w-100"
                disabled={
                  submitting
                  || isOwnCar
                  || !docsOk
                  || offered.length === 0
                  || !startDate
                  || !endDate
                }
                onClick={() => void onConfirm()}
              >
                {submitting ? t('res.new.submitting') : t('res.new.confirm')}
              </button>
              {!docsOk && userQuery.data ? (
                <p className="small text-warning mt-2 mb-0">{t('res.new.docs.intro')}</p>
              ) : null}
            </aside>
          </div>
        </div>
      )}
    </main>
  );
}
