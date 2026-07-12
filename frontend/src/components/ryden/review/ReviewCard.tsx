import { useTranslation } from 'react-i18next';

import ReviewerAvatar from './ReviewerAvatar';
import ReviewStarsRow from './ReviewStarsRow';
import { starsFromRating } from './starsFromRating';

export interface ReviewCardProps {
  forename: string;
  surname: string;
  dateLabel: string;
  rating: number;
  comment?: string | null;
  /** Foto adjunta a la reseña (no el avatar del autor). */
  imageUrl?: string | null;
  /** Foto de perfil del autor; si falta, se muestran iniciales. */
  avatarUrl?: string | null;
}

/** Espejo de {@code ryden-review:reviewCard}. */
export default function ReviewCard({
  forename,
  surname,
  dateLabel,
  rating,
  comment,
  imageUrl,
  avatarUrl,
}: ReviewCardProps) {
  const { t } = useTranslation();
  const starProps = starsFromRating(rating);

  return (
    <article className="card border-0 shadow-sm rounded-4 listing-review-card">
      <div className="card-body p-4 d-flex flex-column gap-2">
        <div className="d-flex flex-wrap justify-content-between align-items-start gap-2">
          <div className="d-flex align-items-center gap-3 min-w-0 flex-grow-1">
            <ReviewerAvatar forename={forename} surname={surname} avatarUrl={avatarUrl} />
            <div className="min-w-0">
              <p className="fw-semibold mb-0 ryden-text-break">
                {forename} {surname}
              </p>
              <p className="text-secondary small mb-0">{dateLabel}</p>
            </div>
          </div>
          <div className="text-secondary" aria-label={t('reviewCard.ratingAria')}>
            <ReviewStarsRow {...starProps} />
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
