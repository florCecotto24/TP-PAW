import type { ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { formatCurrency } from '../../../api/format';

export interface PriceMarketInsight {
  minPrice: number;
  maxPrice: number;
  averagePrice: number;
}

export interface PriceMarketInsightCardProps {
  insight?: PriceMarketInsight | null;
  priceInputId?: string;
  initialUserPrice?: number | null;
  showDefaultPriceHint?: boolean;
  currencyCode?: string;
  children?: ReactNode;
}

/**
 * Espejo de {@code ryden-car:priceMarketInsightCard}: campo de precio + barra de mercado.
 * La interactividad del marcador de usuario se porta desde {@code price-market-insight.js}
 * vía atributos {@code data-ryden-price-insight}.
 */
export default function PriceMarketInsightCard({
  insight,
  priceInputId = 'pricePerDay',
  initialUserPrice,
  children,
  currencyCode = 'ARS',
}: PriceMarketInsightCardProps) {
  const { t } = useTranslation();

  const dataAttrs = insight
    ? {
        'data-ryden-price-insight': true,
        'data-min': insight.minPrice,
        'data-max': insight.maxPrice,
        'data-avg': insight.averagePrice,
        'data-currency': currencyCode,
      }
    : {};

  const minFmt = insight ? formatCurrency(insight.minPrice) : '';
  const maxFmt = insight ? formatCurrency(insight.maxPrice) : '';
  const avgFmt = insight ? formatCurrency(insight.averagePrice) : '';

  return (
    <div
      className="ryden-price-insight card border-0 shadow-sm rounded-4 mb-0"
      data-price-input-id={priceInputId}
      {...(initialUserPrice != null ? { 'data-initial-user-price': initialUserPrice } : {})}
      {...dataAttrs}
    >
      <div className="card-body p-3 p-md-4">
        <label htmlFor={priceInputId} className="form-label required-label mb-2">
          {t('publishCar.form.pricePerDay')}
        </label>
        <div className="ryden-price-insight__price-field mb-2">{children}</div>
        {insight ? (
          <>
            <div className="ryden-price-insight__bar-wrap">
              <div className="ryden-price-insight__bar-track" role="presentation">
                <div className="ryden-price-insight__bar-hit" aria-hidden="true" />
                <div className="ryden-price-insight__bar-gradient" />
                <div
                  className="ryden-price-insight__marker ryden-price-insight__marker--min ryden-price-insight__marker--bound"
                  role="button"
                  tabIndex={0}
                  title={minFmt}
                  aria-label={minFmt}
                >
                  <span className="ryden-price-insight__marker-dot" />
                </div>
                <div
                  className="ryden-price-insight__marker ryden-price-insight__marker--avg"
                  title={avgFmt}
                >
                  <span className="ryden-price-insight__marker-dot" />
                </div>
                <div
                  className="ryden-price-insight__marker ryden-price-insight__marker--max ryden-price-insight__marker--bound"
                  role="button"
                  tabIndex={0}
                  title={maxFmt}
                  aria-label={maxFmt}
                >
                  <span className="ryden-price-insight__marker-dot" />
                </div>
                <div
                  className="ryden-price-insight__marker ryden-price-insight__marker--user ryden-price-insight__marker--animate d-none"
                  role="slider"
                  tabIndex={0}
                  aria-valuemin={0}
                  aria-valuemax={0}
                  aria-valuenow={0}
                  title=""
                >
                  <span className="ryden-price-insight__marker-dot" />
                  <span className="ryden-price-insight__marker-label" />
                </div>
              </div>
            </div>
            <div className="d-flex justify-content-between align-items-start gap-2 mt-2 small">
              <div className="text-danger fw-semibold">
                {t('car.price.insight.min')}
                <div className="text-body fw-normal">{minFmt}</div>
              </div>
              <div className="text-success fw-semibold text-center">
                {t('car.price.insight.avg')}
                <div className="text-body fw-normal">{avgFmt}</div>
              </div>
              <div className="text-danger fw-semibold text-end">
                {t('car.price.insight.max')}
                <div className="text-body fw-normal">{maxFmt}</div>
              </div>
            </div>
          </>
        ) : null}
      </div>
    </div>
  );
}
