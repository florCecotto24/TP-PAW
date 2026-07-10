import { useMemo } from 'react';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  BreadcrumbTrail,
  CarDetailGalleryGrid,
  CarDetailGalleryModal,
  DetailListingMeta,
  LoadingBlock,
  ReviewCard,
  SimilarVehiclesHeader,
  SpecCard,
  chunk,
  type GalleryMediaItem,
} from '../../../components/ryden';
import { apiAssetUrl, idFromUri, profilePictureAssetUrl } from '../../../api/uri';
import { ApiError } from '../../../api/client';
import { formatDateLong } from '../../../i18n/dateFormat';
import { useSessionStore } from '../../../session/sessionStore';
import { apiErrorMessage } from '../../auth/errorMessage';
import { carDetail, paths, publicProfile } from '../../../routes/paths';
import DetailReservationForm from '../components/DetailReservationForm';
import BrowseCarCard from '../components/BrowseCarCard';
import {
  CAR_REVIEWS_PAGE_SIZE,
  useAdminSetCarStatus,
  useCar,
  useCarAvailabilities,
  useCarOwner,
  useCarPictures,
  useCarReviewsPage,
  useIsFavorite,
  useSimilarCars,
  useToggleFavorite,
  useUserBrief,
} from '../hooks';
import type { AvailabilityDto, PictureDto, ReviewDto } from '../types';

const GALLERY_MODAL_ID = 'carDetailGalleryModal';
const GALLERY_CAROUSEL_ID = 'carDetailCarousel';
const REVIEWS_CAROUSEL_ID = 'carDetailReviewsCarousel';

function minOfferedPrice(availabilities: AvailabilityDto[]): number | null {
  const prices = availabilities
    .filter((a) => a.kind === 'offered')
    .map((a) => a.dayPrice)
    .filter((p) => Number.isFinite(p));
  return prices.length > 0 ? Math.min(...prices) : null;
}

function isVariablePrice(availabilities: AvailabilityDto[]): boolean {
  const prices = availabilities
    .filter((a) => a.kind === 'offered')
    .map((a) => a.dayPrice);
  return new Set(prices).size > 1;
}

function picturesToGalleryMedia(pictures: PictureDto[]): GalleryMediaItem[] {
  return pictures.map((pic) => ({
    url: apiAssetUrl(pic.links.self),
    video: pic.kind === 'video',
    contentType: pic.contentType,
  }));
}

function ReviewCardWithAuthor({ review }: { review: ReviewDto }) {
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

export default function CarDetailPage() {
  const { t } = useTranslation();
  const [searchParams] = useSearchParams();
  const { id: resolvedId } = useParams<{ id: string }>();
  const carQuery = useCar(resolvedId);
  const car = carQuery.data;
  const picturesQuery = useCarPictures(car?.links?.pictures);
  const isLoggedIn = useSessionStore((s) => s.status === 'authenticated');
  const currentUser = useSessionStore((s) => s.currentUser);
  const isAdmin = currentUser?.role === 'admin';
  // Admin pide vista privada del dueño para saber el rol (el DTO público no lo expone).
  const ownerQuery = useCarOwner(car?.links?.owner, isAdmin);
  const availQuery = useCarAvailabilities(car?.links?.availabilities);
  const similarQuery = useSimilarCars(car);
  const favoriteQuery = useIsFavorite(car?.links?.self);
  const toggleFavorite = useToggleFavorite();
  const adminSetCarStatus = useAdminSetCarStatus();
  const ownerIsAdmin = ownerQuery.data?.role === 'admin';

  const src = searchParams.get('src');
  const fromParam = searchParams.get('from') ?? undefined;
  const untilParam = searchParams.get('until') ?? undefined;
  const reviewsView = searchParams.get('reviewsView');
  const reviewPage = Math.max(0, Number(searchParams.get('reviewPage') ?? '0') || 0);
  const reviewsIsListView = reviewsView === 'list';

  const reviewsQuery = useCarReviewsPage(car?.links?.reviews, reviewPage, CAR_REVIEWS_PAGE_SIZE);
  const reviewItems = reviewsQuery.data?.items ?? [];
  const reviewTotal = reviewsQuery.data?.page.total;
  const reviewPageCount =
    reviewTotal != null ? Math.max(1, Math.ceil(reviewTotal / CAR_REVIEWS_PAGE_SIZE)) : 1;
  const reviewsHasMoreThanOnePage = reviewPageCount > 1;

  const offeredAvailabilities = useMemo(
    () => (availQuery.data ?? []).filter((a) => a.kind === 'offered'),
    [availQuery.data],
  );
  const minPrice = useMemo(() => minOfferedPrice(offeredAvailabilities), [offeredAvailabilities]);
  const priceFrom = isVariablePrice(offeredAvailabilities);
  const galleryMedia = useMemo(
    () => picturesToGalleryMedia(picturesQuery.data ?? []),
    [picturesQuery.data],
  );

  // Preferir car.links.owner (siempre en el DTO) frente a esperar el GET del dueño;
  // el fetch privado del admin no debe condicionar si se puede favoritar.
  const ownerId =
    idFromUri(car?.links?.owner)
    ?? (ownerQuery.data?.links?.self ? idFromUri(ownerQuery.data.links.self) : null);
  const currentUserId = currentUser?.links?.self ? idFromUri(currentUser.links.self) : null;
  const isOwnerRequesting = !!ownerId && ownerId === currentUserId;
  const carIsFavoritable = isLoggedIn && !isOwnerRequesting;

  const preservedParams = useMemo(() => {
    const q: Record<string, string> = {};
    if (src === 'search') q.src = 'search';
    if (fromParam) q.from = fromParam;
    if (untilParam) q.until = untilParam;
    return q;
  }, [src, fromParam, untilParam]);

  function detailHref(extra: Record<string, string | number | undefined>): string | null {
    if (!resolvedId) return null;
    const merged: Record<string, string> = { ...preservedParams };
    for (const [k, v] of Object.entries(extra)) {
      if (v !== undefined && v !== '') merged[k] = String(v);
    }
    return carDetail(resolvedId, merged);
  }

  if (carQuery.isLoading) {
    return (
      <main className="car-detail-page container pb-4">
        <LoadingBlock variant="page" className="py-4" />
      </main>
    );
  }

  if (carQuery.isError || !car) {
    return (
      <main className="car-detail-page container pb-4">
        <div className="alert alert-danger rounded-3 mt-3" role="alert">
          {t('browse.detail.loadError')}
        </div>
        <Link to={paths.search} className="btn btn-link ps-0">
          {t('browse.detail.backToSearch')}
        </Link>
      </main>
    );
  }

  const title = `${car.brandName} ${car.modelName}`.trim();
  const ratingLabel = car.ratingAvg != null ? car.ratingAvg.toFixed(1) : undefined;
  const reviewCountLabel = reviewTotal ?? 0;
  const favoriteBusy = toggleFavorite.isPending;
  const isFavorited = favoriteQuery.data ?? false;
  const favoriteAria = isFavorited ? t('carCard.favorite.remove') : t('carCard.favorite.add');
  const similarSearchHref = `${paths.search}?category=${car.type}`;
  const ownerProfileHref =
    ownerId != null ? `${publicProfile(ownerId)}?carId=${resolvedId}` : undefined;
  const ownerProfileImageUrl = profilePictureAssetUrl(ownerQuery.data?.links);

  const reviewsToggleView = reviewsIsListView ? 'carousel' : 'list';
  const reviewsToggleHref = detailHref({
    reviewsView: reviewsToggleView,
    ...(reviewsIsListView ? { reviewPage: 0 } : {}),
  });

  return (
    <main className="car-detail-page container pb-4">
      {src === 'search' ? (
        <BreadcrumbTrail
          midLabel={t('nav.search')}
          midHref={paths.search}
          currentLabel={title}
        />
      ) : (
        <BreadcrumbTrail currentLabel={title} />
      )}

      <div className="d-flex flex-column flex-lg-row justify-content-between align-items-start gap-3 mb-4">
        <div className="flex-grow-1 min-w-0">
          <div className="d-flex align-items-center gap-2 flex-wrap">
            <h1 className="h2 fw-bold mb-0 ryden-text-break">{title}</h1>
            {carIsFavoritable ? (
              <form className="mb-0 car-detail-favorite-form" onSubmit={(e) => e.preventDefault()}>
                <button
                  type="button"
                  className={`car-detail-favorite-btn${isFavorited ? ' car-detail-favorite-btn--on' : ''}`}
                  aria-label={favoriteAria}
                  title={favoriteAria}
                  disabled={favoriteBusy || favoriteQuery.isLoading}
                  onClick={() =>
                    void toggleFavorite.mutateAsync({
                      car,
                      makeFavorite: !isFavorited,
                    })
                  }
                >
                  <i className={`bi bi-heart${isFavorited ? '-fill' : ''}`} aria-hidden="true"></i>
                </button>
              </form>
            ) : null}
          </div>
          <DetailListingMeta rating={ratingLabel} reviewCount={reviewCountLabel} />
        </div>
      </div>

      <div className="row g-4 align-items-start">
        <div className="col-lg-8 order-1 d-flex flex-column gap-4">
          {picturesQuery.isLoading ? (
            <LoadingBlock variant="inline" />
          ) : (
            <CarDetailGalleryGrid
              modalId={GALLERY_MODAL_ID}
              mediaItems={galleryMedia}
              vehicleLabel={title}
            />
          )}

          {ownerQuery.data && ownerProfileHref ? (
            <div className="d-flex align-items-center gap-2">
              <Link
                to={ownerProfileHref}
                className="d-flex align-items-center gap-2 text-decoration-none text-reset"
              >
                {ownerProfileImageUrl ? (
                  <img
                    src={ownerProfileImageUrl}
                    alt={t('carDetail.owner.profileImageAlt')}
                    className="rounded-circle border"
                    style={{ width: 40, height: 40, objectFit: 'cover' }}
                  />
                ) : (
                  <span
                    className="d-inline-flex align-items-center justify-content-center rounded-circle border bg-white text-secondary"
                    style={{ width: 40, height: 40 }}
                    aria-hidden="true"
                  >
                    <i className="bi bi-person-fill fs-5"></i>
                  </span>
                )}
                <span
                  className="fw-semibold text-decoration-underline ryden-text-break"
                  aria-label={t('carDetail.owner.nameAriaLabel')}
                >
                  {ownerQuery.data.forename} {ownerQuery.data.surname}
                </span>
              </Link>
            </div>
          ) : null}

          {car.description ? (
            <section className="card bg-white rounded-4 shadow-sm border-0 p-4">
              <h2 className="h5 fw-bold mb-3">{t('carDetail.description')}</h2>
              <p className="mb-0 ryden-multiline-plaintext">{car.description}</p>
            </section>
          ) : null}

          <section className="card bg-white rounded-4 shadow-sm border-0 p-4">
            <h2 className="h5 fw-bold mb-3">{t('carDetail.specification')}</h2>
            <div className="row row-cols-2 row-cols-md-3 g-3">
              <div className="col">
                <SpecCard icon="car-front" label={t(`browse.carType.${car.type}`)} />
              </div>
              <div className="col">
                <SpecCard
                  icon="gear-wide-connected"
                  label={t(`browse.transmission.${car.transmission}`)}
                />
              </div>
              <div className="col">
                <SpecCard icon="fuel-pump" label={t(`browse.powertrain.${car.powertrain}`)} />
              </div>
            </div>
          </section>

          <section className="card bg-white rounded-4 shadow-sm border-0 p-4 mt-4" id="listing-reviews">
            <div className="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3">
              <h2 className="h5 fw-bold mb-0">{t('carDetail.reviews.title')}</h2>
              <div className="d-flex align-items-center gap-2">
                {!reviewsIsListView && reviewItems.length > 0 ? (
                  <>
                    <button
                      className="btn btn-sm btn-outline-secondary"
                      type="button"
                      data-bs-target={`#${REVIEWS_CAROUSEL_ID}`}
                      data-bs-slide="prev"
                      aria-label={t('carDetail.reviews.carousel.prev')}
                    >
                      <i className="bi bi-chevron-left" aria-hidden="true"></i>
                    </button>
                    <button
                      className="btn btn-sm btn-outline-secondary"
                      type="button"
                      data-bs-target={`#${REVIEWS_CAROUSEL_ID}`}
                      data-bs-slide="next"
                      aria-label={t('carDetail.reviews.carousel.next')}
                    >
                      <i className="bi bi-chevron-right" aria-hidden="true"></i>
                    </button>
                  </>
                ) : null}
                {reviewsHasMoreThanOnePage && reviewsToggleHref ? (
                  <Link className="btn btn-sm btn-link text-decoration-none" to={reviewsToggleHref}>
                    {reviewsIsListView
                      ? t('carDetail.reviews.viewLess')
                      : t('carDetail.reviews.viewAll')}
                  </Link>
                ) : null}
              </div>
            </div>

            {reviewsQuery.isLoading ? <LoadingBlock variant="inline" /> : null}

            {reviewItems.length === 0 && !reviewsQuery.isLoading ? (
              <p className="text-secondary mb-0">{t('carDetail.reviews.empty')}</p>
            ) : null}

            {reviewsIsListView && reviewItems.length > 0 ? (
              <>
                <div className="row row-cols-1 row-cols-md-2 g-3 mb-3">
                  {reviewItems.map((review) => (
                    <div className="col" key={review.links.self}>
                      <ReviewCardWithAuthor review={review} />
                    </div>
                  ))}
                </div>
                {reviewsHasMoreThanOnePage ? (
                  <div className="d-flex justify-content-between align-items-center gap-2 mt-3">
                    {detailHref({ reviewsView: 'list', reviewPage: reviewPage - 1 }) ? (
                      <Link
                        to={detailHref({ reviewsView: 'list', reviewPage: reviewPage - 1 })!}
                        className={`btn btn-outline-secondary btn-sm${reviewPage > 0 ? '' : ' disabled'}`}
                        aria-disabled={reviewPage <= 0}
                        onClick={(e) => reviewPage <= 0 && e.preventDefault()}
                      >
                        {t('carDetail.reviews.prev')}
                      </Link>
                    ) : (
                      <span />
                    )}
                    <span className="text-secondary small">
                      {t('carDetail.reviews.pageIndicator', {
                        current: reviewPage + 1,
                        total: reviewPageCount,
                      })}
                    </span>
                    {detailHref({ reviewsView: 'list', reviewPage: reviewPage + 1 }) ? (
                      <Link
                        to={detailHref({ reviewsView: 'list', reviewPage: reviewPage + 1 })!}
                        className={`btn btn-outline-secondary btn-sm${
                          reviewPage + 1 < reviewPageCount ? '' : ' disabled'
                        }`}
                        aria-disabled={reviewPage + 1 >= reviewPageCount}
                        onClick={(e) => reviewPage + 1 >= reviewPageCount && e.preventDefault()}
                      >
                        {t('carDetail.reviews.next')}
                      </Link>
                    ) : (
                      <span />
                    )}
                  </div>
                ) : null}
              </>
            ) : null}

            {!reviewsIsListView && reviewItems.length > 0 ? (
              <div id={REVIEWS_CAROUSEL_ID} className="carousel slide ryden-review-carousel" data-bs-ride="false">
                <div className="carousel-inner">
                  {chunk(reviewItems, 2).map((slideReviews, slideIndex) => (
                    <div
                      key={slideIndex}
                      className={`carousel-item${slideIndex === 0 ? ' active' : ''}`}
                    >
                      <div className="row row-cols-1 row-cols-md-2 g-3 pb-2 align-items-stretch">
                        {slideReviews.map((review) => (
                          <div className="col d-flex" key={review.links.self}>
                            <ReviewCardWithAuthor review={review} />
                          </div>
                        ))}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            ) : null}
          </section>
        </div>

        <div className="col-lg-4 order-2">
          {isAdmin && car ? (
            <div className="card bg-white border-0 shadow-sm rounded-4 mb-3 p-3">
              <h6 className="fw-semibold mb-2">{t('carDetail.admin.sectionTitle')}</h6>
              {ownerIsAdmin ? (
                <p className="small text-secondary mb-0">{t('carDetail.admin.ownerIsAdmin')}</p>
              ) : (
                <>
                  {adminSetCarStatus.isSuccess ? (
                    <div className="alert alert-success py-2 small mb-2">
                      {adminSetCarStatus.variables?.status === 'active'
                        ? t('carDetail.admin.resumedSuccess')
                        : t('carDetail.admin.pausedSuccess')}
                    </div>
                  ) : null}
                  {adminSetCarStatus.isError ? (
                    <div className="alert alert-danger py-2 small mb-2">
                      {adminSetCarStatus.error instanceof ApiError
                        || adminSetCarStatus.error instanceof Error
                        ? apiErrorMessage(t, adminSetCarStatus.error)
                        : t('carDetail.admin.error')}
                    </div>
                  ) : null}
                  {car.status === 'active' || car.status === 'paused' ? (
                    <button
                      type="button"
                      className="btn btn-warning btn-sm rounded-3 w-100"
                      disabled={adminSetCarStatus.isPending || !ownerQuery.data}
                      onClick={() =>
                        adminSetCarStatus.mutate({ carSelfLink: car.links.self, status: 'admin_paused' })
                      }
                    >
                      {t('carDetail.admin.pause')}
                    </button>
                  ) : null}
                  {car.status === 'admin_paused' ? (
                    <button
                      type="button"
                      className="btn btn-success btn-sm rounded-3 w-100"
                      disabled={adminSetCarStatus.isPending}
                      onClick={() =>
                        adminSetCarStatus.mutate({ carSelfLink: car.links.self, status: 'active' })
                      }
                    >
                      {t('carDetail.admin.resume')}
                    </button>
                  ) : null}
                </>
              )}
            </div>
          ) : null}
          <div className="detail-reservation-sticky">
            {resolvedId ? (
              <DetailReservationForm
                carId={Number(resolvedId)}
                carName={title}
                dailyPrice={minPrice ?? 0}
                priceFrom={priceFrom}
                isOwnerRequesting={isOwnerRequesting}
                minimumRentalDays={car.minimumRentalDays}
              />
            ) : null}
          </div>
        </div>
      </div>

      <section
        className="similarVehiclesSection mt-5 pt-5 border-top border-secondary-subtle"
        id="similarVehiclesSection"
      >
        <SimilarVehiclesHeader seeAllHref={similarSearchHref} />
        {similarQuery.isLoading ? <LoadingBlock variant="grid" /> : null}
        {(similarQuery.data ?? []).length === 0 && !similarQuery.isLoading ? (
          <p className="text-secondary text-center mb-0">{t('carDetail.similarCarsWhenAvailable')}</p>
        ) : (
          <div className="row row-cols-1 row-cols-md-2 row-cols-lg-4 g-3">
            {(similarQuery.data ?? []).map((similar) => (
              <div className="col d-flex justify-content-center" key={similar.links.self}>
                <BrowseCarCard car={similar} />
              </div>
            ))}
          </div>
        )}
      </section>

      {galleryMedia.length > 0 ? (
        <CarDetailGalleryModal
          modalId={GALLERY_MODAL_ID}
          carouselId={GALLERY_CAROUSEL_ID}
          mediaItems={galleryMedia}
          vehicleLabel={title}
        />
      ) : null}
    </main>
  );
}
