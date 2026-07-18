import { useEffect, useRef, useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { Button, Form, Modal } from 'react-bootstrap';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Avatar, BreadcrumbTrail, LoadingBlock, FieldView, ReceiptUploadPicker, FlatpickrDateInput } from '../../components/ryden';
import { profilePictureAssetUrl } from '../../api/uri';
import { getClientConfig } from '../../api/clientConfig';
import type { UserDto as SessionUserDto } from '../../api/types';
import { useSessionStore, sessionClient } from '../../session/sessionStore';
import { useMyUserUri } from './hooks';
import {
  deleteDocument,
  deleteProfilePicture,
  fetchUser,
  openDocument,
  patchUser,
  uploadDocument,
  uploadProfilePicture,
} from './api';
import type { DocumentType, ProfileFormValues, UserDto, UserPatchDto } from './types';
import { apiErrorMessage } from '../auth/errorMessage';
import { ApiError } from '../../api/client';
import {
  registrationPasswordLimits,
  validatePassword,
  validatePasswordConfirm,
} from '../auth/passwordValidation';
import { formatDate, formatMonthYear, wallTodayYmd } from '../../i18n/dateFormat';

// =============================================================================
// MyProfilePage — perfil propio editable. Markup espejo del profile.jsp original
// (Bootstrap + clases del tema: profile-card, profile-fields-grid, etc.).
//   GET /users/{myId} → datos. PATCH para editar / cambiar password.
//   PUT/DELETE profile-picture y documents (multipart) vía sub-recursos.
// =============================================================================

const DOC_TYPES: DocumentType[] = ['license', 'identity'];

function toForm(u: UserDto): ProfileFormValues {
  return {
    forename: u.forename ?? '',
    surname: u.surname ?? '',
    phoneNumber: u.phoneNumber ?? '',
    birthDate: u.birthDate ?? '',
    about: u.about ?? '',
    cbu: u.cbu ?? '',
  };
}

/** Construye el patch con SOLO los campos que cambiaron respecto al original. */
function diffPatch(original: UserDto, form: ProfileFormValues): UserPatchDto {
  const patch: UserPatchDto = {};
  if (form.forename !== (original.forename ?? '')) patch.forename = form.forename;
  if (form.surname !== (original.surname ?? '')) patch.surname = form.surname;
  if (form.phoneNumber !== (original.phoneNumber ?? '')) patch.phoneNumber = form.phoneNumber;
  if (form.birthDate !== (original.birthDate ?? '')) patch.birthDate = form.birthDate;
  if (form.about !== (original.about ?? '')) patch.about = form.about;
  if (form.cbu !== (original.cbu ?? '')) patch.cbu = form.cbu;
  return patch;
}

export default function MyProfilePage() {
  const { t } = useTranslation();
  const myUri = useMyUserUri();
  const sessionStatus = useSessionStore((s) => s.status);
  const sessionUser = useSessionStore((s) => s.currentUser);
  const queryClient = useQueryClient();

  const userQuery = useQuery({
    queryKey: ['profile', 'me', myUri],
    // Perfil propio: vista privada (email, cbu, etc.).
    queryFn: () => fetchUser(myUri as string, { private: true }),
    enabled: !!myUri,
    initialData: sessionUser ?? undefined,
  });

  if (sessionStatus === 'authenticated' && !myUri) {
    return (
      <div className="container profile-container">
        <LoadingBlock variant="page" className="py-4" />
      </div>
    );
  }
  if (!myUri) {
    return (
      <div className="container profile-container">
        <div className="profile-header">
          <h1 className="profile-header__title">{t('profile.me.title')}</h1>
        </div>
        <div className="alert alert-warning" role="alert">
          {t('profile.common.loginRequired')}
        </div>
      </div>
    );
  }
  if (userQuery.isLoading) {
    return (
      <div className="container profile-container">
        <LoadingBlock variant="page" className="py-4" />
      </div>
    );
  }
  if (userQuery.isError || !userQuery.data) {
    return (
      <div className="container profile-container">
        <div className="alert alert-danger" role="alert">
          {t('profile.common.error')}
        </div>
      </div>
    );
  }

  const user = userQuery.data;
  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['profile', 'me', myUri] });

  return (
    <div className="container profile-container">
      <BreadcrumbTrail currentLabel={t('profile.me.title')} />
      <div className="profile-header">
        <h1 className="profile-header__title">{t('profile.me.title')}</h1>
      </div>

      <ProfilePictureCard user={user} onChanged={invalidate} />
      <ProfileDataCard user={user} userUri={myUri} onSaved={invalidate} />
      <DocumentsCard user={user} onChanged={invalidate} />
      <PasswordCard userUri={myUri} />
    </div>
  );
}

// --- Card de avatar + nombre + email ----------------------------------------
function ProfilePictureCard({ user, onChanged }: { user: UserDto; onChanged: () => void }) {
  const { t, i18n } = useTranslation();
  // links.profilePicture se anuncia siempre; hasProfilePicture indica si hay bytes.
  const hasPicture = user.hasProfilePicture === true;
  const pictureLink = hasPicture ? (profilePictureAssetUrl(user.links) ?? undefined) : undefined;
  const fileRef = useRef<HTMLInputElement>(null);
  const [menuOpen, setMenuOpen] = useState(false);
  const [banner, setBanner] = useState<'saved' | 'removed' | null>(null);

  // Cierra el menú al clickear fuera (espejo del comportamiento del JSP).
  useEffect(() => {
    if (!menuOpen) return;
    const close = () => setMenuOpen(false);
    document.addEventListener('click', close);
    return () => document.removeEventListener('click', close);
  }, [menuOpen]);

  const upload = useMutation({
    mutationFn: (file: File) => uploadProfilePicture(user, file),
    onSuccess: () => {
      setBanner('saved');
      onChanged();
    },
  });
  const remove = useMutation({
    mutationFn: () => deleteProfilePicture(user),
    onSuccess: () => {
      setBanner('removed');
      onChanged();
    },
  });

  return (
    <div className="profile-card">
      <div className="profile-card__header">
        <div className="profile-card__avatar">
          <Avatar
            src={pictureLink}
            alt={`${user.forename} ${user.surname}`}
            forename={user.forename}
            surname={user.surname}
            className="profile-card__avatar-placeholder"
            imgClassName="profile-card__avatar-img"
            placeholderClassName="profile-card__avatar-placeholder"
            colored
            barePhoto
          />

          <button
            type="button"
            className="profile-card__avatar-edit-btn"
            aria-label={t('profile.photo.edit')}
            onClick={(e) => {
              e.stopPropagation();
              setMenuOpen((o) => !o);
            }}
          >
            <i className="bi bi-pencil-fill" aria-hidden="true"></i>
          </button>
          <div
            className={`profile-avatar-menu${menuOpen ? ' is-open' : ''}`}
            aria-hidden={!menuOpen}
            onClick={(e) => e.stopPropagation()}
          >
            <button
              type="button"
              className="profile-avatar-menu__item"
              onClick={() => {
                setMenuOpen(false);
                fileRef.current?.click();
              }}
            >
              <i className="bi bi-upload" aria-hidden="true"></i>{' '}
              {pictureLink ? t('profile.photo.replace') : t('profile.photo.upload')}
            </button>
            {pictureLink && (
              <button
                type="button"
                className="profile-avatar-menu__item profile-avatar-menu__item--danger"
                onClick={() => {
                  setMenuOpen(false);
                  remove.mutate();
                }}
                disabled={remove.isPending}
              >
                <i className="bi bi-trash" aria-hidden="true"></i> {t('profile.photo.remove')}
              </button>
            )}
          </div>

          <input
            ref={fileRef}
            type="file"
            accept="image/*"
            className="d-none"
            onChange={(e) => {
              const file = e.target.files?.[0];
              if (file) upload.mutate(file);
              e.target.value = '';
            }}
          />
        </div>

        <div className="profile-card__info min-w-0">
          <h2 className="profile-card__name ryden-text-break">
            {user.forename} {user.surname}
          </h2>
          {user.memberSince && (
            <p className="profile-card__member-since">
              <span className="profile-card__member-since-label">{t('profile.me.memberSince')}</span>{' '}
              <span className="profile-card__member-since-value">
                {formatMonthYear(user.memberSince, i18n.language)}
              </span>
            </p>
          )}
          {user.email && <p className="profile-card__email ryden-text-break">{user.email}</p>}
        </div>
      </div>

      {banner && !upload.isError && !remove.isError && (
        <div className="alert alert-success mt-3 mb-0" role="alert">
          {t(banner === 'saved' ? 'profile.photo.saved' : 'profile.photo.removed')}
        </div>
      )}
      {(upload.isError || remove.isError) && (
        <div className="alert alert-danger mt-3 mb-0" role="alert">
          {t('profile.common.error')}
        </div>
      )}
    </div>
  );
}

// --- Datos personales (vista / edición) -------------------------------------
function ProfileDataCard({
  user,
  userUri,
  onSaved,
}: {
  user: UserDto;
  userUri: string;
  onSaved: () => void;
}) {
  const { t, i18n } = useTranslation();
  const [editing, setEditing] = useState(false);
  const [form, setForm] = useState<ProfileFormValues>(() => toForm(user));
  const [done, setDone] = useState(false);
  const [fieldErrors, setFieldErrors] = useState<Partial<Record<'phoneNumber' | 'birthDate' | 'cbu', string>>>({});
  const userLimits = getClientConfig().user;

  // Resincroniza si el usuario base cambia (tras invalidate).
  useEffect(() => setForm(toForm(user)), [user]);

  const mutation = useMutation({
    mutationFn: (patch: UserPatchDto) => patchUser(userUri, patch),
    onSuccess: (updated) => {
      useSessionStore.setState({ currentUser: updated as unknown as SessionUserDto });
      setDone(true);
      setEditing(false);
      onSaved();
    },
    onError: (err) => {
      if (err instanceof ApiError && err.body?.errors?.length) {
        const nextErrors: typeof fieldErrors = {};
        for (const fe of err.body.errors) {
          if (fe.field === 'phoneNumber' || fe.field === 'birthDate' || fe.field === 'cbu') {
            nextErrors[fe.field] = fe.message;
          }
        }
        if (Object.keys(nextErrors).length > 0) {
          setFieldErrors((prev) => ({ ...prev, ...nextErrors }));
        }
      }
    },
  });

  /** Mirror of {@code app.validation.profile-phone-pattern}: digits and + only. */
  function sanitizePhone(raw: string): string {
    return raw.replace(/[^0-9+]/g, '').slice(0, userLimits.profilePhoneMaxLength);
  }

  function phoneError(raw: string): string | null {
    const phone = raw.trim();
    if (phone && !/^[0-9+]+$/.test(phone)) {
      return t('profile.me.phoneInvalid', { max: userLimits.profilePhoneMaxLength });
    }
    if (phone.length > userLimits.profilePhoneMaxLength) {
      return t('profile.me.phoneInvalid', { max: userLimits.profilePhoneMaxLength });
    }
    return null;
  }

  function birthDateError(raw: string): string | null {
    if (raw && raw > wallTodayYmd()) return t('profile.me.birthDateFuture');
    return null;
  }

  function cbuError(raw: string): string | null {
    const cbuDigits = getClientConfig().cbuRequiredDigits;
    const cbu = raw.trim();
    if (cbu && !new RegExp(`^\\d{${cbuDigits}}$`).test(cbu)) {
      return t('profile.me.cbuInvalid', { digits: cbuDigits });
    }
    return null;
  }

  function update<K extends keyof ProfileFormValues>(key: K, value: string) {
    setForm((f) => ({ ...f, [key]: value }));
    setDone(false);
    if (key === 'phoneNumber') {
      const msg = phoneError(value);
      setFieldErrors((prev) => {
        const next = { ...prev };
        if (msg) next.phoneNumber = msg;
        else delete next.phoneNumber;
        return next;
      });
    } else if (key === 'birthDate') {
      const msg = birthDateError(value);
      setFieldErrors((prev) => {
        const next = { ...prev };
        if (msg) next.birthDate = msg;
        else delete next.birthDate;
        return next;
      });
    } else if (key === 'cbu') {
      const msg = cbuError(value);
      setFieldErrors((prev) => {
        const next = { ...prev };
        if (msg) next.cbu = msg;
        else delete next.cbu;
        return next;
      });
    }
  }

  function onSubmit(e: FormEvent) {
    e.preventDefault();
    const nextErrors: typeof fieldErrors = {};
    const phoneMsg = phoneError(form.phoneNumber);
    const birthMsg = birthDateError(form.birthDate);
    const cbuMsg = cbuError(form.cbu);
    if (phoneMsg) nextErrors.phoneNumber = phoneMsg;
    if (birthMsg) nextErrors.birthDate = birthMsg;
    if (cbuMsg) nextErrors.cbu = cbuMsg;
    setFieldErrors(nextErrors);
    if (phoneMsg) {
      document.getElementById('phoneNumber')?.focus();
      return;
    }
    if (birthMsg) {
      document.getElementById('birthDate')?.focus();
      return;
    }
    if (cbuMsg) {
      document.getElementById('cbu')?.focus();
      return;
    }
    const patch = diffPatch(user, form);
    if (Object.keys(patch).length === 0) {
      setEditing(false);
      return;
    }
    mutation.mutate(patch);
  }

  const orDash = (v?: string | null) => (v && v.trim() ? v : t('profile.common.notSpecified'));

  if (!editing) {
    return (
      <div className="profile-card profile-card--section">
        <div className="profile-card__section-header">
          <h2 className="profile-section-title">{t('profile.me.optionalSection')}</h2>
          <Button variant="outline-primary" size="sm" onClick={() => setEditing(true)}>
            {t('profile.common.edit')}
          </Button>
        </div>
        <hr className="profile-card__divider" />
        {done && (
          <div className="alert alert-success" role="alert">
            {t('profile.common.saved')}
          </div>
        )}
        <div className="profile-fields-grid">
          <FieldView label={t('profile.me.forename')} value={orDash(form.forename)} />
          <FieldView label={t('profile.me.surname')} value={orDash(form.surname)} />
          <FieldView label={t('profile.me.phoneNumber')} value={orDash(form.phoneNumber)} />
          <FieldView
            label={t('profile.me.birthDate')}
            value={form.birthDate ? formatDate(form.birthDate, i18n.language) : orDash('')}
          />
          <FieldView label={t('profile.me.cbu')} value={orDash(form.cbu)} />
          <FieldView label={t('profile.me.about')} value={orDash(form.about)} multiline />
        </div>
      </div>
    );
  }

  return (
    <div className="profile-card profile-card--section">
      <h2 className="profile-section-title">{t('profile.me.optionalSection')}</h2>
      <hr className="profile-card__divider" />
      <Form onSubmit={onSubmit} noValidate>
        {mutation.isError && (
          <div className="alert alert-danger" role="alert">
            {apiErrorMessage(t, mutation.error)}
          </div>
        )}
        <div className="profile-fields-grid">
          <Form.Group className="mb-3" controlId="forename">
            <Form.Label>{t('profile.me.forename')}</Form.Label>
            <Form.Control
              value={form.forename}
              autoComplete="given-name"
              maxLength={userLimits.displayNamePartMaxLength}
              onChange={(e) => update('forename', e.target.value)}
              required
            />
          </Form.Group>
          <Form.Group className="mb-3" controlId="surname">
            <Form.Label>{t('profile.me.surname')}</Form.Label>
            <Form.Control
              value={form.surname}
              autoComplete="family-name"
              maxLength={userLimits.displayNamePartMaxLength}
              onChange={(e) => update('surname', e.target.value)}
              required
            />
          </Form.Group>
          <Form.Group className="mb-3" controlId="phoneNumber">
            <Form.Label>{t('profile.me.phoneNumber')}</Form.Label>
            <Form.Control
              value={form.phoneNumber}
              autoComplete="tel"
              inputMode="tel"
              maxLength={userLimits.profilePhoneMaxLength}
              isInvalid={!!fieldErrors.phoneNumber}
              onChange={(e) => update('phoneNumber', sanitizePhone(e.target.value))}
            />
            {fieldErrors.phoneNumber ? (
              <div className="text-danger small d-block mt-1" role="alert">{fieldErrors.phoneNumber}</div>
            ) : null}
          </Form.Group>
          <Form.Group className="mb-3" controlId="birthDate">
            <Form.Label>{t('profile.me.birthDate')}</Form.Label>
            <FlatpickrDateInput
              id="birthDate"
              value={form.birthDate}
              maxDate="today"
              autoComplete="bday"
              allowClear
              clearLabel={t('profile.me.clearBirthDate')}
              isInvalid={!!fieldErrors.birthDate}
              aria-invalid={fieldErrors.birthDate ? true : undefined}
              aria-describedby={fieldErrors.birthDate ? 'birthDateError' : undefined}
              onChange={(ymd) => update('birthDate', ymd)}
            />
            {fieldErrors.birthDate ? (
              <div id="birthDateError" className="text-danger small d-block mt-1" role="alert">
                {fieldErrors.birthDate}
              </div>
            ) : null}
          </Form.Group>
          <Form.Group className="mb-3" controlId="cbu">
            <Form.Label>{t('profile.me.cbu')}</Form.Label>
            <Form.Control
              value={form.cbu}
              inputMode="numeric"
              maxLength={getClientConfig().cbuRequiredDigits}
              isInvalid={!!fieldErrors.cbu}
              onChange={(e) => update('cbu', e.target.value)}
            />
            {fieldErrors.cbu ? (
              <div className="text-danger small d-block mt-1" role="alert">{fieldErrors.cbu}</div>
            ) : null}
          </Form.Group>
          <Form.Group className="mb-3" controlId="about">
            <Form.Label>{t('profile.me.about')}</Form.Label>
            <Form.Control
              as="textarea"
              rows={4}
              value={form.about}
              maxLength={userLimits.profileAboutMaxLength}
              onChange={(e) => update('about', e.target.value)}
            />
          </Form.Group>
        </div>
        <div className="profile-card__form-actions">
          <Button
            type="button"
            variant="outline-secondary"
            onClick={() => {
              setForm(toForm(user));
              setFieldErrors({});
              setEditing(false);
            }}
          >
            {t('profile.common.cancel')}
          </Button>
          <Button type="submit" variant="primary" disabled={mutation.isPending}>
            {mutation.isPending ? t('profile.common.saving') : t('profile.common.save')}
          </Button>
        </div>
      </Form>
    </div>
  );
}

// --- Documentos -------------------------------------------------------------
function DocumentsCard({ user, onChanged }: { user: UserDto; onChanged: () => void }) {
  const { t } = useTranslation();
  return (
    <div className="profile-card profile-card--section">
      <h2 className="profile-section-title">{t('profile.docs.title')}</h2>
      <hr className="profile-card__divider" />
      <p className="text-muted small mb-3">{t('profile.docs.sectionHint')}</p>
      <div className="profile-fields-grid">
        {DOC_TYPES.map((type) => (
          <DocumentRow key={type} user={user} type={type} onChanged={onChanged} />
        ))}
      </div>
    </div>
  );
}

function DocumentRow({
  user,
  type,
  onChanged,
}: {
  user: UserDto;
  type: DocumentType;
  onChanged: () => void;
}) {
  const { t } = useTranslation();
  const label = type === 'license' ? t('profile.docs.license') : t('profile.docs.identity');
  const validated = type === 'license' ? user.licenseValidated : user.identityValidated;
  const uploaded = type === 'license' ? user.licenseUploaded : user.identityUploaded;
  const [viewError, setViewError] = useState(false);
  const [banner, setBanner] = useState<'uploaded' | 'removed' | null>(null);
  const [showRemoveConfirm, setShowRemoveConfirm] = useState(false);
  const [showReplacePicker, setShowReplacePicker] = useState(false);

  const upload = useMutation({
    mutationFn: (file: File) => uploadDocument(user, type, file),
    onSuccess: () => {
      setBanner('uploaded');
      setShowReplacePicker(false);
      onChanged();
    },
  });
  const remove = useMutation({
    mutationFn: () => deleteDocument(user, type),
    onSuccess: () => {
      setBanner('removed');
      onChanged();
    },
  });
  const view = useMutation({
    mutationFn: () => openDocument(user, type),
    onSuccess: (ok) => setViewError(!ok),
  });

  const docPickerLabels = {
    chooseFile: t('profile.docs.chooseFile'),
    confirmUpload: t('profile.docs.confirmUpload'),
    confirming: t('profile.docs.confirming'),
    uploadAria: uploaded ? t('profile.docs.replace') : t('profile.docs.upload'),
    replaceFile: t('profile.docs.replaceFile'),
    removeFile: t('profile.docs.removeFile'),
    invalidFile: t('profile.docs.invalidFile'),
    fileTooLarge: t('profile.docs.fileTooLarge', { maxMb: 5 }),
    uploadError: t('profile.docs.uploadError'),
  };

  const onConfirmUpload = async (file: File) => {
    await upload.mutateAsync(file);
  };

  function confirmRemoveDocument() {
    setShowRemoveConfirm(false);
    remove.mutate();
  }

  return (
    <div className="mb-3">
      {uploaded ? (
        <div className="form-label">{label}</div>
      ) : (
        <label className="form-label" htmlFor={`docFile-${type}`}>{label}</label>
      )}
      <p className="small mb-2">
        {validated ? (
          <i className="bi bi-check-circle-fill text-success" aria-hidden="true"></i>
        ) : uploaded ? (
          <i className="bi bi-clock-fill text-warning" aria-hidden="true"></i>
        ) : (
          <i className="bi bi-x-circle-fill text-secondary" aria-hidden="true"></i>
        )}
        <span className="ms-1">
          {validated
            ? t('profile.docs.validated')
            : uploaded
              ? t('profile.docs.pending')
              : t('profile.docs.none')}
        </span>
      </p>
      {uploaded && (
        <>
          <p className="small mb-2">
            <i className="bi bi-check-circle-fill text-success me-1" aria-hidden="true"></i>
            <span>{t('profile.docs.uploaded')}</span>
          </p>
          <p className="small mb-2">
            <button
              type="button"
              className="btn btn-link link-primary text-break p-0 align-baseline"
              onClick={() => {
                setViewError(false);
                view.mutate();
              }}
              disabled={view.isPending}
            >
              {t('profile.docs.viewFile')}
            </button>
          </p>
          <div className="d-flex flex-wrap gap-2 mb-3">
            <Button
              type="button"
              variant="outline-danger"
              size="sm"
              onClick={() => setShowRemoveConfirm(true)}
              disabled={remove.isPending || upload.isPending}
            >
              {t('profile.docs.remove')}
            </Button>
            {!showReplacePicker && (
              <Button
                type="button"
                variant="outline-primary"
                size="sm"
                onClick={() => setShowReplacePicker(true)}
                disabled={remove.isPending || upload.isPending}
              >
                {t('profile.docs.replace')}
              </Button>
            )}
          </div>
          {showReplacePicker && (
            <div className="mb-2">
              <label className="form-label small mb-1" htmlFor={`docReplace-${type}`}>
                {t('profile.docs.replace')}
              </label>
              <ReceiptUploadPicker
                id={`docReplace-${type}`}
                disabled={upload.isPending || remove.isPending}
                busy={upload.isPending}
                onConfirm={onConfirmUpload}
                labels={docPickerLabels}
              />
              <Button
                type="button"
                variant="outline-secondary"
                size="sm"
                className="mt-2"
                onClick={() => setShowReplacePicker(false)}
                disabled={upload.isPending}
              >
                {t('profile.common.cancel')}
              </Button>
            </div>
          )}
        </>
      )}
      {!uploaded && (
        <ReceiptUploadPicker
          id={`docFile-${type}`}
          disabled={upload.isPending}
          busy={upload.isPending}
          onConfirm={onConfirmUpload}
          labels={docPickerLabels}
        />
      )}
      {banner && !upload.isError && !remove.isError && (
        <div className="alert alert-success mt-2 mb-0 py-2 small" role="alert">
          {t(banner === 'uploaded' ? 'profile.docs.uploaded' : 'profile.docs.removed')}
        </div>
      )}
      {viewError && (
        <div className="alert alert-warning mt-2 mb-0 py-2 small" role="alert">
          {t('profile.docs.viewError')}
        </div>
      )}
      {(upload.isError || remove.isError) && (
        <div className="alert alert-danger mt-2 mb-0 py-2 small" role="alert">
          {t('profile.common.error')}
        </div>
      )}

      <Modal show={showRemoveConfirm} onHide={() => setShowRemoveConfirm(false)} centered>
        <Modal.Header closeButton>
          <Modal.Title className="fs-5 fw-semibold">{t('profile.docs.remove')}</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <p className="mb-0 text-secondary">{t('profile.docs.removeConfirm', { label })}</p>
        </Modal.Body>
        <Modal.Footer className="border-0 pt-0">
          <Button
            variant="outline-secondary"
            onClick={() => setShowRemoveConfirm(false)}
            disabled={remove.isPending}
          >
            {t('profile.common.cancel')}
          </Button>
          <Button
            variant="danger"
            onClick={confirmRemoveDocument}
            disabled={remove.isPending}
          >
            {t('profile.docs.remove')}
          </Button>
        </Modal.Footer>
      </Modal>
    </div>
  );
}

// --- Cambio de contraseña ---------------------------------------------------
function PasswordCard({ userUri }: { userUri: string }) {
  const { t } = useTranslation();
  const [open, setOpen] = useState(false);
  const [current, setCurrent] = useState('');
  const [next, setNext] = useState('');
  const [confirm, setConfirm] = useState('');
  const [fieldErrors, setFieldErrors] = useState<
    Partial<Record<'password' | 'passwordConfirm' | 'currentPassword', string>>
  >({});
  const [done, setDone] = useState(false);
  const limits = registrationPasswordLimits();

  const mutation = useMutation({
    mutationFn: (patch: UserPatchDto) => patchUser(userUri, patch),
    onSuccess: async (_updated, variables) => {
      setDone(true);
      setOpen(false);
      setCurrent('');
      setNext('');
      setConfirm('');
      setFieldErrors({});
      // Password change bumps password_version — old JWTs stop authenticating immediately.
      // Re-issue the token pair with Basic using the new password (stay logged in across reload).
      const email = useSessionStore.getState().currentUser?.email;
      const newPassword = variables.password;
      if (email && newPassword) {
        try {
          await sessionClient.loginBasic(email, newPassword);
        } catch {
          useSessionStore.getState().logout();
        }
      }
    },
    onError: (err) => {
      if (err instanceof ApiError && err.body?.errors?.length) {
        const nextErrors: typeof fieldErrors = {};
        for (const fe of err.body.errors) {
          if (
            fe.field === 'password'
            || fe.field === 'passwordConfirm'
            || fe.field === 'currentPassword'
          ) {
            nextErrors[fe.field] = fe.message;
          }
        }
        if (Object.keys(nextErrors).length > 0) {
          setFieldErrors((prev) => ({ ...prev, ...nextErrors }));
        }
      }
    },
  });

  function passwordMsg(code: ReturnType<typeof validatePassword>): string | null {
    if (code === 'tooShort') {
      return t('auth.register.passwordTooShort', { count: limits.registrationPasswordMinLength });
    }
    if (code === 'tooLong') {
      return t('auth.register.passwordTooLong', { count: limits.registrationPasswordMaxLength });
    }
    return null;
  }

  function onSubmit(e: FormEvent) {
    e.preventDefault();
    setDone(false);
    const pwdErr = passwordMsg(validatePassword(next));
    const confirmErr = validatePasswordConfirm(next, confirm) ? t('profile.me.passwordMismatch') : null;
    const nextErrors: typeof fieldErrors = {};
    if (pwdErr) nextErrors.password = pwdErr;
    if (confirmErr) nextErrors.passwordConfirm = confirmErr;
    setFieldErrors(nextErrors);
    if (pwdErr) {
      document.getElementById('newPassword')?.focus();
      return;
    }
    if (confirmErr) {
      document.getElementById('newPasswordConfirm')?.focus();
      return;
    }
    mutation.mutate({ password: next, passwordConfirm: confirm, currentPassword: current });
  }

  return (
    <div className="profile-card profile-card--section">
      <h2 className="profile-section-title">{t('profile.me.passwordSectionTitle')}</h2>
      <hr className="profile-card__divider" />
      <p className="text-muted small mb-3">{t('profile.me.passwordSectionHint')}</p>

      {done && (
        <div className="alert alert-success" role="alert">
          {t('profile.me.passwordChanged')}
        </div>
      )}

      {!open ? (
        <Button variant="outline-primary" onClick={() => setOpen(true)}>
          {t('profile.me.changePassword')}
        </Button>
      ) : (
        <Form onSubmit={onSubmit} noValidate style={{ maxWidth: 520 }}>
          <p className="text-muted small">{t('profile.me.passwordIntro')}</p>
          <Form.Group className="mb-3" controlId="currentPassword">
            <Form.Label>{t('profile.me.currentPassword')}</Form.Label>
            <Form.Control
              type="password"
              value={current}
              autoComplete="current-password"
              isInvalid={!!fieldErrors.currentPassword}
              onChange={(e) => {
                setCurrent(e.target.value);
                setFieldErrors((prev) => {
                  if (!prev.currentPassword) return prev;
                  const next = { ...prev };
                  delete next.currentPassword;
                  return next;
                });
              }}
              required
            />
            {fieldErrors.currentPassword ? (
              <div className="text-danger small d-block mt-1" role="alert">{fieldErrors.currentPassword}</div>
            ) : null}
          </Form.Group>
          <Form.Group className="mb-3" controlId="newPassword">
            <Form.Label>{t('profile.me.newPassword')}</Form.Label>
            <Form.Control
              type="password"
              value={next}
              autoComplete="new-password"
              isInvalid={!!fieldErrors.password}
              onChange={(e) => {
                const value = e.target.value;
                setNext(value);
                const msg = passwordMsg(validatePassword(value));
                setFieldErrors((prev) => {
                  const errs = { ...prev };
                  if (msg) errs.password = msg;
                  else delete errs.password;
                  if (confirm) {
                    const mismatch = validatePasswordConfirm(value, confirm)
                      ? t('profile.me.passwordMismatch')
                      : null;
                    if (mismatch) errs.passwordConfirm = mismatch;
                    else delete errs.passwordConfirm;
                  }
                  return errs;
                });
              }}
              required
            />
            {fieldErrors.password ? (
              <div className="text-danger small d-block mt-1" role="alert">{fieldErrors.password}</div>
            ) : (
              <Form.Text className="text-muted">
                {t('auth.register.passwordHint', {
                  min: limits.registrationPasswordMinLength,
                  max: limits.registrationPasswordMaxLength,
                })}
              </Form.Text>
            )}
          </Form.Group>
          <Form.Group className="mb-3" controlId="newPasswordConfirm">
            <Form.Label>{t('profile.me.newPasswordConfirm')}</Form.Label>
            <Form.Control
              type="password"
              value={confirm}
              autoComplete="new-password"
              isInvalid={!!fieldErrors.passwordConfirm}
              onChange={(e) => {
                const value = e.target.value;
                setConfirm(value);
                const mismatch = validatePasswordConfirm(next, value)
                  ? t('profile.me.passwordMismatch')
                  : null;
                setFieldErrors((prev) => {
                  const errs = { ...prev };
                  if (mismatch) errs.passwordConfirm = mismatch;
                  else delete errs.passwordConfirm;
                  return errs;
                });
              }}
              required
            />
            {fieldErrors.passwordConfirm ? (
              <div className="text-danger small d-block mt-1" role="alert">{fieldErrors.passwordConfirm}</div>
            ) : null}
          </Form.Group>
          {mutation.isError && (
            <div className="alert alert-danger" role="alert">
              {apiErrorMessage(t, mutation.error)}
            </div>
          )}
          <div className="d-flex gap-2">
            <Button
              type="button"
              variant="outline-secondary"
              onClick={() => {
                setOpen(false);
                setFieldErrors({});
                setCurrent('');
                setNext('');
                setConfirm('');
              }}
            >
              {t('profile.common.cancel')}
            </Button>
            <Button type="submit" variant="primary" disabled={mutation.isPending}>
              {mutation.isPending ? t('profile.common.saving') : t('profile.me.changePassword')}
            </Button>
          </div>
        </Form>
      )}
    </div>
  );
}
