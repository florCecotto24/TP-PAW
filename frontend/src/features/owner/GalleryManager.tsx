import { useEffect, useState, type ChangeEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { addPicture, deletePicture, fetchPictures } from './api';
import { apiAssetUrl } from '../../api/uri';
import { useApiErrorMessage } from './hooks';
import type { CarDto, PictureDto } from './types';

export default function GalleryManager({ car }: { car: CarDto }) {
  const { t } = useTranslation();
  const errorMessage = useApiErrorMessage();

  const [pictures, setPictures] = useState<PictureDto[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [tick, setTick] = useState(0);

  useEffect(() => {
    let active = true;
    fetchPictures(car)
      .then((res) => { if (active) setPictures(res.data ?? []); })
      .catch((err) => { if (active) setError(errorMessage(err)); });
    return () => { active = false; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [car, tick]);

  async function onAdd(e: ChangeEvent<HTMLInputElement>) {
    const files = e.target.files ? Array.from(e.target.files) : [];
    if (files.length === 0) return;
    setError(null);
    setBusy(true);
    try {
      // El `displayOrder` se basa en cuántas imágenes hay YA en el servidor, no en
      // el estado local `pictures` (que puede estar desactualizado si el orden se
      // corrió en el server o si la lista quedó vieja). Releemos el conteo fresco
      // justo antes de asignar el orden y lo incrementamos por archivo.
      const current = await fetchPictures(car);
      let order = current.data?.length ?? 0;
      for (const f of files) await addPicture(car, f, order++);
      setTick((n) => n + 1);
    } catch (err) {
      setError(errorMessage(err, 'owner.gallery.errors.uploadFailed'));
    } finally {
      setBusy(false);
      e.target.value = '';
    }
  }

  async function onDelete(p: PictureDto) {
    setError(null);
    setBusy(true);
    try {
      await deletePicture(p);
      setTick((n) => n + 1);
    } catch (err) {
      setError(errorMessage(err, 'owner.gallery.errors.deleteFailed'));
    } finally {
      setBusy(false);
    }
  }

  return (
    <article className="card border-0 shadow-sm rounded-4 mb-4 bg-white">
      <div className="card-body p-4">
        <h2 className="h5 fw-semibold mb-3">{t('owner.gallery.title')}</h2>

        {error && <div className="alert alert-danger py-2 small" role="alert">{error}</div>}

        {pictures.length === 0 ? (
          <p className="text-secondary mb-3">{t('owner.gallery.empty')}</p>
        ) : (
          <div className="row g-2 mb-3">
            {pictures.map((p) => (
              <div className="col-6 col-md-4" key={p.links.self}>
                <div className="publish-gallery-item border rounded p-2 position-relative">
                  {p.kind === 'image' ? (
                    <img className="img-fluid rounded publish-gallery-media w-100" src={apiAssetUrl(p.links.self)} alt="" />
                  ) : (
                    <div className="publish-gallery-media w-100 d-flex align-items-center justify-content-center text-secondary bg-body-tertiary rounded">
                      <i className="bi bi-play-circle fs-2" aria-hidden="true" />
                    </div>
                  )}
                  <button
                    type="button"
                    className="btn btn-sm btn-danger position-absolute top-0 end-0 m-1"
                    onClick={() => onDelete(p)}
                    disabled={busy}
                    aria-label={t('owner.gallery.delete')}
                  >
                    <i className="bi bi-trash" aria-hidden="true" />
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}

        <label className="form-label d-block" htmlFor="galleryAdd">{t('owner.gallery.add')}</label>
        <input
          id="galleryAdd"
          type="file"
          className="form-control"
          accept="image/*,video/*"
          multiple
          disabled={busy}
          onChange={onAdd}
        />
      </div>
    </article>
  );
}
