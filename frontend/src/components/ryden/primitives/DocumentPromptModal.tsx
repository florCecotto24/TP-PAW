import type { ReactNode } from 'react';
import Modal, { type ModalSize, type ModalVariant } from './Modal';

export interface DocumentPromptModalProps {
  id: string;
  title: string;
  licenseInputId?: string;
  identityInputId?: string;
  licenseLabel?: string;
  identityLabel?: string;
  uploadedSlotMessage: string;
  licensePending?: boolean;
  identityPending?: boolean;
  hideLicenseSlot?: boolean;
  hideIdentitySlot?: boolean;
  cancelLabel: string;
  confirmLabel: string;
  errorId: string;
  confirmId?: string;
  openButtonId?: string;
  includeOpenTrigger?: boolean;
  showCloseButton?: boolean;
  confirmButtonClass?: string;
  cancelButtonClass?: string;
  /** When true, confirm (and optionally cancel/close) cannot fire again. */
  confirmDisabled?: boolean;
  size?: ModalSize;
  variant?: ModalVariant;
  open?: boolean;
  onOpenChange?: (open: boolean) => void;
  error?: string;
  onConfirm?: () => void;
  onLicenseChange?: (file: File | null) => void;
  onIdentityChange?: (file: File | null) => void;
  children?: ReactNode;
}

/**
 * Espejo de {@code ryden:documentPromptModal}: dos slots de archivo (licencia / identidad)
 * con estado "subido" o input file, error y cancel/confirm.
 */
export default function DocumentPromptModal({
  id,
  title,
  licenseInputId = 'licenseFile',
  identityInputId = 'identityFile',
  licenseLabel = '',
  identityLabel = '',
  uploadedSlotMessage,
  licensePending = true,
  identityPending = true,
  hideLicenseSlot = false,
  hideIdentitySlot = false,
  cancelLabel,
  confirmLabel,
  errorId,
  confirmId,
  openButtonId,
  includeOpenTrigger = false,
  showCloseButton = true,
  confirmButtonClass = 'btn btn-primary',
  cancelButtonClass = 'btn btn-secondary',
  confirmDisabled = false,
  size = 'md',
  variant = 'default',
  open,
  onOpenChange,
  error,
  onConfirm,
  onLicenseChange,
  onIdentityChange,
  children,
}: DocumentPromptModalProps) {
  const renderLicenseSlot = !hideLicenseSlot;
  const renderIdentitySlot = !hideIdentitySlot;

  return (
    <>
      {includeOpenTrigger && openButtonId ? (
        <button
          type="button"
          id={openButtonId}
          className="d-none"
          aria-hidden="true"
          tabIndex={-1}
          onClick={() => onOpenChange?.(true)}
        >
          open
        </button>
      ) : null}

      <Modal
        id={id}
        title={title}
        size={size}
        variant={variant}
        showFooter={false}
        closable={showCloseButton && !confirmDisabled}
        open={open}
        onOpenChange={onOpenChange}
      >
        {children}
        {renderLicenseSlot ? (
          <>
            <div
              id={`${id}-license-pending`}
              className={`mb-3${licensePending === false ? ' d-none' : ''}`}
            >
              <label htmlFor={licenseInputId} className="form-label">
                {licenseLabel}
              </label>
              <input
                type="file"
                className="form-control"
                id={licenseInputId}
                name={licenseInputId}
                accept="image/*,application/pdf"
                onChange={(e) => onLicenseChange?.(e.target.files?.[0] ?? null)}
              />
            </div>
            <div
              id={`${id}-license-done`}
              className={`mb-3${licensePending !== false ? ' d-none' : ''}`}
            >
              <p className="form-label fw-semibold mb-1">{licenseLabel}</p>
              <p className="text-success small mb-0">
                <span className="bi bi-check-circle-fill me-1" aria-hidden="true"></span>
                {uploadedSlotMessage}
              </p>
            </div>
          </>
        ) : null}
        {renderIdentitySlot ? (
          <>
            <div
              id={`${id}-identity-pending`}
              className={`mb-3${identityPending === false ? ' d-none' : ''}`}
            >
              <label htmlFor={identityInputId} className="form-label">
                {identityLabel}
              </label>
              <input
                type="file"
                className="form-control"
                id={identityInputId}
                name={identityInputId}
                accept="image/*,application/pdf"
                onChange={(e) => onIdentityChange?.(e.target.files?.[0] ?? null)}
              />
            </div>
            <div
              id={`${id}-identity-done`}
              className={`mb-2${identityPending !== false ? ' d-none' : ''}`}
            >
              <p className="form-label fw-semibold mb-1">{identityLabel}</p>
              <p className="text-success small mb-0">
                <span className="bi bi-check-circle-fill me-1" aria-hidden="true"></span>
                {uploadedSlotMessage}
              </p>
            </div>
          </>
        ) : null}
        <p
          id={errorId}
          className={`text-danger small mt-2 mb-0${error ? '' : ' d-none'}`}
          role="alert"
        >
          {error ?? ''}
        </p>
        <div className="d-flex justify-content-end gap-2 mt-4">
          <button
            type="button"
            className={cancelButtonClass}
            disabled={confirmDisabled}
            onClick={() => onOpenChange?.(false)}
          >
            {cancelLabel}
          </button>
          <button
            type="button"
            className={confirmButtonClass}
            id={confirmId}
            disabled={confirmDisabled}
            onClick={onConfirm}
          >
            {confirmLabel}
          </button>
        </div>
      </Modal>
    </>
  );
}
