import { useState } from 'react';
import { useTranslation } from 'react-i18next';

export interface StarRatingInputProps {
  value: number;
  onChange: (value: number) => void;
  id?: string;
}

/** Espejo del widget `.ryden-star-rating` interactivo (5 estrellas clicables). */
export default function StarRatingInput({ value, onChange, id }: StarRatingInputProps) {
  const { t } = useTranslation();
  const [hovered, setHovered] = useState<number | null>(null);
  const active = hovered ?? value;

  return (
    <div
      className="ryden-star-rating"
      role="radiogroup"
      aria-label={t('res.review.rating')}
      onMouseLeave={() => setHovered(null)}
    >
      {[1, 2, 3, 4, 5].map((n) => (
        <span
          key={n}
          role="radio"
          aria-checked={value === n}
          aria-label={String(n)}
          tabIndex={0}
          className={`ryden-star${n <= active ? ' ryden-star--selected' : ''}`}
          onMouseEnter={() => setHovered(n)}
          onClick={() => onChange(n)}
          onKeyDown={(e) => {
            if (e.key === 'Enter' || e.key === ' ') {
              e.preventDefault();
              onChange(n);
            }
          }}
        >
          ★
        </span>
      ))}
      <input type="hidden" id={id} value={value} readOnly />
    </div>
  );
}
