import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import BreadcrumbTrail from '../../../components/ryden/layout/BreadcrumbTrail';
import DocumentPromptModal from '../../../components/ryden/primitives/DocumentPromptModal';
import {
  createReservation,
  getCarSummary,
  getUser,
  idFromUri,
  listCarAvailabilities,
  uploadUserDocument,
  type AvailabilityView,
} from '../api';
import { formatPrice } from '../format';
import { formatDateTime } from '../../../i18n/dateFormat';
import { reservationActionError } from '../reservationError';
import { useCurrentUser } from '../useCurrentUser';
import { useSessionStore } from '../../../session/sessionStore';
import { paths, carDetail, reservationConfirmation } from '../../../routes/paths';

function billableDays(startIso: string, endIso: string): number {
  const a = new Date(startIso).getTime();
  const b = new Date(endIso).getTime();
  if (Number.isNaN(a) || Number.isNaN(b) || b <= a) return 0;
  return Math.max(1, Math.ceil((b - a) / (24 * 60 * 60 * 1000)));
}

/** ¿El rango [startYmd, endYmd] cae dentro del período de disponibilidad `a`? */
function coversRange(a: AvailabilityView, startYmd: string, endYmd: string): boolean {
  if (!startYmd || !endYmd) return false;
  return a.startDate <= startYmd && endYmd <= a.endDate;
}

export default function NewReservationPage() {
  const { t, i18n } = useTranslation();
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
  const carNameParam = searchParams.get('carName') ?? '';
  const totalParam = searchParams.get('reservationTotal');

  const startDate = fromDateTimeParam.slice(0, 10);
  const endDate = untilDateTimeParam.slice(0, 10);

  const selectedAvailability = useMemo(() => {
    if (initialAvailabilityId) {
      return offered.find((a) => idFromUri(a.links.self) === initialAvailabilityId) ?? null;
    }
    if (startDate && endDate) {
      return offered.find((a) => coversRange(a, startDate, endDate)) ?? null;
    }
    return offered[0] ?? null;
  }, [offered, initialAvailabilityId, startDate, endDate]);

  const [submitting, setSubmitting] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);
  const [docsModalOpen, setDocsModalOpen] = useState(false);
  const [docsSaving, setDocsSaving] = useState(false);
  const [docsError, setDocsError] = useState<string | null>(null);
  const [pendingLicenseFile, setPendingLicenseFile] = useState<File | null>(null);
  const [pendingIdentityFile, setPendingIdentityFile] = useState<File | null>(null);

  const startIso = fromDateTimeParam.includes('T') ? fromDateTimeParam : startDate;
  const endIso = untilDateTimeParam.includes('T') ? untilDateTimeParam : endDate;

  const clientTotal = totalParam != null && totalParam !== '' ? Number(totalParam) : null;
  const estimatedTotal =
    clientTotal != null && Number.isFinite(clientTotal)
      ? clientTotal
      : selectedAvailability && startIso && endIso
        ? selectedAvailability.dayPrice * billableDays(startIso, endIso)
        : null;

  const licenseUploaded = userQuery.data?.licenseUploaded === true;
  const identityUploaded = userQuery.data?.identityUploaded === true;
  const docsOk = licenseUploaded && identityUploaded;

  const isOwnCar = Boolean(
    carQuery.data?.links.owner
    && myId
    && idFromUri(carQuery.data.links.owner) === myId,
  );

  const ownCarMessage = t('error.byCode.reservation.rider.cannotReserveOwnListing');

  const carName = carNameParam || (carQuery.data ? `${carQuery.data.brandName} ${carQuery.data.modelName}` : '');

  const submitReservation = async () => {
    if (!carUri || !selectedAvailability || !startIso || !endIso) {
      setActionError(t('res.new.missingParams'));
      return;
    }
    setSubmitting(true);
    setActionError(null);
    try {
      const res = await createReservation({
        carUri,
        availabilityUri: selectedAvailability.links.self,
        startDate: startIso,
        endDate: endIso,
      });
      const reservationId = idFromUri(res.data?.links.self ?? res.location);
      navigate(reservationId ? reservationConfirmation(reservationId) : paths.myReservations);
    } catch (err) {
      setActionError(reservationActionError(t, err, 'new'));
    } finally {
      setSubmitting(false);
    }
  };

  const onSubmit = () => {
    setActionError(null);
    if (isOwnCar) {
      setActionError(ownCarMessage);
      return;
    }
    if (!docsOk) {
      setDocsError(null);
      setDocsModalOpen(true);
      return;
    }
    void submitReservation();
  };

  const onSaveDocs = async () => {
    if (!userUri) return;
    const needLicense = !licenseUploaded;
    const needIdentity = !identityUploaded;
    if ((needLicense && !pendingLicenseFile) || (needIdentity && !pendingIdentityFile)) {
      if (needLicense && needIdentity) setDocsError(t('res.new.docs.needBoth'));
      else if (needLicense) setDocsError(t('res.new.docs.needLicense'));
      else setDocsError(t('res.new.docs.needIdentity'));
      return;
    }
    setDocsSaving(true);
    setDocsError(null);
    try {
      if (needLicense && pendingLicenseFile) {
        await uploadUserDocument(userUri, 'license', pendingLicenseFile);
      }
      if (needIdentity && pendingIdentityFile) {
        await uploadUserDocument(userUri, 'identity', pendingIdentityFile);
      }
      await queryClient.invalidateQueries({ queryKey: ['reservations', 'new', 'user', userUri] });
      setDocsModalOpen(false);
      setPendingLicenseFile(null);
      setPendingIdentityFile(null);
      await submitReservation();
    } catch {
      setDocsError(t('res.new.docs.saveFailed'));
    } finally {
      setDocsSaving(false);
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

  const loading = carQuery.isLoading || availQuery.isLoading || userQuery.isLoading;

  return (
    <main className="container py-5">
      <BreadcrumbTrail
        midLabel={carName}
        midHref={carDetail(carId)}
        currentLabel={t('res.new.title')}
      />
      <div className="row justify-content-center">
        <div className="col-md-8 col-lg-6">
          <div className="card border-0 shadow-sm rounded-4 bg-white">
            <div className="card-body p-4 p-md-5">
              {loading ? <p className="text-secondary">{t('res.detail.loading')}</p> : null}
              {carQuery.isError || availQuery.isError ? (
                <div className="alert alert-danger">{t('res.detail.error')}</div>
              ) : null}

              {!loading && (
                <>
                  <h1 className="h4 fw-bold mb-2">{t('res.new.heading')}</h1>
                  <p>
                    {t('res.new.descriptionBefore')} <strong>{carName}</strong>
                    {t('res.new.descriptionAfter')}
                  </p>

                  <p className="text-secondary small mt-3 mb-1">{t('res.new.paymentProofNotice')}</p>

                  {actionError ? (
                    <div className="alert alert-danger mt-3" role="alert">{actionError}</div>
                  ) : null}

                  {offered.length === 0 || !selectedAvailability ? (
                    <div className="alert alert-warning mt-3 mb-0">{t('browse.detail.noAvailability')}</div>
                  ) : (
                    <>
                      <div className="border rounded-3 p-3 bg-cream mb-4 mt-3">
                        <h2 className="h6 fw-bold mb-2">{t('res.new.summaryTitle')}</h2>
                        <p className="mb-1"><strong>{t('res.new.car')}</strong> {carName}</p>
                        <p className="mb-1">
                          <strong>{t('res.new.pickupReturn')}</strong>{' '}
                          {startIso ? formatDateTime(startIso, i18n.language) : fromDateTimeParam}
                          {' → '}
                          {endIso ? formatDateTime(endIso, i18n.language) : untilDateTimeParam}
                        </p>
                        <p className="mb-1">
                          <strong>{t('res.new.location')}</strong> {selectedAvailability.startPointStreet}
                          {selectedAvailability.startPointNumber ? ` ${selectedAvailability.startPointNumber}` : ''}
                        </p>
                        {estimatedTotal != null ? (
                          <p className="mb-0"><strong>{t('res.new.total')}</strong> {formatPrice(estimatedTotal)}</p>
                        ) : null}
                      </div>

                      <div className="border rounded-3 p-3 bg-cream mb-3">
                        <h2 className="h6 fw-bold mb-2">{t('res.new.account.title')}</h2>
                        <p className="mb-1">
                          <strong>{t('res.new.account.name')}</strong>{' '}
                          {userQuery.data?.forename} {userQuery.data?.surname}
                        </p>
                        <p className="mb-0">
                          <strong>{t('res.new.account.email')}</strong> {userQuery.data?.email}
                        </p>
                      </div>

                      <div className="d-flex gap-2 mt-2">
                        <Link to={carDetail(carId)} className="btn btn-outline-secondary w-50">
                          {t('common.back')}
                        </Link>
                        <button
                          type="button"
                          className="btn btn-primary w-50"
                          disabled={submitting || isOwnCar}
                          onClick={onSubmit}
                        >
                          {submitting ? t('res.new.submitting') : t('res.new.confirm')}
                        </button>
                      </div>
                    </>
                  )}
                </>
              )}
            </div>
          </div>
        </div>
      </div>

      <DocumentPromptModal
        id="reservationMissingDocsModal"
        title={t('res.new.docs.title')}
        licenseLabel={t('res.new.docs.license')}
        identityLabel={t('res.new.docs.identity')}
        uploadedSlotMessage={t('res.new.docs.uploaded')}
        licensePending={!licenseUploaded}
        identityPending={!identityUploaded}
        hideLicenseSlot={licenseUploaded}
        hideIdentitySlot={identityUploaded}
        cancelLabel={t('res.new.docs.cancel')}
        confirmLabel={t('res.new.docs.save')}
        errorId="reservationMissingDocsError"
        open={docsModalOpen}
        onOpenChange={setDocsModalOpen}
        error={docsError ?? undefined}
        onLicenseChange={setPendingLicenseFile}
        onIdentityChange={setPendingIdentityFile}
        onConfirm={() => void onSaveDocs()}
        confirmButtonClass={`btn btn-primary${docsSaving ? ' disabled' : ''}`}
      >
        <p className="mb-2 text-secondary text-center">{t('res.new.docs.modalBody')}</p>
        <p className="mb-3 text-secondary text-center">{t('res.new.docs.modalBodyFormats')}</p>
      </DocumentPromptModal>
    </main>
  );
}
