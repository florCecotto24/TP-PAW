import { useEffect, useState, type ChangeEvent, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { Alert, Button } from 'react-bootstrap';
import { useSessionStore } from '../../session/sessionStore';
import type { UserDto } from '../../api/types';
import {
  createBrand,
  createModel,
  fetchBrands,
  fetchCar,
  fetchModels,
  fetchUser,
  idFromUri,
  patchCbu,
  publishCar,
  uploadIdentityDocument,
} from './api';
import GalleryPicker from './GalleryPicker';
import { useApiErrorMessage } from './hooks';
import type {
  BrandDto,
  CarCreateDto,
  CarDto,
  CarType,
  ModelDto,
  Powertrain,
  Transmission,
} from './types';

const CAR_TYPES: CarType[] = [
  'sedan', 'hatchback', 'suv', 'coupe', 'convertible', 'wagon', 'van', 'pickup',
];
const TRANSMISSIONS: Transmission[] = ['manual', 'automatic', 'semi_automatic'];
const POWERTRAINS: Powertrain[] = ['gasoline', 'diesel', 'electric', 'hybrid', 'cng'];

const OTHER = '__other__';

// Un usuario cumple los prerequisitos de publicación cuando tiene identidad
// cargada/validada Y un CBU. `identityValidated` lo fija el admin, pero el viejo
// flujo dejaba publicar apenas el documento estaba subido; como el DTO no expone
// "documento subido pero no validado", usamos identityValidated como señal y, si
// el owner sube el documento en esta misma pantalla, marcamos el paso como hecho
// localmente (identityJustUploaded) para no bloquearlo.
function hasCbu(user: UserDto | null): boolean {
  return !!user?.cbu && user.cbu.trim().length > 0;
}

// =============================================================================
// Coordinador: decide entre (1) prerequisitos, (2) formulario, (3) resultado.
// =============================================================================
export default function PublishCarPage() {
  const { t } = useTranslation();
  const errorMessage = useApiErrorMessage();
  const userUri = useSessionStore((s) => s.currentUserUri);
  const sessionUser = useSessionStore((s) => s.currentUser);

  const [user, setUser] = useState<UserDto | null>(sessionUser);
  const [identityJustUploaded, setIdentityJustUploaded] = useState(false);
  const [loading, setLoading] = useState(true);
  const [published, setPublished] = useState<CarDto | null>(null);

  // Releemos el usuario para tener cbu/identityValidated frescos (el del store
  // puede estar desactualizado tras cargar documento/cbu en otra pantalla).
  useEffect(() => {
    if (!userUri) { setLoading(false); return; }
    let active = true;
    setLoading(true);
    fetchUser(userUri)
      .then((res) => { if (active) setUser(res.data); })
      .catch(() => { if (active) setUser(sessionUser); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [userUri]);

  if (!userUri) {
    return (
      <main className="container py-5">
        <div className="alert alert-warning" role="alert">{t('owner.errors.notAuthenticated')}</div>
      </main>
    );
  }

  if (loading) {
    return (
      <main className="container py-5">
        <p className="text-secondary" role="status">{t('app.loading')}</p>
      </main>
    );
  }

  // (3) Resultado tras publicar.
  if (published) {
    return <PublishResult car={published} user={user} />;
  }

  const cbuOk = hasCbu(user);
  const identityOk = !!user?.identityValidated || identityJustUploaded;

  // (1) Prerequisitos no cumplidos.
  if (!cbuOk || !identityOk) {
    return (
      <PublishPrerequisites
        user={user}
        cbuOk={cbuOk}
        identityOk={identityOk}
        errorMessage={errorMessage}
        onUserUpdated={(u) => setUser(u)}
        onIdentityUploaded={() => setIdentityJustUploaded(true)}
      />
    );
  }

  // (2) Formulario de publicación.
  return (
    <PublishCarForm
      onPublished={(car) => {
        setPublished(car);
        window.scrollTo({ top: 0 });
      }}
    />
  );
}

// =============================================================================
// (1) Prerequisitos: cargar CBU y subir documento de identidad.
// Replica publishCarPrerequisites.jsp (checklist + acciones inline).
// =============================================================================
function PublishPrerequisites({
  user,
  cbuOk,
  identityOk,
  errorMessage,
  onUserUpdated,
  onIdentityUploaded,
}: {
  user: UserDto | null;
  cbuOk: boolean;
  identityOk: boolean;
  errorMessage: (err: unknown, fallbackKey?: string) => string;
  onUserUpdated: (u: UserDto) => void;
  onIdentityUploaded: () => void;
}) {
  const { t } = useTranslation();
  const userUri = useSessionStore((s) => s.currentUserUri);

  const [cbu, setCbu] = useState('');
  const [cbuError, setCbuError] = useState<string | null>(null);
  const [cbuBusy, setCbuBusy] = useState(false);

  const [identityError, setIdentityError] = useState<string | null>(null);
  const [identityBusy, setIdentityBusy] = useState(false);

  async function onSaveCbu(e: FormEvent) {
    e.preventDefault();
    setCbuError(null);
    if (!cbu.trim()) { setCbuError(t('owner.prereq.errors.cbuRequired')); return; }
    if (!userUri) return;
    setCbuBusy(true);
    try {
      const res = await patchCbu(userUri, cbu.trim());
      onUserUpdated(res.data);
    } catch (err) {
      setCbuError(errorMessage(err, 'owner.prereq.errors.cbuSaveFailed'));
    } finally {
      setCbuBusy(false);
    }
  }

  async function onUploadIdentity(e: ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    e.target.value = '';
    setIdentityError(null);
    if (!file) { setIdentityError(t('owner.prereq.errors.identityRequired')); return; }
    if (!user) return;
    setIdentityBusy(true);
    try {
      await uploadIdentityDocument(user, file);
      onIdentityUploaded();
    } catch (err) {
      setIdentityError(errorMessage(err, 'owner.prereq.errors.identitySaveFailed'));
    } finally {
      setIdentityBusy(false);
    }
  }

  return (
    <main className="container py-5">
      <div className="row justify-content-center">
        <div className="col-md-8 col-lg-6">
          <article className="card border-0 shadow-sm rounded-4 bg-white">
            <div className="card-body p-4 p-md-5">
              <h1 className="h4 fw-bold mb-3">{t('owner.prereq.title')}</h1>
              <p className="text-secondary mb-4">{t('owner.prereq.intro')}</p>

              <ul className="list-unstyled mb-4">
                <li className="d-flex align-items-center gap-2 mb-2">
                  <i
                    className={`bi ${cbuOk ? 'bi-check-circle-fill text-success' : 'bi-exclamation-circle-fill text-warning'} fs-5`}
                    aria-hidden="true"
                  />
                  <span>{cbuOk ? t('owner.prereq.cbuDone') : t('owner.prereq.requirementCbu')}</span>
                </li>
                <li className="d-flex align-items-center gap-2">
                  <i
                    className={`bi ${identityOk ? 'bi-check-circle-fill text-success' : 'bi-exclamation-circle-fill text-warning'} fs-5`}
                    aria-hidden="true"
                  />
                  <span>{identityOk ? t('owner.prereq.identityDone') : t('owner.prereq.requirementIdentity')}</span>
                </li>
              </ul>

              {!cbuOk && (
                <form onSubmit={onSaveCbu} className="border rounded-3 p-3 mb-3" noValidate>
                  <label className="form-label fw-medium" htmlFor="prereqCbu">
                    <i className="bi bi-bank me-1" aria-hidden="true" />
                    {t('owner.prereq.cbuLabel')}
                  </label>
                  {cbuError && <div className="alert alert-danger py-2 small" role="alert">{cbuError}</div>}
                  <div className="d-flex gap-2">
                    <input
                      id="prereqCbu"
                      className="form-control"
                      inputMode="numeric"
                      value={cbu}
                      placeholder={t('owner.prereq.cbuPlaceholder')}
                      onChange={(e) => setCbu(e.target.value)}
                    />
                    <Button type="submit" variant="primary" disabled={cbuBusy}>
                      {t('owner.prereq.cbuSave')}
                    </Button>
                  </div>
                </form>
              )}

              {!identityOk && (
                <div className="border rounded-3 p-3 mb-3">
                  <label className="form-label fw-medium" htmlFor="prereqIdentity">
                    <i className="bi bi-person-vcard me-1" aria-hidden="true" />
                    {t('owner.prereq.identityLabel')}
                  </label>
                  {identityError && <div className="alert alert-danger py-2 small" role="alert">{identityError}</div>}
                  <input
                    id="prereqIdentity"
                    type="file"
                    className="form-control"
                    accept="application/pdf,image/*"
                    disabled={identityBusy}
                    onChange={onUploadIdentity}
                  />
                  <small className="text-muted d-block mt-2">{t('owner.prereq.identityHint')}</small>
                </div>
              )}

              <p className="text-muted small mt-3 mb-3">{t('owner.prereq.footnote')}</p>

              <div className="d-flex flex-wrap gap-2">
                <Link className="btn btn-outline-secondary" to="/mis-autos">{t('owner.myCars.title')}</Link>
              </div>
            </div>
          </article>
        </div>
      </div>
    </main>
  );
}

// =============================================================================
// (3) Resultado post-publicación: pending (modelo nuevo no validado) o
// confirmación. Replica publishCarPending.jsp + publishCarConfirmation.jsp.
// =============================================================================
function PublishResult({
  car,
  user,
}: {
  car: CarDto;
  user: UserDto | null;
}) {
  const { t } = useTranslation();
  const carId = idFromUri(car.links.self);

  // Pending: la marca/modelo quedó pendiente de aprobación admin.
  if (!car.modelValidated) {
    return (
      <main className="container py-5">
        <div className="row justify-content-center">
          <div className="col-md-9 col-lg-7">
            <div className="card border-0 shadow-sm rounded-4 bg-white">
              <div className="card-body p-4 p-md-5 text-center">
                <div className="mb-3">
                  <i className="bi bi-hourglass-split text-warning" style={{ fontSize: '2.5rem' }} aria-hidden="true" />
                </div>
                <h1 className="h3 fw-bold mb-2">{t('owner.pending.title')}</h1>
                <p className="text-secondary mb-4">{t('owner.pending.subtitle')}</p>

                <div className="card mb-4 border-0 text-start bg-body-tertiary">
                  <div className="card-body">
                    <p className="mb-2">
                      <strong>{t('owner.pending.brandLabel')}:</strong> {car.brandName}
                      <span className="badge bg-warning text-dark ms-2">{t('owner.pending.pendingNote')}</span>
                    </p>
                    <p className="mb-0">
                      <strong>{t('owner.pending.modelLabel')}:</strong> {car.modelName}
                      <span className="badge bg-warning text-dark ms-2">{t('owner.pending.pendingNote')}</span>
                    </p>
                  </div>
                </div>

                <div className="d-grid gap-2 d-sm-flex justify-content-sm-center">
                  {carId && (
                    <Link to={`/mis-autos/${carId}`} className="btn btn-primary">
                      <i className="bi bi-car-front me-1" aria-hidden="true" />
                      {t('owner.pending.viewInMyCars')}
                    </Link>
                  )}
                  <Link to="/" className="btn btn-outline-secondary">{t('owner.pending.backToHome')}</Link>
                </div>
              </div>
            </div>
          </div>
        </div>
      </main>
    );
  }

  // Confirmación.
  // El backend pone LACK_DOC al publicar sin seguro y ACTIVE con seguro
  // (CarServiceImpl.publish): el status explícito es la única señal necesaria.
  const lackDoc = car.status === 'lack_doc';
  return (
    <main className="container py-5">
      <div className="row justify-content-center">
        <div className="col-md-9 col-lg-7">
          <div className="card border-0 shadow-sm rounded-4 bg-white">
            <div className="card-body p-4 p-md-5 text-center">
              <div className="mb-3">
                <i className="bi bi-check-circle-fill text-success" style={{ fontSize: '2.5rem' }} aria-hidden="true" />
              </div>
              <h1 className="h3 fw-bold mb-2">{t('owner.confirmation.title')}</h1>
              {user?.forename && (
                <p className="mb-1">{t('owner.confirmation.greeting', { name: user.forename })}</p>
              )}
              <p className="mb-1">
                {t('owner.confirmation.message', { brand: car.brandName, model: car.modelName })}
              </p>
              <p className="text-secondary mb-4">{t('owner.confirmation.subtitle')}</p>

              <div className="card mb-4 border-0 text-start bg-body-tertiary">
                <div className="card-body">
                  <p className="mb-2"><strong>{t('owner.confirmation.brand')}</strong> {car.brandName}</p>
                  <p className="mb-2"><strong>{t('owner.confirmation.model')}</strong> {car.modelName}</p>
                  {car.year ? (
                    <p className="mb-2"><strong>{t('owner.confirmation.year')}</strong> {car.year}</p>
                  ) : null}
                  <p className="mb-2"><strong>{t('owner.confirmation.plate')}</strong> {car.plate}</p>
                  <p className="mb-2"><strong>{t('owner.confirmation.type')}</strong> {t(`owner.enums.type.${car.type}`)}</p>
                  <p className="mb-2"><strong>{t('owner.confirmation.powertrain')}</strong> {t(`owner.enums.powertrain.${car.powertrain}`)}</p>
                  <p className="mb-0"><strong>{t('owner.confirmation.transmission')}</strong> {t(`owner.enums.transmission.${car.transmission}`)}</p>
                </div>
              </div>

              {lackDoc && (
                <div className="alert alert-warning d-flex align-items-start gap-2 text-start mb-4" role="alert">
                  <i className="bi bi-shield-exclamation flex-shrink-0 mt-1" aria-hidden="true" />
                  <span>{t('owner.confirmation.lackDoc')}</span>
                </div>
              )}

              <div className="d-grid gap-2 d-sm-flex justify-content-sm-center">
                {carId && (
                  <Link to={`/mis-autos/${carId}`} className="btn btn-primary">
                    <i className="bi bi-plus-lg me-1" aria-hidden="true" />
                    {t('owner.confirmation.addAvailabilityCta')}
                  </Link>
                )}
                <Link to="/mis-autos" className="btn btn-outline-primary">
                  <i className="bi bi-car-front me-1" aria-hidden="true" />
                  {t('owner.confirmation.myCarsCta')}
                </Link>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>
  );
}

// =============================================================================
// (2) Formulario de publicación.
// =============================================================================
function PublishCarForm({
  onPublished,
}: {
  onPublished: (car: CarDto) => void;
}) {
  const { t } = useTranslation();
  const errorMessage = useApiErrorMessage();

  // Catálogo.
  const [brands, setBrands] = useState<BrandDto[]>([]);
  const [models, setModels] = useState<ModelDto[]>([]);
  const [brandSel, setBrandSel] = useState<string>(''); // self URN o OTHER
  const [modelSel, setModelSel] = useState<string>(''); // self URN o OTHER
  const [newBrandName, setNewBrandName] = useState('');
  const [newModelName, setNewModelName] = useState('');

  // Atributos del auto.
  const [plate, setPlate] = useState('');
  const [year, setYear] = useState('');
  const [type, setType] = useState<CarType>('sedan');
  const [transmission, setTransmission] = useState<Transmission>('manual');
  const [powertrain, setPowertrain] = useState<Powertrain>('gasoline');
  const [description, setDescription] = useState('');
  const [minimumRentalDays, setMinimumRentalDays] = useState('1');
  const [pictures, setPictures] = useState<File[]>([]);
  const [insurance, setInsurance] = useState<File | null>(null);

  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    let active = true;
    fetchBrands()
      .then((res) => { if (active) setBrands(res.data ?? []); })
      .catch(() => { if (active) setError(errorMessage(null, 'owner.errors.catalogLoad')); });
    return () => { active = false; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Al elegir marca existente, cargamos sus modelos (vía su link `models`).
  useEffect(() => {
    setModelSel('');
    setModels([]);
    if (!brandSel || brandSel === OTHER) return;
    const brand = brands.find((b) => b.links.self === brandSel);
    if (!brand) return;
    let active = true;
    fetchModels(brand)
      .then((res) => { if (active) setModels(res.data ?? []); })
      .catch(() => { /* deja la lista vacía: el owner puede usar "Otro" */ });
    return () => { active = false; };
  }, [brandSel, brands]);

  /** Resuelve marca/modelo a las partes del CarCreateDto, creando catálogo si "Otro". */
  async function resolveCatalog(): Promise<Partial<CarCreateDto>> {
    // Marca.
    let brand: BrandDto | undefined;
    if (brandSel === OTHER) {
      const name = newBrandName.trim();
      if (!name) throw new Error('owner.publish.errors.brandRequired');
      brand = (await createBrand(name)).data;
    } else {
      brand = brands.find((b) => b.links.self === brandSel);
    }
    if (!brand) throw new Error('owner.publish.errors.brandRequired');

    // Modelo.
    if (modelSel === OTHER) {
      const name = newModelName.trim();
      if (!name) throw new Error('owner.publish.errors.modelRequired');
      const model = (await createModel(brand, name, type)).data;
      return { modelUri: model.links.self };
    }
    if (modelSel) {
      const existing = models.find((m) => m.links.self === modelSel);
      if (existing) return { modelUri: existing.links.self };
    }
    // Sin modelo elegido: mandamos por nombre (el server resuelve/crea).
    return { brandName: brand.name };
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);

    if (!plate.trim()) { setError(t('owner.publish.errors.plateRequired')); return; }

    setSubmitting(true);
    try {
      const catalog = await resolveCatalog();
      const body: CarCreateDto = {
        plate: plate.trim(),
        year: year ? Number(year) : undefined,
        type,
        transmission,
        powertrain,
        description: description.trim() || undefined,
        minimumRentalDays: minimumRentalDays ? Number(minimumRentalDays) : undefined,
        ...catalog,
      };
      const res = await publishCar(body, pictures, insurance);
      // El POST devuelve el CarDto (con status + modelValidated): de ahí derivamos
      // si mostramos "pendiente" (modelo nuevo) o "confirmación". Si por algún
      // motivo no viene cuerpo, lo releemos desde Location.
      let car = res.data;
      if (!car) {
        const loc = res.location;
        const newId = idFromUri(loc);
        if (newId) car = (await fetchCar(newId)).data;
      }
      if (car) {
        onPublished(car);
      }
    } catch (err) {
      if (err instanceof Error && err.message.startsWith('owner.')) {
        setError(t(err.message));
      } else {
        setError(errorMessage(err, 'owner.publish.errors.failed'));
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="container py-5">
      <div className="row justify-content-center">
        <div className="col-md-8 col-lg-6">
          <div className="card border-0 shadow-sm rounded-4 bg-white">
            <div className="card-body p-4 p-md-5">
              <h4 className="fw-semibold mb-2">{t('owner.publish.title')}</h4>
              <p className="text-secondary small mb-4">{t('owner.publish.prerequisitesNote')}</p>

              <form onSubmit={onSubmit} noValidate>
                {error && <Alert variant="danger" role="alert">{error}</Alert>}

                {/* ── Marca y modelo ──────────────────────────────── */}
                <h6 className="fw-semibold text-secondary text-uppercase mb-3" style={{ letterSpacing: '.04em' }}>
                  {t('owner.publish.catalogSection')}
                </h6>

                <div className="mb-3">
                  <label className="form-label required-label" htmlFor="publishBrand">{t('owner.publish.brand')}</label>
                  <select
                    id="publishBrand"
                    className="form-select"
                    value={brandSel}
                    onChange={(e) => setBrandSel(e.target.value)}
                    required
                  >
                    <option value="">{t('owner.publish.selectPlaceholder')}</option>
                    {brands.map((b) => (
                      <option key={b.links.self} value={b.links.self}>
                        {b.name}{!b.validated ? ` (${t('owner.publish.pending')})` : ''}
                      </option>
                    ))}
                    <option value={OTHER}>{t('owner.publish.other')}</option>
                  </select>
                </div>

                {brandSel === OTHER && (
                  <div className="mb-3">
                    <label className="form-label required-label" htmlFor="publishNewBrand">{t('owner.publish.newBrandName')}</label>
                    <input
                      id="publishNewBrand"
                      className="form-control"
                      value={newBrandName}
                      onChange={(e) => setNewBrandName(e.target.value)}
                      required
                    />
                  </div>
                )}

                <div className="mb-3">
                  <label className="form-label" htmlFor="publishModel">{t('owner.publish.model')}</label>
                  <select
                    id="publishModel"
                    className="form-select"
                    value={modelSel}
                    onChange={(e) => setModelSel(e.target.value)}
                    disabled={!brandSel}
                  >
                    <option value="">{t('owner.publish.selectPlaceholder')}</option>
                    {models.map((m) => (
                      <option key={m.links.self} value={m.links.self}>
                        {m.name}{!m.validated ? ` (${t('owner.publish.pending')})` : ''}
                      </option>
                    ))}
                    <option value={OTHER}>{t('owner.publish.other')}</option>
                  </select>
                </div>

                {modelSel === OTHER && (
                  <div className="mb-3">
                    <label className="form-label required-label" htmlFor="publishNewModel">{t('owner.publish.newModelName')}</label>
                    <input
                      id="publishNewModel"
                      className="form-control"
                      value={newModelName}
                      onChange={(e) => setNewModelName(e.target.value)}
                      required
                    />
                  </div>
                )}

                {/* ── Datos del auto ──────────────────────────────── */}
                <h6 className="fw-semibold text-secondary text-uppercase mb-3 mt-4" style={{ letterSpacing: '.04em' }}>
                  {t('owner.publish.carSection')}
                </h6>

                <div className="mb-3">
                  <label className="form-label required-label" htmlFor="publishPlate">{t('owner.publish.plate')}</label>
                  <input
                    id="publishPlate"
                    className="form-control"
                    style={{ textTransform: 'uppercase' }}
                    value={plate}
                    onChange={(e) => setPlate(e.target.value)}
                    required
                  />
                </div>

                <div className="mb-3">
                  <label className="form-label" htmlFor="publishYear">{t('owner.publish.year')}</label>
                  <input
                    id="publishYear"
                    type="number"
                    className="form-control"
                    value={year}
                    min={1900}
                    max={2100}
                    onChange={(e) => setYear(e.target.value)}
                  />
                </div>

                <div className="row g-3 mb-3">
                  <div className="col-md-4">
                    <label className="form-label" htmlFor="publishType">{t('owner.publish.type')}</label>
                    <select id="publishType" className="form-select" value={type} onChange={(e) => setType(e.target.value as CarType)}>
                      {CAR_TYPES.map((v) => (
                        <option key={v} value={v}>{t(`owner.enums.type.${v}`)}</option>
                      ))}
                    </select>
                  </div>
                  <div className="col-md-4">
                    <label className="form-label" htmlFor="publishTransmission">{t('owner.publish.transmission')}</label>
                    <select id="publishTransmission" className="form-select" value={transmission} onChange={(e) => setTransmission(e.target.value as Transmission)}>
                      {TRANSMISSIONS.map((v) => (
                        <option key={v} value={v}>{t(`owner.enums.transmission.${v}`)}</option>
                      ))}
                    </select>
                  </div>
                  <div className="col-md-4">
                    <label className="form-label" htmlFor="publishPowertrain">{t('owner.publish.powertrain')}</label>
                    <select id="publishPowertrain" className="form-select" value={powertrain} onChange={(e) => setPowertrain(e.target.value as Powertrain)}>
                      {POWERTRAINS.map((v) => (
                        <option key={v} value={v}>{t(`owner.enums.powertrain.${v}`)}</option>
                      ))}
                    </select>
                  </div>
                </div>

                <div className="mb-3">
                  <label className="form-label" htmlFor="publishMinDays">{t('owner.publish.minimumRentalDays')}</label>
                  <input
                    id="publishMinDays"
                    type="number"
                    min={1}
                    className="form-control"
                    style={{ maxWidth: '10rem' }}
                    value={minimumRentalDays}
                    onChange={(e) => setMinimumRentalDays(e.target.value)}
                  />
                </div>

                <div className="mb-3">
                  <label className="form-label" htmlFor="publishDescription">{t('owner.publish.description')}</label>
                  <textarea
                    id="publishDescription"
                    className="form-control"
                    rows={3}
                    value={description}
                    onChange={(e) => setDescription(e.target.value)}
                  />
                </div>

                {/* ── Fotos y seguro ──────────────────────────────── */}
                <h6 className="fw-semibold text-secondary text-uppercase mb-3 mt-4" style={{ letterSpacing: '.04em' }}>
                  {t('owner.publish.filesSection')}
                </h6>

                <GalleryPicker onChange={setPictures} />

                <div className="mb-4">
                  <label className="form-label d-block" htmlFor="publishInsurance">{t('owner.publish.insurance')}</label>
                  <input
                    id="publishInsurance"
                    type="file"
                    className="form-control"
                    onChange={(e) => setInsurance(e.target.files?.[0] ?? null)}
                  />
                </div>

                <div className="d-flex justify-content-end mt-2">
                  <Button type="submit" variant="primary" disabled={submitting}>
                    {submitting ? (
                      <>
                        <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true" />
                        {t('owner.publish.submitting')}
                      </>
                    ) : (
                      <>
                        <i className="bi bi-check-lg me-1" aria-hidden="true" />
                        {t('owner.publish.submit')}
                      </>
                    )}
                  </Button>
                </div>
              </form>
            </div>
          </div>
        </div>
      </div>
    </main>
  );
}
