import ReviewCard, { type ReviewCardProps } from './ReviewCard';
import { chunk } from '../utils/chunk';

export interface ReviewCarouselItem extends Omit<ReviewCardProps, 'forename' | 'surname'> {
  reviewerForename: string;
  reviewerSurname: string;
  dateText: string;
  imageId?: number | null;
  imageUrl?: string | null;
}

export interface ReviewCarouselProps {
  reviews: ReviewCarouselItem[];
  id: string;
  resolveImageUrl?: (imageId: number) => string;
  itemsPerSlide?: number;
}

/** Espejo de {@code ryden-review:reviewCarousel}: 2 reseñas por slide en md+. */
export default function ReviewCarousel({
  reviews,
  id,
  resolveImageUrl,
  itemsPerSlide = 2,
}: ReviewCarouselProps) {
  if (reviews.length === 0) return null;

  const slides = chunk(reviews, itemsPerSlide);

  return (
    <div id={id} className="carousel slide ryden-review-carousel" data-bs-ride="false">
      <div className="carousel-inner">
        {slides.map((slideReviews, slideIndex) => (
          <div key={slideIndex} className={`carousel-item${slideIndex === 0 ? ' active' : ''}`}>
            <div className="row row-cols-1 row-cols-md-2 g-3 pb-2 align-items-stretch">
              {slideReviews.map((review, i) => {
                const imageUrl =
                  review.imageUrl ??
                  (review.imageId && resolveImageUrl ? resolveImageUrl(review.imageId) : null);
                return (
                  <div key={`${slideIndex}-${i}`} className="col d-flex">
                    <ReviewCard
                      forename={review.reviewerForename}
                      surname={review.reviewerSurname}
                      dateLabel={review.dateText}
                      rating={review.rating}
                      comment={review.comment}
                      imageUrl={imageUrl}
                      avatarUrl={review.avatarUrl}
                    />
                  </div>
                );
              })}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
