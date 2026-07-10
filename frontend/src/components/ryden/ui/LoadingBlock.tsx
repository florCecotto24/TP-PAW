import { useTranslation } from 'react-i18next';

export type LoadingBlockVariant = 'page' | 'inline' | 'grid';

export interface LoadingBlockProps {
  variant?: LoadingBlockVariant;
  className?: string;
}

/** Spinner / skeleton loading states aligned with Ryden theme. */
export default function LoadingBlock({ variant = 'inline', className = '' }: LoadingBlockProps) {
  const { t } = useTranslation();
  const extra = className.trim();

  if (variant === 'grid') {
    return (
      <div
        className={`ryden-loading-grid${extra ? ` ${extra}` : ''}`}
        role="status"
        aria-live="polite"
      >
        {[0, 1, 2].map((i) => (
          <div key={i} className="ryden-loading-grid__card" aria-hidden="true" />
        ))}
        <span className="visually-hidden">{t('app.loading')}</span>
      </div>
    );
  }

  if (variant === 'page') {
    return (
      <div
        className={`ryden-loading-page${extra ? ` ${extra}` : ''}`}
        role="status"
        aria-live="polite"
      >
        <div className="spinner-border text-primary" aria-hidden="true" />
        <p className="text-secondary mb-0 mt-3">{t('app.loading')}</p>
      </div>
    );
  }

  return (
    <p
      className={`ryden-loading-inline text-secondary mb-0${extra ? ` ${extra}` : ''}`}
      role="status"
    >
      <span className="spinner-border spinner-border-sm text-primary" aria-hidden="true" />
      {t('app.loading')}
    </p>
  );
}
