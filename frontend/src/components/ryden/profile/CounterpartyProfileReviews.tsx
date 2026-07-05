import { useTranslation } from 'react-i18next';
import { formatDateLong } from '../../../i18n/dateFormat';

export interface CounterpartyReviewItem {
  reviewerName: string;
  rating: number;
  reviewDate: string;
  commentText?: string | null;
}

export interface CounterpartyProfileReviewsProps {
  reviews?: CounterpartyReviewItem[];
}

/** Espejo de {@code ryden:counterpartyProfileReviews}. */
export default function CounterpartyProfileReviews({ reviews = [] }: CounterpartyProfileReviewsProps) {
  const { t, i18n } = useTranslation();

  return (
    <section className="counterparty-section-card counterparty-reviews-card card border-0 shadow-sm rounded-4">
      <div className="card-body p-4">
        <div className="mb-3">
          <h2 className="h5 fw-semibold mb-1">{t('counterpartyProfile.reviews.recentHeading')}</h2>
        </div>
        {reviews.length > 0 ? (
          <ul className="list-group list-group-flush">
            {reviews.map((review, index) => (
              <li key={index} className="list-group-item px-0 py-3 bg-transparent">
                <div className="d-flex flex-column gap-2">
                  <div className="d-flex align-items-center justify-content-between gap-2">
                    <div className="min-w-0 flex-grow-1">
                      <p className="fw-semibold mb-1 ryden-text-break">{review.reviewerName}</p>
                      <div className="d-inline-flex align-items-center gap-1">
                        {Array.from({ length: 5 }, (_, i) => (
                          <i
                            key={i}
                            className={`bi bi-star${i < review.rating ? '-fill text-warning' : ' text-secondary-subtle'}`}
                            aria-hidden="true"
                            style={{ fontSize: '0.75rem' }}
                          ></i>
                        ))}
                      </div>
                    </div>
                    <span className="text-secondary small">{formatDateLong(review.reviewDate, i18n.language)}</span>
                  </div>
                  {review.commentText ? (
                    <p className="mb-0 text-secondary ryden-multiline-plaintext">{review.commentText}</p>
                  ) : (
                    <p className="mb-0 small text-secondary">{t('reviewCard.noComment')}</p>
                  )}
                </div>
              </li>
            ))}
          </ul>
        ) : (
          <p className="mb-0 text-secondary small">{t('counterpartyProfile.reviews.empty')}</p>
        )}
      </div>
    </section>
  );
}
