import type { InputHTMLAttributes, ReactNode } from 'react';
import Modal, { type ModalSize, type ModalVariant } from './Modal';

export interface DataPromptModalProps {
  id: string;
  title: string;
  fieldId: string;
  fieldLabel: string;
  cancelLabel: string;
  confirmLabel: string;
  errorId: string;
  confirmId?: string;
  openButtonId?: string;
  includeOpenTrigger?: boolean;
  inputType?: string;
  maxlength?: number;
  inputMode?: InputHTMLAttributes<HTMLInputElement>['inputMode'];
  pattern?: string;
  autoComplete?: string;
  formControlClass?: string;
  size?: ModalSize;
  variant?: ModalVariant;
  showCloseButton?: boolean;
  confirmButtonClass?: string;
  cancelButtonClass?: string;
  digitsOnly?: boolean;
  open?: boolean;
  onOpenChange?: (open: boolean) => void;
  value?: string;
  onValueChange?: (value: string) => void;
  error?: string;
  onConfirm?: () => void;
  children?: ReactNode;
}

/**
 * Optional intro (children), a text field,
 * error line and cancel/confirm buttons.
 */
export default function DataPromptModal({
  id,
  title,
  fieldId,
  fieldLabel,
  cancelLabel,
  confirmLabel,
  errorId,
  confirmId,
  openButtonId,
  includeOpenTrigger = false,
  inputType = 'text',
  maxlength,
  inputMode,
  pattern,
  autoComplete = 'off',
  formControlClass = 'form-control',
  size = 'md',
  variant = 'default',
  showCloseButton = true,
  confirmButtonClass = 'btn btn-primary',
  cancelButtonClass = 'btn btn-secondary',
  digitsOnly = false,
  open,
  onOpenChange,
  value = '',
  onValueChange,
  error,
  onConfirm,
  children,
}: DataPromptModalProps) {
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
        closable={showCloseButton}
        open={open}
        onOpenChange={onOpenChange}
      >
        {children}
        <label htmlFor={fieldId} className="form-label">
          {fieldLabel}
        </label>
        <input
          type={inputType}
          className={formControlClass}
          id={fieldId}
          name={fieldId}
          maxLength={maxlength}
          inputMode={inputMode}
          pattern={pattern}
          autoComplete={autoComplete}
          data-ryden-digits-only={digitsOnly ? 'true' : undefined}
          value={value}
          onChange={(e) => onValueChange?.(e.target.value)}
        />
        <p
          id={errorId}
          className={`text-danger small mt-2 mb-0${error ? '' : ' d-none'}`}
          role="alert"
        >
          {error ?? ''}
        </p>
        <div className="d-flex justify-content-end gap-2 mt-4">
          <button type="button" className={cancelButtonClass} onClick={() => onOpenChange?.(false)}>
            {cancelLabel}
          </button>
          <button type="button" className={confirmButtonClass} id={confirmId} onClick={onConfirm}>
            {confirmLabel}
          </button>
        </div>
      </Modal>
    </>
  );
}
