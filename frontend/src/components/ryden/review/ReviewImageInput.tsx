import { useEffect, useId, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';

export interface ReviewImageInputProps {
  id?: string;
  value: File | null;
  onChange: (file: File | null) => void;
}

/**
 * Input de imagen de reseña con preview (miniatura + nombre + botón de
 * quitar). Sin archivo: solo “Elegir imagen”. Con archivo: preview + quitar
 * (y reemplazar), sin el botón primario al costado.
 */
export default function ReviewImageInput({ id, value, onChange }: ReviewImageInputProps) {
  const { t } = useTranslation();
  const autoId = useId();
  const inputId = id ?? `review-image-${autoId}`;
  const inputRef = useRef<HTMLInputElement>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);

  useEffect(() => {
    if (!value) {
      setPreviewUrl(null);
      return;
    }
    const url = URL.createObjectURL(value);
    setPreviewUrl(url);
    return () => URL.revokeObjectURL(url);
  }, [value]);

  const clear = () => {
    onChange(null);
    if (inputRef.current) inputRef.current.value = '';
  };

  return (
    <div className="d-flex flex-column align-items-start gap-2">
      <input
        ref={inputRef}
        id={inputId}
        type="file"
        accept="image/*"
        className="visually-hidden"
        onChange={(e) => onChange(e.target.files?.[0] ?? null)}
      />
      {previewUrl && value ? (
        <>
          <div className="border rounded p-2 position-relative" style={{ maxWidth: 240 }}>
            <img
              src={previewUrl}
              alt={value.name}
              className="img-fluid rounded"
              style={{ height: 130, objectFit: 'cover', width: '100%' }}
            />
            <small className="d-block text-truncate mt-1" style={{ maxWidth: 220 }}>
              {value.name}
            </small>
            <button
              type="button"
              className="btn btn-sm btn-danger position-absolute top-0 end-0 m-1"
              aria-label={t('res.review.removeImage')}
              onClick={clear}
            >
              <i className="bi bi-trash" aria-hidden="true" />
            </button>
          </div>
          <label htmlFor={inputId} className="btn btn-outline-secondary btn-sm mb-0">
            {t('res.review.replaceImage')}
          </label>
        </>
      ) : (
        <label htmlFor={inputId} className="btn btn-outline-secondary btn-sm mb-0">
          {t('res.review.chooseImage')}
        </label>
      )}
    </div>
  );
}
