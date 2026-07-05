import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';

export interface ReviewImageInputProps {
  id?: string;
  value: File | null;
  onChange: (file: File | null) => void;
}

/**
 * Input de imagen de reseña con preview (miniatura + nombre + botón de
 * quitar), espejo de `.ryden-review-picture-input` en `components.js`.
 */
export default function ReviewImageInput({ id, value, onChange }: ReviewImageInputProps) {
  const { t } = useTranslation();
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

  return (
    <div>
      <input
        id={id}
        type="file"
        accept="image/*"
        className="visually-hidden"
        onChange={(e) => onChange(e.target.files?.[0] ?? null)}
      />
      <label htmlFor={id} className="btn btn-outline-secondary btn-sm mb-0">
        {t('res.review.chooseImage')}
      </label>
      {previewUrl && value ? (
        <div className="border rounded p-2 position-relative d-inline-block mt-2" style={{ maxWidth: 240 }}>
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
            onClick={() => onChange(null)}
          >
            <i className="bi bi-trash" aria-hidden="true" />
          </button>
        </div>
      ) : null}
    </div>
  );
}
