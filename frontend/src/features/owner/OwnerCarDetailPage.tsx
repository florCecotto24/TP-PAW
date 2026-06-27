import { useEffect, useState, type ChangeEvent, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { Alert, Button, Modal } from 'react-bootstrap';
import {
  deactivateCar,
  fetchCar,
  patchCar,
  uploadInsurance,
} from './api';
import { useApiErrorMessage } from './hooks';
import AvailabilityManager from './AvailabilityManager';
import GalleryManager from './GalleryManager';
import { STATUS_BADGE, type CarDto } from './types';

export default function OwnerCarDetailPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const errorMessage = useApiErrorMessage();

  const [car, setCar] = useState<CarDto | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [showDeactivate, setShowDeactivate] = useState(false);
  const [showPause, setShowPause] = useState(false);

  // Edición de atributos.
  const [description, setDescription] = useState('');
  const [minDays, setMinDays] = useState('1');

  useEffect(() => {
    if (!id) return;
    let active = true;
    setError(null);
    fetchCar(id)
      .then((res) => {
        if (!active) return;
        setCar(res.data);
        setDescription(res.data.description ?? '');
        setMinDays(String(res.data.minimumRentalDays ?? 1));
      })
      .catch((err) => { if (active) setError(errorMessage(err, 'owner.detail.errors.loadFailed')); });
    return () => { active = false; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  async function applyPatch(patch: Parameters<typeof patchCar>[1], failKey?: string) {
    if (!id) return;
    setError(null);
    setBusy(true);
    try {
      const res = await patchCar(id, patch);
      setCar(res.data);
    } catch (err) {
      setError(errorMessage(err, failKey));
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

  function onSaveAttributes(e: FormEvent) {
    e.preventDefault();
    void applyPatch(
      { description: description.trim(), minimumRentalDays: Number(minDays) || 1 },
      'owner.detail.errors.saveFailed',
    );
  }

  async function onDeactivate() {
    if (!id || !car) return;
    setShowDeactivate(false);
    setBusy(true);
    setError(null);
    try {
      await deactivateCar(id);
      navigate('/mis-autos');
    } catch (err) {
      setError(errorMessage(err, 'owner.detail.errors.deactivateFailed'));
    } finally {
      setBusy(false);
    }
  }

  async function onInsurance(e: ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (!file || !car) return;
    setBusy(true);
    setError(null);
    try {
      await uploadInsurance(car, file);
    } catch (err) {
      setError(errorMessage(err, 'owner.detail.errors.insuranceFailed'));
    } finally {
      setBusy(false);
      e.target.value = '';
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
        <p className="text-secondary" role="status">{t('app.loading')}</p>
      </main>
    );
  }

  const isPaused = car.status === 'paused';
  // Solo el owner puede pausar/despausar; admin_paused no es revertible por el owner.
  const canTogglePause = car.status === 'active' || car.status === 'paused';
  const isDeactivated = car.status === 'deactivated';

  return (
    <main className="container pt-5 pb-4">
      <nav aria-label="breadcrumb" className="mb-3">
        <ol className="breadcrumb mb-0">
          <li className="breadcrumb-item">
            <Link to="/mis-autos">{t('owner.myCars.title')}</Link>
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
              <div className="d-flex align-items-start gap-3 mb-3 flex-wrap">
                <h2 className="h4 fw-semibold mb-0 flex-grow-1 min-w-0">
                  {car.brandName} {car.modelName}
                  {car.year ? <span className="text-secondary fw-normal"> ({car.year})</span> : null}
                </h2>
                <span className={`badge ${STATUS_BADGE[car.status]}`}>
                  {t(`owner.enums.status.${car.status}`)}
                </span>
              </div>
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
          {/* Editar datos */}
          <article className="card border-0 shadow-sm rounded-4 mb-4 bg-white">
            <div className="card-body p-4">
              <h2 className="h5 fw-semibold mb-3">{t('owner.detail.editSection')}</h2>
              <form onSubmit={onSaveAttributes}>
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
                <div className="d-flex justify-content-end">
                  <Button type="submit" variant="primary" disabled={busy}>
                    <i className="bi bi-check-lg me-1" aria-hidden="true" />
                    {t('owner.detail.save')}
                  </Button>
                </div>
              </form>
            </div>
          </article>

          <GalleryManager car={car} />
          <AvailabilityManager car={car} />
          </>
          )}
        </div>

        {/* Barra lateral: estado, acciones, seguro */}
        <div className="col-lg-4">
          <article className="card border-0 shadow-sm rounded-4 bg-white">
            <div className="card-body p-4 d-flex flex-column gap-3">
              <div>
                <span className="text-secondary small text-uppercase fw-semibold d-block mb-2" style={{ letterSpacing: '.04em' }}>
                  {t('owner.detail.statusLabel')}
                </span>
                <span className={`badge w-100 py-2 ${STATUS_BADGE[car.status]}`} style={{ whiteSpace: 'normal' }}>
                  {t(`owner.enums.status.${car.status}`)}
                </span>
              </div>

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

              {!isDeactivated && (
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
                  <i className="bi bi-shield-check text-secondary fs-5" aria-hidden="true" />
                  <span className="fw-semibold">{t('owner.detail.insuranceSection')}</span>
                </div>
                <label className="form-label small mb-1" htmlFor="detailInsurance">{t('owner.detail.uploadInsurance')}</label>
                <input
                  id="detailInsurance"
                  type="file"
                  className="form-control form-control-sm"
                  disabled={busy}
                  onChange={onInsurance}
                />
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
    </main>
  );
}
