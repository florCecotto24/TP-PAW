import Button from '../primitives/Button';
import { useTranslation } from 'react-i18next';

export interface HostProfileBarProps {
  hostName: string;
  avatarUrl: string;
  responseHint: string;
  onContact?: () => void;
}

/** Espejo de {@code ryden:hostProfileBar}. */
export default function HostProfileBar({
  hostName,
  avatarUrl,
  responseHint,
  onContact,
}: HostProfileBarProps) {
  const { t } = useTranslation();

  return (
    <div className="host-profile-bar d-flex align-items-center justify-content-between flex-wrap gap-3 rounded-4 p-3 mt-4">
      <div className="d-flex align-items-center gap-3">
        <img
          src={avatarUrl}
          alt=""
          width={48}
          height={48}
          className="rounded-circle object-fit-cover flex-shrink-0"
        />
        <div>
          <p className="mb-0 fw-semibold">{t('hostProfileBar.hostLabel', { name: hostName })}</p>
          <p className="mb-0 small text-secondary d-flex align-items-center gap-1">
            <i className="bi bi-lightning-charge-fill text-primary" aria-hidden="true"></i>
            {responseHint}
          </p>
        </div>
      </div>
      <Button
        text={t('hostProfileBar.contact')}
        size="sm"
        type="primary"
        cssClass="btn-outline-secondary detail-contact-btn"
        id="detailContactBtn"
        onClick={onContact}
      />
    </div>
  );
}
