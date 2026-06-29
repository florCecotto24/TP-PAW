import { useTranslation } from 'react-i18next';

export interface DetailListingMetaProps {
  verified?: boolean;
  rating?: string;
  reviewCount?: number | string;
}

/** Espejo de {@code ryden-car:detailListingMeta}. */
export default function DetailListingMeta({ rating, reviewCount }: DetailListingMetaProps) {
  const { t } = useTranslation();

  if (!reviewCount) return null;

  return (
    <div className="d-flex flex-wrap align-items-center gap-3 detail-listing-meta mt-2">
      <span className="d-inline-flex align-items-center gap-1 text-secondary">
        {rating ? (
          <>
            <span className="text-dark fw-semibold">{rating}</span>
            <i className="bi bi-star-fill text-warning" aria-hidden="true"></i>
          </>
        ) : null}
        <span className="text-muted">
          {t('detailCarMeta.reviewsCount', {
            count: typeof reviewCount === 'number' ? reviewCount : Number(reviewCount) || 0,
          })}
        </span>
      </span>
    </div>
  );
}
