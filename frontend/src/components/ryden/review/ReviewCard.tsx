import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';

const AVATAR_PALETTE = [
  '#6E8DB8',
  '#8E6EB8',
  '#B8896E',
  '#6EB8A8',
  '#8B9E6E',
  '#B86E8E',
  '#B8A86E',
  '#6E9EB8',
];

function hashName(str: string): number {
  let h = 0;
  for (let i = 0; i < str.length; i++) {
    h = (str.charCodeAt(i) + ((h << 5) - h)) | 0;
  }
  return Math.abs(h);
}

function reviewerInitials(forename: string, surname: string): string {
  const f = forename.trim();
  const s = surname.trim();
  const initials = `${f.charAt(0)}${s.charAt(0)}`.toUpperCase();
  return initials || '?';
}

function reviewerAvatarColor(forename: string, surname: string): string {
  return AVATAR_PALETTE[hashName(`${forename.trim()}${surname.trim()}`) % AVATAR_PALETTE.length];
}

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
  const [avatarFailed, setAvatarFailed] = useState(false);
  useEffect(() => setAvatarFailed(false), [avatarUrl]);
  const showPhoto = Boolean(avatarUrl) && !avatarFailed;
  const initials = reviewerInitials(forename, surname);

  return (
    <article className="card border-0 shadow-sm rounded-4 listing-review-card">
      <div className="card-body p-4 d-flex flex-column gap-2">
        <div className="d-flex flex-wrap justify-content-between align-items-start gap-2">
          <div className="d-flex align-items-center gap-3 min-w-0 flex-grow-1">
            {showPhoto ? (
              <img
                src={avatarUrl!}
                alt=""
                className="reviewer-avatar flex-shrink-0"
                onError={() => setAvatarFailed(true)}
              />
            ) : (
              <div
                className="reviewer-avatar flex-shrink-0"
                style={{ backgroundColor: reviewerAvatarColor(forename, surname) }}
                aria-hidden="true"
              >
                {initials}
              </div>
            )}
            <div className="min-w-0">
              <p className="fw-semibold mb-0 ryden-text-break">
                {forename} {surname}
              </p>
              <p className="text-secondary small mb-0">{dateLabel}</p>
            </div>
          </div>
          <div className="d-inline-flex align-items-center gap-1 text-secondary" aria-label={t('reviewCard.ratingAria')}>
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
