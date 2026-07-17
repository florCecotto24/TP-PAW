import { useEffect, useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom';
import { Alert, Button, Modal } from 'react-bootstrap';
import { ApiError } from '../../api/client';
import { useSessionStore } from '../../session/sessionStore';
import {
  deactivateCar,
  deleteInsurance,
  fetchCar,
  fetchPictures,
  idFromUri,
  openInsurance,
  patchCar,
  uploadInsurance,
} from './api';
import { hasCbu, useApiErrorMessage, useCarReservationPreview, useCurrentUserId } from './hooks';
import { LoadingBlock, FieldView, ReceiptUploadPicker, AuthenticatedImg } from '../../components/ryden';
import ReservationListCard from '../reservations/components/ReservationListCard';
import AvailabilityManager from './AvailabilityManager';
import GalleryManager from './GalleryManager';
import { STATUS_BADGE, type CarDto, type PictureDto } from './types';
import { paths, carDetail, ownerReservationsCar } from '../../routes/paths';
import { resolveResourceUri } from '../../api/resourceUri';
import type { OwnerCarDetailLocationState } from '../../routes/navigationState';

export default function OwnerCarDetailPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const location = useLocation();
  const { id } = useParams<{ id: string }>();
  const carSelfFromNav = (location.state as OwnerCarDetailLocationState | null)?.carSelf;
  const carSelfUri = resolveResourceUri({
    stateUri: carSelfFromNav,
    routeId: id,
    collection: 'cars',
  });
  const errorMessage = useApiErrorMessage();
  const currentUser = useSessionStore((s) => s.currentUser);
  const ownerId = useCurrentUserId();

  const [car, setCar] = useState<CarDto | null>(null);
  const [coverImage, setCoverImage] = useState<PictureDto | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [showDeactivate, setShowDeactivate] = useState(false);
  const [showPause, setShowPause] = useState(false);
  const [showRemoveInsurance, setShowRemoveInsurance] = useState(false);
  const [insuranceSuccess, setInsuranceSuccess] = useState(false);
  const [insuranceRemoved, setInsuranceRemoved] = useState(false);
  const [insuranceViewError, setInsuranceViewError] = useState(false);
  const [insuranceViewBusy, setInsuranceViewBusy] = useState(false);

  const reservationsPreview = useCarReservationPreview(ownerId, id);

  // Edición de atributos (mismo patrón que perfil: vista → Editar → Cancelar/Guardar).
  const [editingAttributes, setEditingAttributes] = useState(false);
  const [attributesSaved, setAttributesSaved] = useState(false);
  const [description, setDescription] = useState('');
  const [minDays, setMinDays] = useState('1');

  useEffect(() => {
    if (!carSelfUri || !ownerId) return;
    let active = true;
    setCar(null);
    setCoverImage(null);
    setError(null);
    setEditingAttributes(false);
    setAttributesSaved(false);
    fetchCar(carSelfUri)
      .then((res) => {
        if (!active) return;
        const carOwnerId = idFromUri(res.data.links.owner);
        if (carOwnerId !== ownerId && currentUser?.role !== 'admin') {
          navigate(paths.myCars, { replace: true });
          return;
        }
        setCar(res.data);
        setDescription(res.data.description ?? '');
        setMinDays(String(res.data.minimumRentalDays ?? 1));
      })
      .catch((err) => {
        if (!active) return;
        if (err instanceof ApiError && (err.status === 403 || err.status === 404)) {
          navigate(paths.myCars, { replace: true });
          return;
        }
        setError(errorMessage(err, 'owner.detail.errors.loadFailed'));
      });
    return () => { active = false; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [carSelfUri, ownerId]);

  // Imagen de portada de la cabecera (reservation-detail-car-media del JSP):
  // la primera imagen de la galería, si hay alguna.
  useEffect(() => {
    if (!car) {
      setCoverImage(null);
      return;
    }
    let active = true;
    fetchPictures(car)
      .then((res) => {
        if (!active) return;
        setCoverImage((res.data ?? []).find((p) => p.kind === 'image') ?? null);
      })
      .catch(() => { /* la cabecera cae al ícono placeholder si falla */ });
    return () => { active = false; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [car?.links.self, ownerId]);

  function syncAttributesFromCar(next: CarDto) {
    setDescription(next.description ?? '');
    setMinDays(String(next.minimumRentalDays ?? 1));
  }

  function beginEditAttributes() {
    if (!car) return;
    syncAttributesFromCar(car);
    setAttributesSaved(false);
    setEditingAttributes(true);
  }

  function cancelEditAttributes() {
    if (car) syncAttributesFromCar(car);
    setEditingAttributes(false);
  }

  async function applyPatch(patch: Parameters<typeof patchCar>[1], failKey?: string) {
    const self = car?.links.self ?? carSelfUri;
    if (!self) return;
    setError(null);
    setBusy(true);
    try {
      const res = await patchCar(self, patch);
      setCar(res.data);
      syncAttributesFromCar(res.data);
      return res.data;
    } catch (err) {
      setError(errorMessage(err, failKey));
      return null;
    } finally {
      setBusy(false);
    }
  }

  // Reanudar (paused -> active) es directo; pausar (active -> paused) pide
  // confirmación, igual que el viejo myCarDetail (pauseListingModal).
  function onTogglePauseClick() {
    if (!car) return;
    if (car.status === 'paused') {
      void applyPatch({ status: 'active' }, 'owner.detail.errors.statusFailed');
    } else {
      setShowPause(true);
    }
  }

  function confirmPause() {
    setShowPause(false);
    void applyPatch({ status: 'paused' }, 'owner.detail.errors.statusFailed');
  }

  async function onSaveAttributes(e: FormEvent) {
    e.preventDefault();
    const saved = await applyPatch(
      { description: description.trim(), minimumRentalDays: Number(minDays) || 1 },
      'owner.detail.errors.saveFailed',
    );
    if (saved) {
      setEditingAttributes(false);
      setAttributesSaved(true);
    }
  }

  async function onDeactivate() {
    const self = car?.links.self ?? carSelfUri;
    if (!self || !car) return;
    setShowDeactivate(false);
    setBusy(true);
    setError(null);
    try {
      await deactivateCar(self);
      navigate(paths.myCars);
    } catch (err) {
      setError(errorMessage(err, 'owner.detail.errors.deactivateFailed'));
    } finally {
      setBusy(false);
    }
  }

  async function onInsuranceConfirm(file: File) {
    if (!car) return;
    const self = car.links.self ?? carSelfUri;
    setBusy(true);
    setError(null);
    setInsuranceSuccess(false);
    setInsuranceRemoved(false);
    try {
      await uploadInsurance(car, file);
      if (!self) return;
      const refreshed = await fetchCar(self);
      setCar(refreshed.data);
      setInsuranceSuccess(true);
    } catch (err) {
      setError(errorMessage(err, 'owner.detail.errors.insuranceFailed'));
      throw err;
    } finally {
      setBusy(false);
    }
  }

  async function onDeleteInsurance() {
    if (!car) return;
    const self = car.links.self ?? carSelfUri;
    setShowRemoveInsurance(false);
    setBusy(true);
    setError(null);
    setInsuranceSuccess(false);
    setInsuranceRemoved(false);
    try {
      await deleteInsurance(car);
      if (!self) return;
      const refreshed = await fetchCar(self);
      setCar(refreshed.data);
      setInsuranceRemoved(true);
    } catch (err) {
      setError(errorMessage(err, 'owner.detail.errors.insuranceDeleteFailed'));
    } finally {
      setBusy(false);
    }
  }

  async function onViewInsurance() {
    if (!car) return;
    setInsuranceViewError(false);
    setInsuranceViewBusy(true);
    try {
      const ok = await openInsurance(car);
      setInsuranceViewError(!ok);
    } finally {
      setInsuranceViewBusy(false);
    }
  }

  if (error && !car) {
    return (
      <main className="container pt-5 pb-4">
        <section className="reservation-management-header mb-4">
          <h1 className="h3 fw-bold mb-2">{t('owner.detail.title')}</h1>
        </section>
        <div className="alert alert-danger" role="alert">{error}</div>
      </main>
    );
  }

  if (!car) {
    return (
      <main className="container pt-5 pb-4">
        <section className="reservation-management-header mb-4">
          <h1 className="h3 fw-bold mb-2">{t('owner.detail.title')}</h1>
        </section>
        <LoadingBlock variant="page" className="py-4" />
      </main>
    );
  }

  const isPaused = car.status === 'paused';
  // Igual que myCarDetail.jsp: el bloqueo de cuenta (comprobante de devolución
  // vencido) pisa el estado ACTIVE/PAUSED/LACK_DOC en el sidebar, salvo que el
  // auto ya esté desactivado o pausado por un admin (esos estados mandan).
  const ownerBlocked = !!currentUser?.blocked && car.status !== 'deactivated' && car.status !== 'admin_paused';
  // Solo el owner puede pausar/despausar; admin_paused no es revertible por el owner.
  const canTogglePause = !ownerBlocked && (car.status === 'active' || car.status === 'paused');
  // admin_paused y deactivated no tienen ninguna acción disponible para el owner
  // (el admin_paused solo se revierte por soporte); antes se mostraba "Desactivar"
  // también en admin_paused, lo cual no refleja el JSP de referencia.
  const canDeactivate = car.status !== 'deactivated' && car.status !== 'admin_paused';
  const missingCbu = car.status === 'lack_doc' && !hasCbu(currentUser);

  return (
    <main className="container pt-5 pb-4">
      <nav aria-label="breadcrumb" className="mb-3">
        <ol className="breadcrumb mb-0">
          <li className="breadcrumb-item">
            <Link to={paths.myCars}>{t('owner.myCars.title')}</Link>
          </li>
          <li className="breadcrumb-item active" aria-current="page">
            {car.brandName} {car.modelName}
          </li>
        </ol>
      </nav>

      {error && <Alert variant="danger" role="alert">{error}</Alert>}

      <div className="row g-4 align-items-start">
        {/* Columna principal */}
        <div className="col-lg-8">
          {/* Cabecera del auto */}
          <article className="card border-0 shadow-sm rounded-4 mb-4 bg-white">
            <div className="card-body p-4">
              <div className="d-flex flex-column flex-md-row gap-3 align-items-start">
                <div className="reservation-detail-car-media rounded-3 overflow-hidden border flex-shrink-0">
                  {coverImage ? (
                    <AuthenticatedImg
                      src={coverImage.links.self}
                      alt={`${car.brandName} ${car.modelName}`}
                      className="w-100 h-100"
                      style={{ objectFit: 'cover' }}
                      fallback={
                        <div className="w-100 h-100 d-flex align-items-center justify-content-center text-secondary bg-body-tertiary">
                          <i className="bi bi-car-front fs-1" aria-hidden="true" />
                        </div>
                      }
                    />
                  ) : (
                    <div className="w-100 h-100 d-flex align-items-center justify-content-center text-secondary bg-body-tertiary">
                      <i className="bi bi-car-front fs-1" aria-hidden="true" />
                    </div>
                  )}
                </div>
                <div className="flex-grow-1 min-w-0">
                  <h2 className="h5 fw-semibold mb-2">
                    {car.brandName} {car.modelName}
                    {car.year ? <span className="text-secondary fw-normal"> ({car.year})</span> : null}
                  </h2>
                  <div className="d-flex flex-wrap gap-2 mb-3">
                    <span className="badge text-bg-light border">{t(`owner.enums.type.${car.type}`)}</span>
                    <span className="badge text-bg-light border">{t(`owner.enums.transmission.${car.transmission}`)}</span>
                    <span className="badge text-bg-light border">{t(`owner.enums.powertrain.${car.powertrain}`)}</span>
                  </div>
                  <div className="d-flex align-items-center gap-2">
                    <span className="small text-secondary">{t('owner.publish.plate')}:</span>
                    <span className="small fw-medium">{car.plate}</span>
                  </div>
                  {!car.modelValidated && (
                    <p className="small text-info-emphasis mt-3 mb-0">
                      <i className="bi bi-clock-history me-1" aria-hidden="true" />
                      {t('owner.detail.modelPending')}
                    </p>
                  )}
                </div>
              </div>
            </div>
          </article>

          {/* Mientras la marca/modelo está pendiente de aprobación, el viejo
              ocultaba edición / galería / disponibilidad y mostraba solo un aviso. */}
          {!car.modelValidated ? (
            <article className="card border-0 shadow-sm rounded-4 mb-4 bg-white">
              <div className="card-body p-4 d-flex flex-column align-items-center text-center gap-3">
                <i className="bi bi-clock-history fs-2 text-info-emphasis" aria-hidden="true" />
                <div>
                  <p className="fw-semibold mb-1">{t('owner.detail.modelPending')}</p>
                  <p className="small text-secondary mb-0">{t('owner.detail.modelPendingHint')}</p>
                </div>
              </div>
            </article>
          ) : (
          <>
          {/* Datos del auto: vista → Editar → Cancelar/Guardar (como perfil). */}
          <article className="card border-0 shadow-sm rounded-4 mb-4 bg-white">
            <div className="card-body p-4">
              <div className="d-flex align-items-center justify-content-between gap-2 mb-3">
                <h2 className="h5 fw-semibold mb-0">{t('owner.detail.dataSection')}</h2>
                {!editingAttributes ? (
                  <Button
                    type="button"
                    variant="outline-primary"
                    size="sm"
                    onClick={beginEditAttributes}
                    disabled={busy}
                  >
                    <i className="bi bi-pencil me-1" aria-hidden="true" />
                    {t('owner.detail.edit')}
                  </Button>
                ) : null}
              </div>
              {attributesSaved && !editingAttributes ? (
                <div className="alert alert-success py-2 small" role="alert">
                  {t('owner.detail.saved')}
                </div>
              ) : null}
              {!editingAttributes ? (
                <div className="d-flex flex-column gap-3">
                  <FieldView
                    label={t('owner.detail.description')}
                    value={
                      description.trim()
                        ? description
                        : t('owner.detail.notSpecified')
                    }
                    multiline
                  />
                  <FieldView
                    label={t('owner.detail.minimumRentalDays')}
                    value={String(Number(minDays) || 1)}
                  />
                </div>
              ) : (
                <form onSubmit={(e) => { void onSaveAttributes(e); }}>
                  <div className="mb-3">
                    <label className="form-label" htmlFor="detailDescription">{t('owner.detail.description')}</label>
                    <textarea
                      id="detailDescription"
                      className="form-control"
                      rows={3}
                      value={description}
                      onChange={(e) => setDescription(e.target.value)}
                    />
                  </div>
                  <div className="mb-3">
                    <label className="form-label" htmlFor="detailMinDays">{t('owner.detail.minimumRentalDays')}</label>
                    <input
                      id="detailMinDays"
                      type="number"
                      min={1}
                      className="form-control"
                      style={{ maxWidth: '10rem' }}
                      value={minDays}
                      onChange={(e) => setMinDays(e.target.value)}
                    />
                  </div>
                  <div className="d-flex justify-content-end gap-2">
                    <Button
                      type="button"
                      variant="outline-secondary"
                      onClick={cancelEditAttributes}
                      disabled={busy}
                    >
                      {t('owner.detail.cancel')}
                    </Button>
                    <Button type="submit" variant="primary" disabled={busy}>
                      <i className="bi bi-check-lg me-1" aria-hidden="true" />
                      {t('owner.detail.save')}
                    </Button>
                  </div>
                </form>
              )}
            </div>
          </article>

          <GalleryManager car={car} />
          <AvailabilityManager car={car} />
          </>
          )}

          <article className="card border-0 shadow-sm rounded-4 bg-white">
            <div className="card-body p-4">
              <div className="d-flex align-items-center justify-content-between mb-3 flex-wrap gap-2">
                <h2 className="h5 fw-semibold mb-0">{t('owner.detail.upcomingReservations.title')}</h2>
                {(reservationsPreview.data?.total ?? 0) > 0 && id ? (
                  <Link to={ownerReservationsCar(id)} className="btn btn-outline-primary btn-sm">
                    {t('owner.detail.upcomingReservations.seeAll')}
                  </Link>
                ) : null}
              </div>
              {reservationsPreview.isLoading ? (
                <LoadingBlock variant="inline" />
              ) : reservationsPreview.data?.preview.length ? (
                <div className="d-flex flex-column gap-3">
                  {reservationsPreview.data.preview.map((reservation) => (
                    <ReservationListCard
                      key={reservation.links.self}
                      reservation={reservation}
                      role="owner"
                    />
                  ))}
                </div>
              ) : (
                <p className="text-secondary small mb-0">{t('owner.detail.upcomingReservations.empty')}</p>
              )}
            </div>
          </article>
        </div>

        {/* Barra lateral: estado, acciones, seguro */}
        <div className="col-lg-4">
          <article className="card border-0 shadow-sm rounded-4 bg-white">
            <div className="card-body p-4 d-flex flex-column gap-3">
              <div>
                <span className="text-secondary small text-uppercase fw-semibold d-block mb-2" style={{ letterSpacing: '.04em' }}>
                  {t('owner.detail.statusLabel')}
                </span>
                {/* El bloqueo de cuenta pisa el badge de estado normal, igual que
                    myCarDetail.jsp (owner.blocked overrides ACTIVE/PAUSED/LACK_DOC). */}
                {ownerBlocked ? (
                  <span className="badge text-bg-danger w-100 py-2" style={{ whiteSpace: 'normal' }}>
                    {t('owner.detail.ownerBlockedBadge')}
                  </span>
                ) : (
                  <span className={`badge w-100 py-2 ${STATUS_BADGE[car.status]}`} style={{ whiteSpace: 'normal' }}>
                    {t(`owner.enums.status.${car.status}`)}
                  </span>
                )}
              </div>

              {id ? (
                <Link to={carDetail(id)} className="btn btn-outline-secondary w-100">
                  <i className="bi bi-eye me-2" aria-hidden="true" />
                  {t('owner.detail.viewCar')}
                </Link>
              ) : null}

              {canTogglePause && (
                <Button
                  variant={isPaused ? 'success' : 'warning'}
                  className="w-100"
                  onClick={onTogglePauseClick}
                  disabled={busy}
                >
                  <i className={`bi ${isPaused ? 'bi-play-fill' : 'bi-pause-fill'} me-2`} aria-hidden="true" />
                  {isPaused ? t('owner.detail.unpause') : t('owner.detail.pause')}
                </Button>
              )}

              {id ? (
                <Link to={ownerReservationsCar(id)} className="btn btn-outline-primary w-100">
                  <i className="bi bi-calendar-check me-2" aria-hidden="true" />
                  {t('owner.detail.viewReservations')}
                </Link>
              ) : null}

              {ownerBlocked && (
                <p className="text-secondary small mb-0">{t('owner.detail.ownerBlockedHint')}</p>
              )}

              {missingCbu && (
                <div>
                  <p className="text-secondary small mb-2">{t('owner.detail.missingCbuHint')}</p>
                  <Link to={paths.profile} className="btn btn-primary w-100">
                    <i className="bi bi-person-fill me-2" aria-hidden="true" />
                    {t('owner.detail.missingCbuCta')}
                  </Link>
                </div>
              )}

              {car.status === 'admin_paused' && (
                <p className="text-secondary small mb-0">{t('owner.detail.adminPausedHint')}</p>
              )}

              {car.status === 'deactivated' && (
                <p className="text-secondary small mb-0">{t('owner.detail.finishedHint')}</p>
              )}

              {canDeactivate && (
                <Button
                  variant="outline-danger"
                  className="w-100"
                  onClick={() => setShowDeactivate(true)}
                  disabled={busy}
                >
                  <i className="bi bi-x-circle me-2" aria-hidden="true" />
                  {t('owner.detail.deactivate')}
                </Button>
              )}

              {/* Seguro */}
              <div className="border rounded-3 p-3 bg-light">
                <div className="d-flex align-items-center gap-2 mb-2">
                  {car.hasInsurance ? (
                    <>
                      <i className="bi bi-shield-check text-success fs-5" aria-hidden="true" />
                      <span className="fw-semibold">{t('owner.detail.insurance.uploaded')}</span>
                    </>
                  ) : (
                    <>
                      <i className="bi bi-shield-exclamation text-warning fs-5" aria-hidden="true" />
                      <span className="fw-semibold">{t('owner.detail.insurance.missing')}</span>
                    </>
                  )}
                </div>
                <p className="small text-secondary mb-2">{t('owner.detail.insurance.hint')}</p>
                {car.hasInsurance ? (
                  <div className="d-flex flex-wrap gap-2 mb-2">
                    <button
                      type="button"
                      className="btn btn-sm btn-outline-primary"
                      onClick={() => void onViewInsurance()}
                      disabled={insuranceViewBusy || busy}
                    >
                      {t('owner.detail.insurance.viewDocument')}
                    </button>
                    <button
                      type="button"
                      className="btn btn-sm btn-outline-danger"
                      onClick={() => setShowRemoveInsurance(true)}
                      disabled={busy}
                    >
                      {t('owner.detail.insurance.removeDocument')}
                    </button>
                  </div>
                ) : null}
                <label className="form-label small mb-1" htmlFor="detailInsurance">
                  {car.hasInsurance ? t('owner.detail.insurance.replace') : t('owner.detail.insurance.upload')}
                </label>
                <ReceiptUploadPicker
                  id="detailInsurance"
                  disabled={busy}
                  busy={busy}
                  onConfirm={onInsuranceConfirm}
                  labels={{
                    chooseFile: t('owner.detail.insurance.chooseFile'),
                    confirmUpload: t('owner.detail.insurance.confirmUpload'),
                    confirming: t('owner.detail.insurance.confirming'),
                    uploadAria: car.hasInsurance
                      ? t('owner.detail.insurance.replace')
                      : t('owner.detail.insurance.upload'),
                    replaceFile: t('owner.detail.insurance.replaceFile'),
                    removeFile: t('owner.detail.insurance.removeFile'),
                    invalidFile: t('owner.detail.insurance.invalidFile'),
                    fileTooLarge: t('owner.detail.insurance.fileTooLarge', { maxMb: 5 }),
                    uploadError: t('owner.detail.errors.insuranceFailed'),
                  }}
                />
                {insuranceSuccess ? (
                  <div className="alert alert-success mt-2 mb-0 py-2 small" role="alert">
                    {t('owner.detail.insurance.uploadSuccess')}
                  </div>
                ) : null}
                {insuranceRemoved ? (
                  <div className="alert alert-success mt-2 mb-0 py-2 small" role="alert">
                    {t('owner.detail.insurance.removeSuccess')}
                  </div>
                ) : null}
                {insuranceViewError ? (
                  <div className="alert alert-warning mt-2 mb-0 py-2 small" role="alert">
                    {t('owner.detail.insurance.viewError')}
                  </div>
                ) : null}
              </div>
            </div>
          </article>
        </div>
      </div>

      <Modal show={showDeactivate} onHide={() => setShowDeactivate(false)} centered>
        <Modal.Header closeButton>
          <Modal.Title className="fs-5 fw-semibold">{t('owner.detail.deactivate')}</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <p className="mb-0 text-secondary">{t('owner.detail.confirmDeactivate')}</p>
        </Modal.Body>
        <Modal.Footer className="border-0 pt-0">
          <Button variant="outline-secondary" onClick={() => setShowDeactivate(false)} disabled={busy}>
            {t('owner.detail.cancel')}
          </Button>
          <Button variant="danger" onClick={onDeactivate} disabled={busy}>
            {t('owner.detail.deactivate')}
          </Button>
        </Modal.Footer>
      </Modal>

      <Modal show={showPause} onHide={() => setShowPause(false)} centered>
        <Modal.Header closeButton>
          <Modal.Title className="fs-5 fw-semibold">{t('owner.detail.pause')}</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <p className="mb-0 text-secondary">{t('owner.detail.confirmPause')}</p>
        </Modal.Body>
        <Modal.Footer className="border-0 pt-0">
          <Button variant="outline-secondary" onClick={() => setShowPause(false)} disabled={busy}>
            {t('owner.detail.cancel')}
          </Button>
          <Button variant="warning" onClick={confirmPause} disabled={busy}>
            {t('owner.detail.pause')}
          </Button>
        </Modal.Footer>
      </Modal>

      <Modal show={showRemoveInsurance} onHide={() => setShowRemoveInsurance(false)} centered>
        <Modal.Header closeButton>
          <Modal.Title className="fs-5 fw-semibold">
            {t('owner.detail.insurance.removeDocument')}
          </Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <p className="mb-0 text-secondary">{t('owner.detail.insurance.removeConfirm')}</p>
        </Modal.Body>
        <Modal.Footer className="border-0 pt-0">
          <Button
            variant="outline-secondary"
            onClick={() => setShowRemoveInsurance(false)}
            disabled={busy}
          >
            {t('owner.detail.cancel')}
          </Button>
          <Button variant="danger" onClick={() => void onDeleteInsurance()} disabled={busy}>
            {t('owner.detail.insurance.removeDocument')}
          </Button>
        </Modal.Footer>
      </Modal>
    </main>
  );
}
