import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { paths, myReservationDetail } from '../../../routes/paths';
import { idFromUri } from '../../../api/uri';
import Modal from '../primitives/Modal';

export interface BlockedUserBannerProps {
  blockedOverdueReservationUri?: string | null;
}

/** Banner de cuenta bloqueada (extraído de {@code navbar.tag}). */
export function BlockedUserBanner({ blockedOverdueReservationUri }: BlockedUserBannerProps) {
  const { t } = useTranslation();
  const singleReservationId = blockedOverdueReservationUri
    ? idFromUri(blockedOverdueReservationUri)
    : null;

  return (
    <div
      className="alert alert-danger border-0 rounded-0 mb-0 d-flex flex-wrap align-items-center gap-2 px-3 py-2 ryden-blocked-banner"
      role="alert"
      aria-live="polite"
    >
      <i className="bi bi-shield-exclamation fs-5 me-1" aria-hidden="true"></i>
      <div className="flex-grow-1">
        <strong className="d-block">{t('navbar.blockedBanner.title')}</strong>
        <span className="small">{t('navbar.blockedBanner.body')}</span>
      </div>
      {singleReservationId != null ? (
        <Link
          to={myReservationDetail(singleReservationId, { role: 'OWNER' })}
          className="btn btn-light btn-sm fw-semibold border"
        >
          <i className="bi bi-upload me-1" aria-hidden="true"></i>
          {t('navbar.blockedBanner.cta.uploadSingle')}
        </Link>
      ) : (
        <Link to={paths.ownerReservations} className="btn btn-light btn-sm fw-semibold border">
          <i className="bi bi-receipt me-1" aria-hidden="true"></i>
          {t('navbar.blockedBanner.cta')}
        </Link>
      )}
    </div>
  );
}

export interface LogoutConfirmModalProps {
  open: boolean;
  onClose: () => void;
  onConfirm: () => void;
}

/** Modal de confirmación de logout (extraído de {@code navbar.tag}). */
export function LogoutConfirmModal({ open, onClose, onConfirm }: LogoutConfirmModalProps) {
  const { t } = useTranslation();

  return (
    <Modal
      id="navbarLogoutModal"
      title={t('navbar.logoutConfirm.title')}
      message={t('navbar.logoutConfirm.message')}
      size="sm"
      open={open}
      onOpenChange={(isOpen) => {
        if (!isOpen) onClose();
      }}
      cancelLabel={t('common.cancel')}
      confirmLabel={t('navbar.logoutConfirm.submit')}
      cancelClass="btn btn-light border rounded-3"
      confirmClass="btn btn-primary rounded-3"
      onCancel={onClose}
      onConfirm={onConfirm}
    />
  );
}
