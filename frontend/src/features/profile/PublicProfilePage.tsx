import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { fetchUser, fetchUserCars, getUserReviews, userCarsLink, userReviewsLink } from './api';
import { formatDateLong } from '../../i18n/dateFormat';
import CarCard from './CarCard';
import type { CarDto, UserDto, UserReviewDto } from './types';

// =============================================================================
// PublicProfilePage — perfil público de un usuario (/usuarios/:id).
//   GET /users/{id} (el server recorta campos sensibles si no sos vos/admin).
//   Autos activos: navegando user.links.cars (GET /cars?ownerId={id}).
// Markup espejo del counterpartyProfile.jsp (counterparty-* + Bootstrap cards).
// =============================================================================

function StarRow({ rating }: { rating: number }) {
  const floor = Math.floor(rating);
  const frac = rating - floor;
  return (
    <div className="d-inline-flex align-items-center gap-1">
      {[1, 2, 3, 4, 5].map((star) => {
        if (star <= floor) {
          return <i key={star} className="bi bi-star-fill text-warning" aria-hidden="true"></i>;
        }
        if (star === floor + 1 && frac >= 0.4 && frac <= 0.6) {
          return <i key={star} className="bi bi-star-half text-warning" aria-hidden="true"></i>;
        }
        return <i key={star} className="bi bi-star text-secondary-subtle" aria-hidden="true"></i>;
      })}
    </div>
  );
}

// El link profilePicture viene SOLO si hay foto; si está ausente (src undefined)
// mostramos el placeholder de iniciales. El onError queda como red de seguridad.
function Avatar({ src, alt, initials }: { src?: string; alt: string; initials: string }) {
  const [failed, setFailed] = useState(false);
  return (
    <div className="counterparty-avatar">
      {src && !failed ? (
        <img
          src={src}
          alt={alt}
          className="counterparty-avatar__img"
          onError={() => setFailed(true)}
        />
      ) : (
        <div className="counterparty-avatar__placeholder">
          <span>{initials}</span>
        </div>
      )}
    </div>
  );
}

export default function PublicProfilePage() {
  const { t } = useTranslation();
  const { id } = useParams<{ id: string }>();
  const userUri = id ? `/users/${id}` : null;

  const userQuery = useQuery({
    queryKey: ['profile', 'public', userUri],
    queryFn: () => fetchUser(userUri as string),
    enabled: !!userUri,
  });

  const reviewsLink = userQuery.data ? userReviewsLink(userQuery.data) : null;
  const reviewsQuery = useQuery({
    queryKey: ['profile', 'public', 'reviews', reviewsLink],
    queryFn: () => getUserReviews(reviewsLink as string),
    enabled: !!reviewsLink,
  });
  const reviews = reviewsQuery.data?.data ?? [];
  const reviewCount = reviewsQuery.data?.total ?? 0;

  if (userQuery.isLoading) {
    return (
      <main className="counterparty-profile-page">
        <div className="counterparty-profile-container">
          <p role="status" className="text-secondary">
            {t('profile.common.loading')}
          </p>
        </div>
      </main>
    );
  }
  if (userQuery.isError || !userQuery.data) {
    return (
      <main className="counterparty-profile-page">
        <div className="counterparty-profile-container">
          <div className="alert alert-danger" role="alert">
            {t('profile.common.notFound')}
          </div>
        </div>
      </main>
    );
  }

  const user = userQuery.data;
  const name = `${user.forename} ${user.surname}`.trim();
  const init = `${(user.forename ?? '').charAt(0)}${(user.surname ?? '').charAt(0)}`.toUpperCase();
  const rating = user.ratingAsOwner ?? user.ratingAsRider ?? null;

  return (
    <main className="counterparty-profile-page">
      <div className="counterparty-profile-container">
        <div className="row g-4">
          <div className="col-12">
            {/* Header card */}
            <section className="card border-0 shadow-sm rounded-4 counterparty-section-card counterparty-header-card">
              <div className="card-body p-4">
                <div className="d-flex flex-column flex-md-row align-items-start gap-4">
                  <Avatar src={user.links.profilePicture} alt={name} initials={init} />
                  <div className="flex-grow-1 min-w-0">
                    <div className="d-flex flex-wrap align-items-center gap-2">
                      <h1 className="h4 fw-semibold mb-0 ryden-text-break">{name}</h1>
                    </div>
                    <div className="d-flex align-items-center gap-2 mt-2">
                      {rating != null ? (
                        <>
                          <span className="counterparty-rating-value">{rating.toFixed(1)}</span>
                          <StarRow rating={rating} />
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
                        <span className="fw-semibold">{user.memberSince}</span>
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
    </main>
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

  const [cars, setCars] = useState<CarDto[]>([]);
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
          <p role="status" className="text-secondary small mb-0">
            {t('profile.common.loading')}
          </p>
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

function UserReviews({
  reviews,
  isLoading,
  isError,
}: {
  reviews: UserReviewDto[];
  isLoading: boolean;
  isError: boolean;
}) {
  const { t, i18n } = useTranslation();

  return (
    <section className="counterparty-section-card counterparty-reviews-card card border-0 shadow-sm rounded-4 mt-4">
      <div className="card-body p-4">
        <h2 className="h5 fw-semibold mb-3">{t('profile.public.reviews')}</h2>

        {isLoading && (
          <p role="status" className="text-secondary small mb-0">
            {t('profile.common.loading')}
          </p>
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
            {reviews.map((review, i) => (
              <li key={`${review.authorName}-${review.createdAt}-${i}`} className="border-bottom pb-3">
                <div className="d-flex flex-wrap align-items-center gap-2 mb-1">
                  <span className="fw-semibold ryden-text-break">{review.authorName}</span>
                  <StarRow rating={review.rating} />
                  {review.createdAt && (
                    <span className="text-secondary small ms-auto">
                      {formatDateLong(review.createdAt, i18n.language)}
                    </span>
                  )}
                </div>
                {review.comment && review.comment.trim() ? (
                  <p className="mb-0 ryden-multiline-plaintext">{review.comment}</p>
                ) : (
                  <p className="mb-0 text-secondary small fst-italic">
                    {t('profile.public.reviewNoComment')}
                  </p>
                )}
              </li>
            ))}
          </ul>
        )}
      </div>
    </section>
  );
}
