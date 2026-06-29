import { useTranslation } from 'react-i18next';

export interface ReviewCardProps {
  forename: string;
  surname: string;
  dateLabel: string;
  rating: number;
  comment?: string | null;
  imageUrl?: string | null;
}

/** Espejo de {@code ryden-review:reviewCard}. */
export default function ReviewCard({
  forename,
  surname,
  dateLabel,
  rating,
  comment,
  imageUrl,
}: ReviewCardProps) {
  const { t } = useTranslation();

  return (
    <article className="card border-0 shadow-sm rounded-4 listing-review-card">
      <div className="card-body p-4 d-flex flex-column gap-2">
        <div className="d-flex flex-wrap justify-content-between align-items-start gap-2">
          <div className="d-flex align-items-center gap-3 min-w-0 flex-grow-1">
            <div
              className="reviewer-avatar flex-shrink-0"
              data-forename={forename}
              data-surname={surname}
              aria-hidden="true"
            />
            <div className="min-w-0">
              <p className="fw-semibold mb-0 ryden-text-break">
                {forename} {surname}
              </p>
              <p className="text-secondary small mb-0">{dateLabel}</p>
            </div>
          </div>
          <div className="d-inline-flex align-items-center gap-1 text-secondary" aria-label="Rating">
            {Array.from({ length: 5 }, (_, i) => (
              <i
                key={i}
                className={`bi bi-star${i < rating ? '-fill text-warning' : ' text-secondary-subtle'}`}
                aria-hidden="true"
              ></i>
            ))}
          </div>
        </div>
        {comment ? (
          <p className="mb-0 small ryden-multiline-plaintext">{comment}</p>
        ) : (
          <p className="mb-0 small text-secondary">{t('reviewCard.noComment')}</p>
        )}
        {imageUrl ? (
          <a href={imageUrl} target="_blank" rel="noopener noreferrer" className="d-block mt-2">
            <img
              src={imageUrl}
              alt={t('reviewCard.image.alt')}
              className="img-fluid rounded-3 listing-review-card-image"
              loading="lazy"
            />
          </a>
        ) : null}
      </div>
    </article>
  );
}
