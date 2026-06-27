import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { formatCurrency } from '../../../api/format';
import CarCardImage from '../../../components/CarCardImage';
import { idFromUri } from '../../../api/uri';
import type { CarDto } from '../types';

/** Tarjeta de auto para home/búsqueda: incluye precio diario cuando la API lo proyecta. */
export default function BrowseCarCard({ car }: { car: CarDto }) {
  const { t } = useTranslation();
  const carId = idFromUri(car.links.self);
  const brand = car.brandName?.trim() ?? '';
  const model = car.modelName?.trim() ?? '';
  const fullTitle = `${brand} ${model}`.trim();
  const href = carId ? `/autos/${carId}` : null;
  const price = car.dayPrice;

  return (
    <div className={`carcard${href ? ' carcard--clickable' : ''} position-relative`}>
      <CarCardImage coverUri={car.links.cover}>
        {href ? (
          <span className="carcard-view-chip" aria-hidden="true">
            {t('browse.carCard.view')}
          </span>
        ) : null}
      </CarCardImage>

      <div className="carcard-info">
        <div className="carcard-info-text text">
          <h4 className="carcard-model" title={fullTitle}>
            {model}
          </h4>
          <p className="carcard-brand" title={brand}>
            {brand}
          </p>
          {car.ratingAvg != null ? (
            <p className="carcard-rating small text-secondary mb-0 mt-1">
              <span className="fw-semibold text-dark">{car.ratingAvg.toFixed(1)}</span>{' '}
              <i className="bi bi-star-fill text-warning" aria-hidden="true"></i>
            </p>
          ) : (
            <p className="small text-secondary mb-0 mt-1">{t('browse.carCard.noReviews')}</p>
          )}
          {car.year != null ? (
            <p className="small text-secondary mb-0 mt-1">
              <i className="bi bi-calendar-check" aria-hidden="true"></i> {car.year}
            </p>
          ) : null}
        </div>
        {price != null ? (
          <div className="carcard-price mt-2">
            <div className="carcard-price-row">
              <span className="carcard-price-amount">{formatCurrency(price)}</span>
              <span className="carcard-price-quote text-secondary small">
                / {t('browse.detail.perDay')}
              </span>
            </div>
          </div>
        ) : null}
        {car.minimumRentalDays > 1 ? (
          <p className="small text-secondary mb-0 mt-1">
            {t('browse.carCard.minRentalDays', { count: car.minimumRentalDays })}
          </p>
        ) : null}
      </div>

      {href ? (
        <Link
          to={href}
          className="stretched-link carcard-stretched-link"
          aria-label={t('browse.carCard.viewAria', { car: fullTitle })}
        ></Link>
      ) : null}
    </div>
  );
}
