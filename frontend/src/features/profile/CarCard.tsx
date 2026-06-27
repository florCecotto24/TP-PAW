import type { ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import CarCardImage from '../../components/CarCardImage';
import { idFromUri } from './hooks';
import type { CarDto } from './types';

// Tarjeta de auto reutilizada en favoritos y perfil público. Replica el markup
// del `carcard.tag` original (Bootstrap + clases del tema). El link al detalle
// usa la ruta SPA en español (/autos/:id); el id sale del self del CarDto
// (URN canónica "/cars/{id}"), no se arma a mano.
export default function CarCard({
  car,
  action,
}: {
  car: CarDto;
  /** Slot opcional para acciones flotantes (p.ej. quitar de favoritos). */
  action?: ReactNode;
}) {
  const { t } = useTranslation();
  const carId = idFromUri(car.links.self);
  const brand = car.brandName?.trim() ?? '';
  const model = car.modelName?.trim() ?? '';
  const fullTitle = `${brand} ${model}`.trim();
  const href = carId ? `/autos/${carId}` : null;

  return (
    <div className={`carcard${href ? ' carcard--clickable' : ''} position-relative`}>
      <CarCardImage coverUri={car.links.cover}>
        {href && (
          <span className="carcard-view-chip" aria-hidden="true">
            {t('profile.public.viewCar')}
          </span>
        )}
      </CarCardImage>

      {action}

      <div className="carcard-info">
        <div className="carcard-info-text text">
          <h4 className="carcard-model" title={fullTitle}>
            {model}
          </h4>
          <p className="carcard-brand" title={brand}>
            {brand}
          </p>
          {car.ratingAvg != null && (
            <p className="carcard-rating small text-secondary mb-0 mt-1">
              <span className="fw-semibold text-dark">{car.ratingAvg.toFixed(1)}</span>{' '}
              <i className="bi bi-star-fill text-warning" aria-hidden="true"></i>
            </p>
          )}
          {car.year != null && (
            <p className="small text-secondary mb-0 mt-1">
              <i className="bi bi-calendar-check" aria-hidden="true"></i> {car.year}
            </p>
          )}
        </div>
      </div>

      {href && (
        <Link
          to={href}
          className="stretched-link carcard-stretched-link"
          aria-label={t('profile.public.viewCar')}
        ></Link>
      )}
    </div>
  );
}
