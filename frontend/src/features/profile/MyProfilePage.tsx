import { useEffect, useRef, useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { Button, Form } from 'react-bootstrap';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Avatar, BreadcrumbTrail, LoadingBlock, FieldView, ReceiptUploadPicker } from '../../components/ryden';
import { profilePictureAssetUrl } from '../../api/uri';
import { getClientConfig } from '../../api/clientConfig';
import { useSessionStore } from '../../session/sessionStore';
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
import { formatDate, formatMonthYear } from '../../i18n/dateFormat';

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
  // El backend incluye links.profilePicture SOLO si el usuario tiene foto, así que
  // su presencia indica si existe (mostramos imagen y la opción "quitar"); si está
  // ausente, mostramos el placeholder de iniciales.
  const pictureLink = profilePictureAssetUrl(user.links) ?? undefined;
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
            className="profile-card__avatar"
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
  const [validationError, setValidationError] = useState<string | null>(null);
  const userLimits = getClientConfig().user;

  // Resincroniza si el usuario base cambia (tras invalidate).
  useEffect(() => setForm(toForm(user)), [user]);

  const mutation = useMutation({
    mutationFn: (patch: UserPatchDto) => patchUser(userUri, patch),
    onSuccess: () => {
      setDone(true);
      setEditing(false);
      onSaved();
    },
  });

  function update<K extends keyof ProfileFormValues>(key: K, value: string) {
    setForm((f) => ({ ...f, [key]: value }));
    setDone(false);
    setValidationError(null);
  }

  function onSubmit(e: FormEvent) {
    e.preventDefault();
    // Validaciones client-side que el JSP mostraba inline (Pattern/@Size/@Past).
    const phone = form.phoneNumber.trim();
    if (phone && !/^\+?[0-9]{1,14}$/.test(phone)) {
      setValidationError(t('profile.me.phoneInvalid'));
      return;
    }
    if (form.birthDate && form.birthDate > new Date().toISOString().slice(0, 10)) {
      setValidationError(t('profile.me.birthDateFuture'));
      return;
    }
    const cbuDigits = getClientConfig().cbuRequiredDigits;
    const cbu = form.cbu.trim();
    if (cbu && !new RegExp(`^\\d{${cbuDigits}}$`).test(cbu)) {
      setValidationError(t('profile.me.cbuInvalid', { digits: cbuDigits }));
      return;
    }
    setValidationError(null);
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
        {validationError && (
          <div className="alert alert-danger" role="alert">
            {validationError}
          </div>
        )}
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
              onChange={(e) => update('phoneNumber', e.target.value)}
            />
          </Form.Group>
          <Form.Group className="mb-3" controlId="birthDate">
            <Form.Label>{t('profile.me.birthDate')}</Form.Label>
            <Form.Control
              type="date"
              value={form.birthDate}
              autoComplete="bday"
              onChange={(e) => update('birthDate', e.target.value)}
            />
          </Form.Group>
          <Form.Group className="mb-3" controlId="cbu">
            <Form.Label>{t('profile.me.cbu')}</Form.Label>
            <Form.Control
              value={form.cbu}
              inputMode="numeric"
              maxLength={getClientConfig().cbuRequiredDigits}
              onChange={(e) => update('cbu', e.target.value)}
            />
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

  const upload = useMutation({
    mutationFn: (file: File) => uploadDocument(user, type, file),
    onSuccess: () => {
      setBanner('uploaded');
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
    try {
      await upload.mutateAsync(file);
    } catch (err) {
      throw err;
    }
  };

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
        ) : (
          <i className="bi bi-x-circle-fill text-danger" aria-hidden="true"></i>
        )}
        <span className="ms-1">
          {validated ? t('profile.docs.validated') : t('profile.docs.pending')}
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
          <Button
            type="button"
            variant="outline-danger"
            size="sm"
            className="d-block mb-3"
            onClick={() => remove.mutate()}
            disabled={remove.isPending}
          >
            {t('profile.docs.remove')}
          </Button>
          <div>
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
          </div>
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
  const [localError, setLocalError] = useState<string | null>(null);
  const [done, setDone] = useState(false);

  const mutation = useMutation({
    mutationFn: (patch: UserPatchDto) => patchUser(userUri, patch),
    onSuccess: () => {
      setDone(true);
      setOpen(false);
      setCurrent('');
      setNext('');
      setConfirm('');
    },
  });

  function onSubmit(e: FormEvent) {
    e.preventDefault();
    setLocalError(null);
    setDone(false);
    if (next !== confirm) {
      setLocalError(t('profile.me.passwordMismatch'));
      return;
    }
    mutation.mutate({ password: next, currentPassword: current });
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
              onChange={(e) => setCurrent(e.target.value)}
              required
            />
          </Form.Group>
          <Form.Group className="mb-3" controlId="newPassword">
            <Form.Label>{t('profile.me.newPassword')}</Form.Label>
            <Form.Control
              type="password"
              value={next}
              autoComplete="new-password"
              onChange={(e) => setNext(e.target.value)}
              required
            />
          </Form.Group>
          <Form.Group className="mb-3" controlId="newPasswordConfirm">
            <Form.Label>{t('profile.me.newPasswordConfirm')}</Form.Label>
            <Form.Control
              type="password"
              value={confirm}
              autoComplete="new-password"
              onChange={(e) => setConfirm(e.target.value)}
              required
            />
          </Form.Group>
          {localError && (
            <div className="alert alert-danger" role="alert">
              {localError}
            </div>
          )}
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
                setLocalError(null);
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
