import { useEffect, useId, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';

/** Alineado con {@code app.upload.max-payment-receipt-megabytes=5}. */
export const MAX_RECEIPT_BYTES = 5 * 1024 * 1024;

export type ReceiptFileValidationError = 'invalidType' | 'tooLarge';

export function validateReceiptFile(
  file: File,
  maxBytes: number = MAX_RECEIPT_BYTES,
): ReceiptFileValidationError | null {
  const type = (file.type || '').toLowerCase();
  const name = (file.name || '').toLowerCase();
  const okType =
    type.startsWith('image/')
    || type === 'application/pdf'
    || name.endsWith('.pdf')
    || /\.(jpe?g|png|gif|webp)$/.test(name);
  if (!okType) return 'invalidType';
  if (file.size > maxBytes) return 'tooLarge';
  return null;
}

export interface ConfirmedUploadPickerLabels {
  chooseFile: string;
  confirmUpload: string;
  confirming: string;
  uploadAria: string;
  replaceFile: string;
  removeFile: string;
  invalidFile: string;
  fileTooLarge: string;
  uploadError: string;
}

export interface ReceiptUploadPickerProps {
  id?: string;
  disabled?: boolean;
  busy?: boolean;
  /** Tras confirmar, el padre sube el archivo; el picker se limpia solo si onConfirm resuelve. */
  onConfirm: (file: File) => void | Promise<void>;
  className?: string;
  /** Si se omite, usa las claves de comprobante de reserva (`res.confirmation.*`). */
  labels?: ConfirmedUploadPickerLabels;
  accept?: string;
  maxBytes?: number;
}

/**
 * Flujo de subida con confirmación (espejo JSP): elegir archivo → preview →
 * confirmar. Sirve para comprobantes de pago/reintegro, seguro, docs KYC, etc.
 */
export default function ReceiptUploadPicker({
  id,
  disabled = false,
  busy = false,
  onConfirm,
  className,
  labels,
  accept = 'image/*,application/pdf',
  maxBytes = MAX_RECEIPT_BYTES,
}: ReceiptUploadPickerProps) {
  const { t } = useTranslation();
  const autoId = useId();
  const inputId = id ?? `receipt-upload-${autoId}`;
  const inputRef = useRef<HTMLInputElement>(null);
  const replaceInputRef = useRef<HTMLInputElement>(null);

  const copy: ConfirmedUploadPickerLabels = labels ?? {
    chooseFile: t('res.confirmation.chooseFile'),
    confirmUpload: t('res.confirmation.confirmUpload'),
    confirming: t('res.confirmation.confirming'),
    uploadAria: t('res.confirmation.uploadAria'),
    replaceFile: t('res.confirmation.replaceFile'),
    removeFile: t('res.confirmation.removeFile'),
    invalidFile: t('res.confirmation.invalidFile'),
    fileTooLarge: t('res.confirmation.fileTooLarge', { maxMb: Math.round(maxBytes / (1024 * 1024)) }),
    uploadError: t('res.confirmation.uploadError'),
  };

  const [file, setFile] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [confirming, setConfirming] = useState(false);

  const locked = disabled || busy || confirming;
  const canSubmit = Boolean(file) && !locked;
  const isPdf =
    Boolean(file)
    && ((file!.type || '').toLowerCase() === 'application/pdf'
      || file!.name.toLowerCase().endsWith('.pdf'));

  useEffect(() => {
    if (!file || isPdf) {
      setPreviewUrl(null);
      return;
    }
    const url = URL.createObjectURL(file);
    setPreviewUrl(url);
    return () => URL.revokeObjectURL(url);
  }, [file, isPdf]);

  const clearSelection = () => {
    setFile(null);
    setError(null);
    if (inputRef.current) inputRef.current.value = '';
    if (replaceInputRef.current) replaceInputRef.current.value = '';
  };

  const applyFile = (next: File | null) => {
    if (!next) {
      clearSelection();
      return;
    }
    const validation = validateReceiptFile(next, maxBytes);
    if (validation === 'invalidType') {
      setError(copy.invalidFile);
      setFile(null);
      if (inputRef.current) inputRef.current.value = '';
      if (replaceInputRef.current) replaceInputRef.current.value = '';
      return;
    }
    if (validation === 'tooLarge') {
      setError(
        labels?.fileTooLarge
          ?? t('res.confirmation.fileTooLarge', { maxMb: Math.round(maxBytes / (1024 * 1024)) }),
      );
      setFile(null);
      if (inputRef.current) inputRef.current.value = '';
      if (replaceInputRef.current) replaceInputRef.current.value = '';
      return;
    }
    setError(null);
    setFile(next);
  };

  const handleConfirm = async () => {
    if (!file || locked) return;
    setConfirming(true);
    setError(null);
    try {
      await onConfirm(file);
      clearSelection();
    } catch {
      setError(copy.uploadError);
    } finally {
      setConfirming(false);
    }
  };

  const submitLabel = confirming || busy ? copy.confirming : copy.confirmUpload;

  return (
    <div className={className}>
      <div className="d-flex align-items-stretch gap-2 ryden-payment-receipt__form">
        <label className="form-control d-flex align-items-center mb-0 flex-grow-1 min-w-0 position-relative ryden-payment-receipt__file-label">
          <span
            className={`text-truncate pe-1 flex-grow-1 min-w-0${file ? '' : ' text-muted'}`}
            title={file?.name}
          >
            {file ? file.name : copy.chooseFile}
          </span>
          <input
            ref={inputRef}
            id={inputId}
            type="file"
            className="position-absolute top-0 start-0 w-100 h-100 opacity-0 ryden-payment-receipt__file-input"
            accept={accept}
            disabled={locked}
            aria-label={copy.uploadAria}
            onChange={(e) => applyFile(e.target.files?.[0] ?? null)}
          />
        </label>
        <button
          type="button"
          className="btn btn-sm btn-primary flex-shrink-0 d-inline-flex align-items-center justify-content-center gap-1 px-2"
          disabled={!canSubmit}
          aria-label={copy.uploadAria}
          title={copy.uploadAria}
          onClick={() => void handleConfirm()}
        >
          <i className="bi bi-cloud-arrow-up" aria-hidden="true" />
          <span className="d-none d-sm-inline">{submitLabel}</span>
        </button>
      </div>

      {file ? (
        <div className="border rounded-3 p-3 bg-white mt-3">
          <div className="d-flex flex-column flex-sm-row gap-3 align-items-start">
            <div
              className="rounded-3 overflow-hidden border bg-body-tertiary flex-shrink-0 d-flex align-items-center justify-content-center"
              style={{ width: 140, height: 105 }}
            >
              {previewUrl ? (
                <img
                  src={previewUrl}
                  alt={file.name}
                  className="w-100 h-100"
                  style={{ objectFit: 'cover' }}
                />
              ) : (
                <div className="text-center text-secondary px-2">
                  <i className="bi bi-file-earmark-pdf fs-2 d-block" aria-hidden="true" />
                  <span className="small">PDF</span>
                </div>
              )}
            </div>
            <div className="min-w-0 flex-grow-1">
              <p className="fw-semibold mb-1 text-truncate" title={file.name}>
                {file.name}
              </p>
              <p className="small text-secondary mb-3">
                {(file.size / (1024 * 1024)).toFixed(2)} MB
              </p>
              <div className="d-flex flex-wrap gap-2">
                <label className={`btn btn-outline-secondary btn-sm mb-0${locked ? ' disabled' : ''}`}>
                  {copy.replaceFile}
                  <input
                    ref={replaceInputRef}
                    type="file"
                    className="visually-hidden"
                    accept={accept}
                    disabled={locked}
                    onChange={(e) => applyFile(e.target.files?.[0] ?? null)}
                  />
                </label>
                <button
                  type="button"
                  className="btn btn-outline-danger btn-sm"
                  disabled={locked}
                  onClick={clearSelection}
                >
                  {copy.removeFile}
                </button>
              </div>
            </div>
          </div>
        </div>
      ) : null}

      {error ? (
        <div className="text-danger small mt-2" role="alert">
          {error}
        </div>
      ) : null}
    </div>
  );
}
