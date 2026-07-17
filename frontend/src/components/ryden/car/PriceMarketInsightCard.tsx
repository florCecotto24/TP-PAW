import {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
  type KeyboardEvent,
  type PointerEvent as ReactPointerEvent,
  type ReactNode,
} from 'react';
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
  /** Precio actual del input controlado (mismo valor que el campo hijo). */
  value?: number | null;
  /** Cuando el usuario arrastra o clickea un bound, notifica el nuevo precio. */
  onPriceChange?: (price: number) => void;
  /** @deprecated Preferir {@link value}; se usa solo si value no viene. */
  initialUserPrice?: number | null;
  showDefaultPriceHint?: boolean;
  currencyCode?: string;
  children?: ReactNode;
}

type Zone = 'red' | 'yellow' | 'green' | null;

function hasMarketSpread(min: number, max: number): boolean {
  return Number.isFinite(min) && Number.isFinite(max) && max > min;
}

function clampPrice(price: number): number {
  return Math.round(Math.max(0, Math.min(99_999_999.99, price)) * 100) / 100;
}

function pctOnBar(value: number, min: number, max: number): number {
  const span = max - min;
  if (!(span > 0)) return 50;
  return Math.min(100, Math.max(0, ((value - min) / span) * 100));
}

function valueFromPct(pct: number, min: number, max: number): number {
  return clampPrice(min + (max - min) * (pct / 100));
}

function zoneLayout(min: number, max: number, avg: number) {
  const avgPct = pctOnBar(Number.isFinite(avg) ? avg : min, min, max);
  const fadeWidth = Math.min(22, Math.max(10, 18));
  const greenHalf = Math.max(4, fadeWidth * 0.35);
  return {
    avgPct,
    fadeWidth,
    redOuterLeft: avgPct - fadeWidth,
    redOuterRight: avgPct + fadeWidth,
    greenLeft: avgPct - greenHalf,
    greenRight: avgPct + greenHalf,
  };
}

function zoneForPct(
  userPct: number,
  layout: ReturnType<typeof zoneLayout>,
): Zone {
  if (userPct <= layout.redOuterLeft || userPct >= layout.redOuterRight) return 'red';
  if (userPct >= layout.greenLeft && userPct <= layout.greenRight) return 'green';
  return 'yellow';
}

function formatMoneyLabel(amount: number, currencyCode: string): string {
  try {
    return new Intl.NumberFormat(undefined, {
      style: 'currency',
      currency: currencyCode,
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(amount);
  } catch {
    return formatCurrency(amount);
  }
}

/**
 * Espejo de {@code ryden-car:priceMarketInsightCard} + la lógica de
 * {@code price-market-insight.js}, implementada en React para que el drag
 * no pelee con re-renders del input controlado.
 */
export default function PriceMarketInsightCard({
  insight,
  priceInputId = 'pricePerDay',
  value,
  onPriceChange,
  initialUserPrice,
  children,
  currencyCode = 'ARS',
}: PriceMarketInsightCardProps) {
  const { t } = useTranslation();
  const trackRef = useRef<HTMLDivElement>(null);
  const draggingRef = useRef(false);
  const [dragging, setDragging] = useState(false);

  const userPrice = value ?? initialUserPrice ?? null;
  const showPicker =
    insight != null && hasMarketSpread(insight.minPrice, insight.maxPrice);

  const layout = useMemo(() => {
    if (!showPicker || !insight) return null;
    return zoneLayout(insight.minPrice, insight.maxPrice, insight.averagePrice);
  }, [insight, showPicker]);

  const userPct =
    showPicker && insight && userPrice != null && userPrice > 0
      ? pctOnBar(userPrice, insight.minPrice, insight.maxPrice)
      : null;

  const zone: Zone =
    layout && userPct != null ? zoneForPct(userPct, layout) : null;

  const minFmt = insight ? formatCurrency(insight.minPrice) : '';
  const maxFmt = insight ? formatCurrency(insight.maxPrice) : '';
  const avgFmt = insight ? formatCurrency(insight.averagePrice) : '';
  const userLabel =
    userPrice != null && userPrice > 0
      ? formatMoneyLabel(userPrice, currencyCode)
      : '';

  const setPriceFromClientX = useCallback(
    (clientX: number) => {
      if (!insight || !onPriceChange || !trackRef.current) return;
      const rect = trackRef.current.getBoundingClientRect();
      if (rect.width <= 0) return;
      const pct = Math.min(100, Math.max(0, ((clientX - rect.left) / rect.width) * 100));
      onPriceChange(valueFromPct(pct, insight.minPrice, insight.maxPrice));
    },
    [insight, onPriceChange],
  );

  const endDrag = useCallback(() => {
    if (!draggingRef.current) return;
    draggingRef.current = false;
    setDragging(false);
    document.body.classList.remove('ryden-price-insight--drag-active');
  }, []);

  useEffect(() => {
    if (!dragging) return undefined;
    const onMove = (e: PointerEvent) => {
      e.preventDefault();
      setPriceFromClientX(e.clientX);
    };
    const onEnd = () => endDrag();
    window.addEventListener('pointermove', onMove);
    window.addEventListener('pointerup', onEnd);
    window.addEventListener('pointercancel', onEnd);
    return () => {
      window.removeEventListener('pointermove', onMove);
      window.removeEventListener('pointerup', onEnd);
      window.removeEventListener('pointercancel', onEnd);
    };
  }, [dragging, setPriceFromClientX, endDrag]);

  const startDrag = (e: ReactPointerEvent) => {
    if (!insight || !onPriceChange) return;
    if (e.button !== undefined && e.button !== 0) return;
    e.preventDefault();
    draggingRef.current = true;
    setDragging(true);
    document.body.classList.add('ryden-price-insight--drag-active');
    trackRef.current?.setPointerCapture?.(e.pointerId);
    setPriceFromClientX(e.clientX);
  };

  const applyBound = (price: number) => {
    if (!onPriceChange || !insight) return;
    onPriceChange(
      clampPrice(Math.max(insight.minPrice, Math.min(insight.maxPrice, price))),
    );
  };

  const onBoundKeyDown = (price: number) => (e: KeyboardEvent) => {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      applyBound(price);
    }
  };

  if (!showPicker) {
    return (
      <div>
        <label htmlFor={priceInputId} className="form-label required-label mb-2">
          {t('publishCar.form.pricePerDay')}
        </label>
        {children}
        {insight ? (
          <div className="text-success fw-semibold small mt-2">
            {t('car.price.insight.reference')}
            <div className="text-body fw-normal">{avgFmt}</div>
          </div>
        ) : (
          <small className="text-muted d-block mt-2">{t('car.price.insight.noMarket')}</small>
        )}
      </div>
    );
  }

  const zoneClass =
    zone === 'red'
      ? ' ryden-price-insight--zone-red'
      : zone === 'yellow'
        ? ' ryden-price-insight--zone-yellow'
        : zone === 'green'
          ? ' ryden-price-insight--zone-green'
          : '';

  return (
    <div className={`ryden-price-insight card border-0 shadow-sm rounded-4 mb-0${zoneClass}`}>
      <div className="card-body p-3 p-md-4">
        <label htmlFor={priceInputId} className="form-label required-label mb-2">
          {t('publishCar.form.pricePerDay')}
        </label>
        <div className="ryden-price-insight__price-field mb-2">{children}</div>
        {layout ? (
          <>
            <div className="ryden-price-insight__bar-wrap">
              <div
                ref={trackRef}
                className="ryden-price-insight__bar-track"
                role="presentation"
                style={{
                  ['--avg-pct' as string]: `${layout.avgPct}%`,
                  ['--fade-width' as string]: `${layout.fadeWidth}%`,
                }}
              >
                <div
                  className="ryden-price-insight__bar-hit"
                  aria-hidden="true"
                  onPointerDown={startDrag}
                />
                <div className="ryden-price-insight__bar-gradient" />
                <div
                  className="ryden-price-insight__marker ryden-price-insight__marker--min ryden-price-insight__marker--bound"
                  role="button"
                  tabIndex={0}
                  title={minFmt}
                  aria-label={minFmt}
                  style={{ left: '0%' }}
                  onClick={() => applyBound(insight.minPrice)}
                  onKeyDown={onBoundKeyDown(insight.minPrice)}
                >
                  <span className="ryden-price-insight__marker-dot" />
                </div>
                <div
                  className="ryden-price-insight__marker ryden-price-insight__marker--avg"
                  title={avgFmt}
                  style={{ left: `${layout.avgPct}%` }}
                >
                  <span className="ryden-price-insight__marker-dot" />
                </div>
                <div
                  className="ryden-price-insight__marker ryden-price-insight__marker--max ryden-price-insight__marker--bound"
                  role="button"
                  tabIndex={0}
                  title={maxFmt}
                  aria-label={maxFmt}
                  style={{ left: '100%' }}
                  onClick={() => applyBound(insight.maxPrice)}
                  onKeyDown={onBoundKeyDown(insight.maxPrice)}
                >
                  <span className="ryden-price-insight__marker-dot" />
                </div>
                {userPct != null ? (
                  <div
                    className={`ryden-price-insight__marker ryden-price-insight__marker--user${
                      dragging ? ' ryden-price-insight__marker--dragging' : ' ryden-price-insight__marker--animate'
                    }`}
                    role="slider"
                    tabIndex={0}
                    aria-valuemin={insight.minPrice}
                    aria-valuemax={insight.maxPrice}
                    aria-valuenow={userPrice ?? 0}
                    title={userLabel}
                    aria-label={userLabel}
                    style={{ left: `${userPct}%` }}
                    onPointerDown={startDrag}
                  >
                    <span className="ryden-price-insight__marker-dot" />
                    <span className="ryden-price-insight__marker-label">{userLabel}</span>
                  </div>
                ) : null}
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
