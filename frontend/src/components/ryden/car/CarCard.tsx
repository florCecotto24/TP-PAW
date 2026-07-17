import type { ReactNode } from 'react';
import { Link, type To } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { formatCurrency } from '../../../api/format';
import { isAppLinkTarget, type AppLinkTarget } from '../../../routes/navigationState';

export type PriceMarketPosition = 'below_market' | 'at_market' | 'above_market';

export interface CarCardProps {
  model: string;
  brand: string;
  price: number;
  image?: string | null;
  pricePeriod?: 'hour' | 'day';
  href?: To | AppLinkTarget | null;
  year?: number | null;
  ratingAvg?: number | null;
  reviewCount?: number | null;
  priceMarketPositionModifier?: PriceMarketPosition | null;
  marketAveragePrice?: number | null;
  marketSampleCount?: number | null;
  minimumRentalDays?: number | null;
  carId?: number | null;
  showFavoriteButton?: boolean;
  favorited?: boolean;
  onToggleFavorite?: (carId: number) => void;
  imageSlot?: ReactNode;
  overlay?: ReactNode;
}

/**
 * Espejo de {@code ryden-car:carCard}: tarjeta de vehículo con imagen, rating, precio
 * y badge de mercado opcional. {@code imageSlot} permite inyectar {@link CarCardImage}.
 */
export default function CarCard({
  model,
  brand,
  price,
  image,
  pricePeriod = 'hour',
  href,
  year,
  ratingAvg,
  reviewCount,
  priceMarketPositionModifier,
  marketAveragePrice,
  marketSampleCount,
  minimumRentalDays,
  carId,
  showFavoriteButton = false,
  favorited = false,
  onToggleFavorite,
  imageSlot,
  overlay,
}: CarCardProps) {
  const { t } = useTranslation();
  const trimmedModel = collapseCardLabel(model);
  const trimmedBrand = collapseCardLabel(brand);
  const yearLabel = year != null && Number.isFinite(year) ? String(year) : null;
  const modelWithYear = yearLabel ? `${trimmedModel} (${yearLabel})` : trimmedModel;
  const modelTitleNode = yearLabel ? (
    <>
      {trimmedModel}{' '}
      <span className="text-secondary fw-normal">({yearLabel})</span>
    </>
  ) : (
    trimmedModel
  );
  const hasFavoriteButton = showFavoriteButton && carId != null;
  const pricePeriodLabel = pricePeriod === 'day' ? t('common.day') : t('common.hour');

  const marketBadgeKey = priceMarketPositionModifier
    ? ({
        below_market: 'carCard.priceMarket.below',
        at_market: 'carCard.priceMarket.at',
        above_market: 'carCard.priceMarket.above',
      }[priceMarketPositionModifier] ?? null)
    : null;

  return (
    <div
      className={`carcard${href ? ' carcard--clickable' : ''}${href || hasFavoriteButton ? ' position-relative' : ''}`}
    >
      <div className="carcard-image">
        {imageSlot ??
          (image ? (
            <img src={image} alt={`${trimmedBrand} ${modelWithYear}`} />
          ) : (
            <div className="no-image-badge">
              <i className="bi bi-car-front"></i>
            </div>
          ))}
        {href ? (
          <span className="carcard-view-chip" aria-hidden="true">
            {t('carCard.viewChip')}
          </span>
        ) : null}
      </div>

      {hasFavoriteButton ? (
        <form
          className="carcard-favorite-form"
          onSubmit={(e) => {
            e.preventDefault();
            onToggleFavorite?.(carId!);
          }}
        >
          <button
            type="submit"
            className={`carcard-favorite-btn${favorited ? ' carcard-favorite-btn--on' : ''}`}
            aria-label={t(favorited ? 'carCard.favorite.remove' : 'carCard.favorite.add')}
            title={t(favorited ? 'carCard.favorite.remove' : 'carCard.favorite.add')}
          >
            <i className={`bi bi-heart${favorited ? '-fill' : ''}`} aria-hidden="true"></i>
          </button>
        </form>
      ) : null}
      {overlay}

      <div className="carcard-info">
        <div className="carcard-info-text text">
          <h4 className="carcard-model" title={`${trimmedBrand} ${modelWithYear}`}>
            {modelTitleNode}
          </h4>
          <p className="carcard-brand" title={trimmedBrand}>
            {trimmedBrand}
          </p>
          {ratingAvg != null && reviewCount != null && reviewCount > 0 ? (
            <p className="carcard-rating small text-secondary mb-0 mt-1">
              <span className="fw-semibold text-dark">{ratingAvg.toFixed(1)}</span>
              <i className="bi bi-star-fill text-warning" aria-hidden="true"></i>
              <span className="text-secondary">
                {' '}
                ({reviewCount}{' '}
                {t(reviewCount === 1 ? 'carCard.review' : 'carCard.reviews')})
              </span>
            </p>
          ) : ratingAvg != null && (reviewCount == null || reviewCount === 0) ? (
            <p className="carcard-rating small text-secondary mb-0 mt-1">
              <span className="fw-semibold text-dark">{ratingAvg.toFixed(1)}</span>
              <i className="bi bi-star-fill text-warning" aria-hidden="true"></i>
            </p>
          ) : (reviewCount == null || reviewCount === 0) && ratingAvg == null ? (
            <p className="carcard-rating small text-secondary mb-0 mt-1">{t('carCard.noReviews')}</p>
          ) : null}
          {minimumRentalDays != null && minimumRentalDays > 1 ? (
            <p className="small text-secondary mb-0 mt-1">
              <i className="bi bi-calendar-check" aria-hidden="true"></i>{' '}
              {t('search.card.minRentalDays', { count: minimumRentalDays })}
            </p>
          ) : null}
        </div>
        <div className="carcard-price text">
          <div className="carcard-price-row">
            <div className="carcard-price-quote">
              <p className="carcard-price-from">{t('carCard.priceFrom')}</p>
              <p className="carcard-price-amount">{formatCurrency(price)}</p>
              <p>/{pricePeriodLabel}</p>
            </div>
            {priceMarketPositionModifier && marketBadgeKey ? (
              <span
                className={`carcard-price-market-badge carcard-price-market-badge--${priceMarketPositionModifier}`}
                title={t('carCard.priceMarket.tooltip', {
                  avg: formatCurrency(marketAveragePrice ?? 0),
                  count: marketSampleCount ?? 0,
                })}
              >
                {t(marketBadgeKey)}
              </span>
            ) : null}
          </div>
        </div>
      </div>

      {href ? (
        <Link
          to={isAppLinkTarget(href) ? href.pathname : href}
          state={isAppLinkTarget(href) ? href.state : undefined}
          className="stretched-link carcard-stretched-link"
          aria-label={t('carCard.viewAriaLabel', { brand: trimmedBrand, model: modelWithYear })}
        />
      ) : null}
    </div>
  );
}

/** Collapses internal whitespace/newlines from catalog free-text (custom brand/model). */
function collapseCardLabel(value: string): string {
  return value.replace(/\s+/g, ' ').trim();
}
