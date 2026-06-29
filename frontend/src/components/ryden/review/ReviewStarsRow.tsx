import { useTranslation } from 'react-i18next';

export interface ReviewStarsRowProps {
  fullStars: number;
  halfStar?: boolean;
}

/** Espejo de {@code ryden-review:reviewStarsRow}. */
export default function ReviewStarsRow({ fullStars, halfStar = false }: ReviewStarsRowProps) {
  const { t } = useTranslation();
  const extraHalf = halfStar ? 1 : 0;
  const emptyCount = 5 - fullStars - extraHalf;

  return (
    <div
      className="reviewStarsRow d-inline-flex align-items-center gap-0"
      role="img"
      aria-label={t('reviewStarsRow.ratingAriaLabel', { rating: fullStars })}
    >
      {Array.from({ length: fullStars }, (_, i) => (
        <i key={`f-${i}`} className="bi bi-star-fill text-primary" aria-hidden="true"></i>
      ))}
      {halfStar ? <i className="bi bi-star-half text-primary" aria-hidden="true"></i> : null}
      {emptyCount > 0
        ? Array.from({ length: emptyCount }, (_, i) => (
            <i key={`e-${i}`} className="bi bi-star text-primary" aria-hidden="true"></i>
          ))
        : null}
    </div>
  );
}
