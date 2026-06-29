import { useEffect, useId, type ReactNode } from 'react';
import { useTranslation } from 'react-i18next';

export type ModalSize = 'sm' | 'md' | 'lg' | 'xl';
export type ModalVariant = 'default' | 'danger' | 'warning';

export interface RydenModalProps {
  id: string;
  title: string;
  message?: string;
  size?: ModalSize;
  variant?: ModalVariant;
  cssClass?: string;
  triggerLabel?: string;
  triggerClass?: string;
  confirmLabel?: string;
  cancelLabel?: string;
  confirmClass?: string;
  cancelClass?: string;
  open?: boolean;
  closable?: boolean;
  showFooter?: boolean;
  onOpenChange?: (open: boolean) => void;
  onConfirm?: () => void;
  onCancel?: () => void;
  children?: ReactNode;
}

/**
 * Espejo de {@code ryden:modal}: overlay custom ({@code modal-overlay}) con backdrop,
 * header, body (mensaje + children) y footer opcional.
 */
export default function Modal({
  id,
  title,
  message,
  size = 'md',
  variant = 'default',
  cssClass = '',
  triggerLabel,
  triggerClass = 'btn btn-primary btn-md',
  confirmLabel,
  cancelLabel,
  confirmClass = 'btn btn-primary btn-md',
  cancelClass = 'btn btn-secondary btn-md',
  open = false,
  closable = true,
  showFooter = true,
  onOpenChange,
  onConfirm,
  onCancel,
  children,
}: RydenModalProps) {
  const { t } = useTranslation();
  const titleId = useId();
  const hasFooter = showFooter && (cancelLabel || confirmLabel);

  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && closable) onOpenChange?.(false);
    };
    document.addEventListener('keydown', onKey);
    return () => document.removeEventListener('keydown', onKey);
  }, [open, closable, onOpenChange]);

  const close = () => onOpenChange?.(false);

  const modalClasses = [
    'modal-overlay',
    `modal-overlay--${size}`,
    `modal-overlay--${variant}`,
    cssClass,
    open ? 'is-open' : '',
  ]
    .filter(Boolean)
    .join(' ');

  return (
    <>
      {triggerLabel ? (
        <button type="button" className={triggerClass} onClick={() => onOpenChange?.(true)}>
          {triggerLabel}
        </button>
      ) : null}

      <div id={id} className={modalClasses} data-modal aria-hidden={!open}>
        <div className="modal__backdrop" onClick={closable ? close : undefined} aria-hidden="true" />
        <div
          className="modal__dialog"
          role="dialog"
          aria-modal="true"
          aria-labelledby={titleId}
          tabIndex={-1}
        >
          <div className="modal__content">
            {closable ? (
              <button
                type="button"
                className="modal__close"
                aria-label={t('common.close')}
                onClick={close}
              >
                <span aria-hidden="true">&times;</span>
              </button>
            ) : null}

            <div className="modal__header">
              <h2 id={titleId} className="modal__title">
                {title}
              </h2>
            </div>

            <div className="modal__body">
              {message ? <p className="modal__message">{message}</p> : null}
              {children}
            </div>

            {hasFooter ? (
              <div className="modal__footer">
                {cancelLabel ? (
                  <button
                    type="button"
                    className={cancelClass}
                    data-modal-action="cancel"
                    onClick={() => {
                      onCancel?.();
                      close();
                    }}
                  >
                    {cancelLabel}
                  </button>
                ) : null}
                {confirmLabel ? (
                  <button
                    type="button"
                    className={confirmClass}
                    data-modal-action="confirm"
                    onClick={() => {
                      onConfirm?.();
                      close();
                    }}
                  >
                    {confirmLabel}
                  </button>
                ) : null}
              </div>
            ) : null}
          </div>
        </div>
      </div>
    </>
  );
}
