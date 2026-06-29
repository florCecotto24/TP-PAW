import type { FormEvent, ReactNode } from 'react';
import Modal, { type ModalSize, type ModalVariant } from './Modal';

export interface ConfirmModalProps {
  id: string;
  title: string;
  message: string;
  action: string;
  cancelLabel: string;
  confirmLabel: string;
  triggerLabel?: string;
  triggerClass?: string;
  variant?: ModalVariant;
  confirmButtonClass?: string;
  cancelButtonClass?: string;
  size?: ModalSize;
  method?: 'get' | 'post';
  open?: boolean;
  onOpenChange?: (open: boolean) => void;
  /** Campos ocultos u otros inputs dentro del formulario POST. */
  children?: ReactNode;
  onSubmit?: (e: FormEvent<HTMLFormElement>) => void;
}

/**
 * Espejo de {@code ryden:confirmModal}: compone {@link Modal} con formulario
 * (cancel + submit) y {@code showFooter=false} en el modal base.
 */
export default function ConfirmModal({
  id,
  title,
  message,
  action,
  cancelLabel,
  confirmLabel,
  triggerLabel,
  triggerClass = 'btn btn-primary',
  variant = 'default',
  confirmButtonClass = 'btn btn-primary',
  cancelButtonClass = 'btn btn-secondary',
  size = 'md',
  method = 'post',
  open,
  onOpenChange,
  children,
  onSubmit,
}: ConfirmModalProps) {
  return (
    <Modal
      id={id}
      title={title}
      message={message}
      size={size}
      variant={variant}
      showFooter={false}
      triggerLabel={triggerLabel}
      triggerClass={triggerClass}
      open={open}
      onOpenChange={onOpenChange}
    >
      <form method={method} action={action} onSubmit={onSubmit}>
        {children}
        <div className="d-flex justify-content-end gap-2 mt-3">
          <button type="button" className={cancelButtonClass} onClick={() => onOpenChange?.(false)}>
            {cancelLabel}
          </button>
          <button type="submit" className={confirmButtonClass}>
            {confirmLabel}
          </button>
        </div>
      </form>
    </Modal>
  );
}
