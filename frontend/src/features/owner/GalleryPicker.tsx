import { useCallback, useEffect, useId, useMemo, useRef, useState, type ChangeEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { publishValidationI18nParams, validateGalleryFiles } from './publishCarValidation';

// Replica el selector de galería del viejo publishCarForm.jsp (+ js/components.js):
// permite elegir VARIAS fotos/videos (acumulando entre selecciones, sin duplicar),
// muestra una previsualización de cada archivo, deja quitarlos y elegir cuál es la
// foto "principal" (portada). Al cambiar la selección o la portada emite la lista
// ORDENADA con la portada primero (displayOrder 0 ⇒ principal en el backend).

function isImage(file: File): boolean {
  return (file.type || '').toLowerCase().startsWith('image/');
}

function isAllowedMedia(file: File): boolean {
  const t = (file.type || '').toLowerCase();
  return t.startsWith('image/') || t === 'video/mp4' || t === 'video/webm' || t === 'video/quicktime';
}

export interface GalleryPickerProps {
  /** Recibe los archivos en orden final (portada primero), listos para subir. */
  onChange: (orderedFiles: File[]) => void;
}

export default function GalleryPicker({ onChange }: GalleryPickerProps) {
  const { t } = useTranslation();
  const inputId = useId();
  const inputRef = useRef<HTMLInputElement>(null);

  const [files, setFiles] = useState<File[]>([]);
  const [coverIndex, setCoverIndex] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const [coverConfirm, setCoverConfirm] = useState(false);

  // Object URLs DERIVADAS de files (no en estado): así la preview nunca queda un
  // render atrasada respecto a files (evita thumbnails rotos/parpadeo al
  // agregar/quitar). Se revocan las anteriores cuando cambia la lista o al
  // desmontar.
  const previews = useMemo(() => files.map((f) => URL.createObjectURL(f)), [files]);
  useEffect(() => () => previews.forEach((u) => URL.revokeObjectURL(u)), [previews]);

  // La portada debe ser una imagen; default = primera imagen.
  const defaultCoverIndex = useCallback((list: File[]): number => {
    const idx = list.findIndex(isImage);
    return idx >= 0 ? idx : 0;
  }, []);

  // Emite la lista ordenada (portada primero) ante cualquier cambio.
  useEffect(() => {
    if (files.length === 0) {
      onChange([]);
      return;
    }
    const ordered = [...files];
    if (coverIndex > 0 && coverIndex < ordered.length) {
      const [cover] = ordered.splice(coverIndex, 1);
      ordered.unshift(cover);
    }
    onChange(ordered);
  }, [files, coverIndex, onChange]);

  function onAdd(e: ChangeEvent<HTMLInputElement>) {
    setError(null);
    const incoming = e.target.files ? Array.from(e.target.files) : [];
    e.target.value = ''; // permite re-elegir el mismo archivo
    if (incoming.length === 0) return;

    // Descartamos los inválidos individualmente (avisando) en vez de abortar todo
    // el lote por un solo archivo no soportado.
    const valid = incoming.filter(isAllowedMedia);
    if (valid.length < incoming.length) setError(t('owner.publish.errors.notGalleryMedia'));
    if (valid.length === 0) return;

    setFiles((prev) => {
      const hadItems = prev.length > 0;
      const merged = [...prev];
      for (const f of valid) {
        const dup = merged.some(
          (m) => m.name === f.name && m.size === f.size && m.lastModified === f.lastModified,
        );
        if (!dup) merged.push(f);
      }
      const galleryError = validateGalleryFiles(merged);
      if (galleryError) {
        setError(t(galleryError, publishValidationI18nParams()));
        return prev;
      }
      if (!hadItems && merged.length > 0) {
        setCoverIndex(defaultCoverIndex(merged));
        setCoverConfirm(true);
      }
      return merged;
    });
  }

  function onRemove(index: number) {
    setFiles((prev) => {
      const next = prev.filter((_, i) => i !== index);
      setCoverIndex((ci) => {
        if (next.length === 0) return 0;
        if (index === ci) return defaultCoverIndex(next);
        return index < ci ? ci - 1 : ci;
      });
      return next;
    });
  }

  function onSetCover(index: number) {
    if (index === coverIndex || !isImage(files[index])) return;
    setCoverIndex(index);
    setCoverConfirm(true);
  }

  return (
    <div className="mb-4">
      <span className="form-label required-label d-block">{t('owner.publish.pictures')}</span>
      <input
        ref={inputRef}
        id={inputId}
        type="file"
        className="form-control"
        accept="image/*,video/mp4,video/webm,video/quicktime,.mp4,.webm,.mov"
        multiple
        onChange={onAdd}
      />
      <small className="text-muted d-block mt-2">{t('owner.publish.picturesHint')}</small>

      {error && <div className="alert alert-danger mt-2 py-2 small" role="alert">{error}</div>}

      {coverConfirm && files.length > 0 && (
        <div className="publish-gallery-cover-confirm mt-2 py-2 small" role="status" aria-live="polite">
          {t('owner.publish.cover.changed')}
        </div>
      )}

      {files.length > 0 && (
        <div className="row g-2 mt-2" id="picturesPreview">
          {files.map((file, index) => {
            const image = isImage(file);
            const isCover = image && index === coverIndex;
            return (
              <div className="col-6 col-md-4" key={`${file.name}-${file.size}-${index}`}>
                <div
                  className={`publish-gallery-item border rounded p-2 position-relative${isCover ? ' publish-gallery-item--cover' : ''}`}
                  role={image && !isCover ? 'button' : undefined}
                  tabIndex={image && !isCover ? 0 : undefined}
                  aria-label={image && !isCover ? t('owner.publish.cover.set') : undefined}
                  onClick={image && !isCover ? () => onSetCover(index) : undefined}
                  onKeyDown={
                    image && !isCover
                      ? (ev) => {
                          if (ev.key === 'Enter' || ev.key === ' ') {
                            ev.preventDefault();
                            onSetCover(index);
                          }
                        }
                      : undefined
                  }
                >
                  {image ? (
                    <img className="img-fluid rounded publish-gallery-media" src={previews[index]} alt={file.name} />
                  ) : (
                    <>
                      <video
                        className="img-fluid rounded w-100 publish-gallery-media"
                        muted
                        playsInline
                        preload="metadata"
                        src={previews[index]}
                      />
                      <span
                        className="position-absolute top-50 start-50 translate-middle text-white fs-2"
                        aria-hidden="true"
                      >
                        <i className="bi bi-play-circle" />
                      </span>
                    </>
                  )}

                  <button
                    type="button"
                    className="btn btn-sm btn-danger position-absolute top-0 end-0 m-1"
                    aria-label={t('owner.publish.removeImage')}
                    onClick={(ev) => {
                      ev.stopPropagation();
                      onRemove(index);
                    }}
                  >
                    <i className="bi bi-trash" aria-hidden="true" />
                  </button>

                  {isCover && (
                    <span className="publish-gallery-cover-badge" title={t('owner.publish.cover.tooltip')}>
                      <i className="bi bi-star-fill" aria-hidden="true" /> {t('owner.publish.cover.badge')}
                    </span>
                  )}

                  {image && !isCover && (
                    <div className="publish-gallery-cover-overlay" aria-hidden="true">
                      <span className="btn btn-light btn-sm publish-gallery-cover-set-btn">
                        {t('owner.publish.cover.set')}
                      </span>
                    </div>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
