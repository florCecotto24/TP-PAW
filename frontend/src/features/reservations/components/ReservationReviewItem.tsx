import { useTranslation } from 'react-i18next';
import { ReviewCard } from '../../../components/ryden';
import { apiAssetUrl, profilePictureAssetUrl } from '../../../api/uri';
import { formatDateLong } from '../../../i18n/dateFormat';
import { useUserBrief } from '../../browse/hooks';
import type { ReviewDto } from '../types';

/** Reseña en detalle de reserva — imagen inline como {@code ryden-review:reviewCard}. */
export default function ReservationReviewItem({ review }: { review: ReviewDto }) {
  const { i18n } = useTranslation();
  const authorQuery = useUserBrief(review.links.author);
  const forename = authorQuery.data?.forename ?? '';
  const surname = authorQuery.data?.surname ?? '';
  const imageUrl = review.links.image ? apiAssetUrl(review.links.image) : null;
  const avatarUrl = profilePictureAssetUrl(authorQuery.data?.links);

  return (
    <ReviewCard
      forename={forename}
      surname={surname}
      dateLabel={formatDateLong(review.createdAt, i18n.language)}
      rating={review.rating ?? 0}
      comment={review.comment}
      imageUrl={imageUrl}
      avatarUrl={avatarUrl}
    />
  );
}
