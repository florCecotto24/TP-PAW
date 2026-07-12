import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { fetchUser, fetchUserCars, getUserReviews, userCarsLink, userReviewsLink } from './api';
import { formatDateLong, formatMonthYear } from '../../i18n/dateFormat';
import { Avatar, LoadingBlock, ReviewCard, ReviewStarsRow, starsFromRating } from '../../components/ryden';
import { apiAssetUrl, profilePictureAssetUrl } from '../../api/uri';
import { useUserBrief } from '../browse/hooks';
import type { ReviewDto, CarSummaryDto } from '../browse/types';
import CarCard from './CarCard';
import type { UserDto } from './types';

// =============================================================================
// PublicProfilePage — perfil público de un usuario (/usuarios/:id).
//   GET /users/{id} (el server recorta campos sensibles si no sos vos/admin).
//   Autos activos: navegando user.links.cars (GET /cars?ownerId={id}).
// Markup espejo del counterpartyProfile.jsp (counterparty-* + Bootstrap cards).
// =============================================================================

export default function PublicProfilePage() {
  const { t, i18n } = useTranslation();
  const { id } = useParams<{ id: string }>();
  const userUri = id ? `/users/${id}` : null;

  const userQuery = useQuery({
    queryKey: ['profile', 'public', userUri],
    queryFn: () => fetchUser(userUri as string),
    enabled: !!userUri,
  });

  const reviewsLink = userQuery.data?.links ? userReviewsLink(userQuery.data) : null;
  const reviewsQuery = useQuery({
    queryKey: ['profile', 'public', 'reviews', reviewsLink],
    queryFn: () => getUserReviews(reviewsLink as string),
    enabled: !!reviewsLink,
  });
  const reviews = reviewsQuery.data?.data ?? [];
  const reviewCount = reviewsQuery.data?.total ?? 0;

  if (userQuery.isLoading) {
    return (
      <div className="container counterparty-profile-page pb-4">
        <LoadingBlock variant="page" className="py-4" />
      </div>
    );
  }
  if (userQuery.isError || !userQuery.data) {
    return (
      <div className="container counterparty-profile-page pb-4">
        <div className="alert alert-danger" role="alert">
          {t('profile.common.notFound')}
        </div>
      </div>
    );
  }

  const user = userQuery.data;
  const name = `${user.forename} ${user.surname}`.trim();
  const init = `${(user.forename ?? '').charAt(0)}${(user.surname ?? '').charAt(0)}`.toUpperCase();
  const rating = user.ratingAsOwner ?? user.ratingAsRider ?? null;

  return (
    <div className="container counterparty-profile-page pb-4">
      <div className="row g-4">
        <div className="col-12">
            {/* Header card */}
            <section className="card border-0 shadow-sm rounded-4 counterparty-section-card counterparty-header-card">
              <div className="card-body p-4">
                <div className="d-flex flex-column flex-md-row align-items-start gap-4">
                  <Avatar src={profilePictureAssetUrl(user.links) ?? undefined} alt={name} initials={init} />
                  <div className="flex-grow-1 min-w-0">
                    <div className="d-flex flex-wrap align-items-center gap-2">
                      <h1 className="h4 fw-semibold mb-0 ryden-text-break">{name}</h1>
                    </div>
                    <div className="d-flex align-items-center gap-2 mt-2">
                      {rating != null ? (
                        <>
                          <span className="counterparty-rating-value">{rating.toFixed(1)}</span>
                          <ReviewStarsRow {...starsFromRating(rating)} />
                          {reviewCount > 0 && (
                            <span className="text-secondary small">
                              {t('profile.public.reviewsCount', { count: reviewCount })}
                            </span>
                          )}
                        </>
                      ) : (
                        <span className="counterparty-rating-value">
                          {t('profile.public.noRating')}
                        </span>
                      )}
                    </div>
                    {user.about && user.about.trim() ? (
                      <p className="counterparty-about mt-3 mb-2 ryden-multiline-plaintext">
                        {user.about}
                      </p>
                    ) : (
                      <p className="counterparty-about mt-3 mb-2">{t('profile.public.aboutEmpty')}</p>
                    )}
                    {user.memberSince && (
                      <p className="counterparty-member-since mb-0">
                        <span className="text-secondary">{t('profile.public.memberSince')}</span>{' '}
                        <span className="fw-semibold">
                          {formatMonthYear(user.memberSince, i18n.language)}
                        </span>
                      </p>
                    )}
                  </div>
                </div>
              </div>
            </section>

            {/* Documents card */}
            <section className="counterparty-section-card counterparty-reviews-card card border-0 shadow-sm rounded-4 mt-4">
              <div className="card-body p-4">
                <h2 className="h5 fw-semibold mb-3">{t('profile.public.documents')}</h2>
                <ul className="list-unstyled mb-0">
                  <DocStatus
                    validated={user.licenseValidated}
                    label={t('profile.docs.license')}
                    className="mb-2"
                  />
                  <DocStatus validated={user.identityValidated} label={t('profile.docs.identity')} />
                </ul>
              </div>
            </section>

            {/* Active cars */}
            <UserCars user={user} />

            {/* Reviews received */}
            <UserReviews
              reviews={reviews}
              isLoading={reviewsQuery.isLoading}
              isError={reviewsQuery.isError}
            />
          </div>
        </div>
      </div>
  );
}

function DocStatus({
  validated,
  label,
  className,
}: {
  validated: boolean;
  label: string;
  className?: string;
}) {
  const { t } = useTranslation();
  return (
    <li className={className}>
      {validated ? (
        <i className="bi bi-check-circle-fill text-success" aria-hidden="true"></i>
      ) : (
        <i className="bi bi-x-circle-fill text-danger" aria-hidden="true"></i>
      )}
      <span className="visually-hidden">
        {t(validated ? 'profile.docs.statusValidated' : 'profile.docs.statusNotValidated')}
      </span>
      <span className="fw-semibold ms-1">{label}</span>
    </li>
  );
}

// Réplica de "Ver más" de counterparty-profile.js: sigue el link `next` de
// paginación (Link header) en vez de re-fetchear con un `page` a mano.
function UserCars({ user }: { user: UserDto }) {
  const { t } = useTranslation();
  const carsLink = userCarsLink(user);

  const carsQuery = useQuery({
    queryKey: ['profile', 'public', 'cars', carsLink],
    queryFn: () => fetchUserCars(carsLink as string),
    enabled: !!carsLink,
  });

  const [cars, setCars] = useState<CarSummaryDto[]>([]);
  const [nextLink, setNextLink] = useState<string | null>(null);
  const [loadingMore, setLoadingMore] = useState(false);
  const [loadMoreError, setLoadMoreError] = useState(false);

  useEffect(() => {
    setCars(carsQuery.data?.data ?? []);
    setNextLink(carsQuery.data?.page.next ?? null);
  }, [carsQuery.data]);

  async function onLoadMore() {
    if (!nextLink) return;
    setLoadingMore(true);
    setLoadMoreError(false);
    try {
      const res = await fetchUserCars(nextLink);
      setCars((prev) => [...prev, ...res.data]);
      setNextLink(res.page.next ?? null);
    } catch {
      setLoadMoreError(true);
    } finally {
      setLoadingMore(false);
    }
  }

  return (
    <section className="counterparty-section-card counterparty-reviews-card card border-0 shadow-sm rounded-4 mt-4">
      <div className="card-body p-4">
        <div className="mb-3">
          <h2 className="h5 fw-semibold mb-1">{t('profile.public.activeCars')}</h2>
        </div>

        {carsQuery.isLoading && (
          <LoadingBlock variant="inline" />
        )}
        {carsQuery.isError && (
          <div className="alert alert-danger mb-0" role="alert">
            {t('profile.common.error')}
          </div>
        )}
        {carsQuery.data && cars.length === 0 && (
          <p className="mb-0 text-secondary small">{t('profile.public.noCars')}</p>
        )}
        {cars.length > 0 && (
          <>
            <div className="row row-cols-1 row-cols-md-2 row-cols-lg-3 g-3 gy-4">
              {cars.map((car) => (
                <div key={car.links.self} className="col d-flex justify-content-center">
                  <CarCard car={car} />
                </div>
              ))}
            </div>
            {loadMoreError && (
              <div className="alert alert-danger mt-3 mb-0 py-2" role="alert">
                {t('profile.common.error')}
              </div>
            )}
            {nextLink && (
              <div className="text-center mt-4">
                <button
                  type="button"
                  className="btn btn-outline-primary"
                  onClick={onLoadMore}
                  disabled={loadingMore}
                >
                  {loadingMore ? t('profile.common.loading') : t('profile.public.viewMoreCars')}
                </button>
              </div>
            )}
          </>
        )}
      </div>
    </section>
  );
}

function ProfileReviewItem({ review }: { review: ReviewDto }) {
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

function UserReviews({
  reviews,
  isLoading,
  isError,
}: {
  reviews: ReviewDto[];
  isLoading: boolean;
  isError: boolean;
}) {
  const { t } = useTranslation();

  return (
    <section className="counterparty-section-card counterparty-reviews-card card border-0 shadow-sm rounded-4 mt-4">
      <div className="card-body p-4">
        <h2 className="h5 fw-semibold mb-3">{t('profile.public.reviews')}</h2>

        {isLoading && (
          <LoadingBlock variant="inline" />
        )}
        {isError && (
          <div className="alert alert-danger mb-0" role="alert">
            {t('profile.common.error')}
          </div>
        )}
        {!isLoading && !isError && reviews.length === 0 && (
          <p className="mb-0 text-secondary small">{t('profile.public.noReviews')}</p>
        )}
        {reviews.length > 0 && (
          <ul className="list-unstyled mb-0 d-flex flex-column gap-3">
            {reviews.map((review) => (
              <li key={review.links.self} className="border-bottom pb-3">
                <ProfileReviewItem review={review} />
              </li>
            ))}
          </ul>
        )}
      </div>
    </section>
  );
}
