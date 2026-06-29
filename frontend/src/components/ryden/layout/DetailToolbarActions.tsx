import { useTranslation } from 'react-i18next';

export interface DetailToolbarActionsProps {
  saveLabel?: string;
  onSave?: () => void;
}

/** Espejo de {@code ryden:detailToolbarActions}. */
export default function DetailToolbarActions({ saveLabel, onSave }: DetailToolbarActionsProps) {
  const { t } = useTranslation();
  const label = saveLabel ?? t('detailToolbar.save');

  return (
    <div className="d-flex flex-shrink-0 gap-2 detail-toolbar-actions">
      <button
        type="button"
        className="btn btn-light border rounded-3 d-inline-flex align-items-center gap-2 px-3 py-2"
        id="detailSaveBtn"
        aria-label={label}
        onClick={onSave}
      >
        <i className="bi bi-heart" aria-hidden="true"></i>
        <span>{label}</span>
      </button>
    </div>
  );
}
