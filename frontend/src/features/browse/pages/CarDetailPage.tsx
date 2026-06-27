import { useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { formatCurrency } from '../../../api/format';
import { apiAssetUrl, idFromUri } from '../../../api/uri';
import { useSessionStore } from '../../../session/sessionStore';
import {
  reviewsTotal,
  useCar,
  useCarAvailabilities,
  useCarOwner,
  useCarPictures,
  useCarReviews,
  useIsFavorite,
  useToggleFavorite,
} from '../hooks';
import type { AvailabilityDto, PictureDto, ReviewDto } from '../types';

function minOfferedPrice(availabilities: AvailabilityDto[]): number | null {
  const prices = availabilities
    .filter((a) => a.kind === 'offered')
    .map((a) => a.dayPrice)
    .filter((p) => Number.isFinite(p));
  return prices.length > 0 ? Math.min(...prices) : null;
}

function Gallery({ pictures }: { pictures: PictureDto[] }) {
  const { t } = useTranslation();
  const [active, setActive] = useState(0);

  if (pictures.length === 0) {
    return (
      <div className="bg-light rounded-4 d-flex align-items-center justify-content-center py-5">
        <p className="text-secondary mb-0">{t('browse.detail.noPictures')}</p>
      </div>
    );
  }

  const current = pictures[active] ?? pictures[0];

  return (
    <div>
      <div className="ratio ratio-16x9 bg-light rounded-4 overflow-hidden mb-3">
        {current.kind === 'image' ? (
          <img
            src={apiAssetUrl(current.links.self)}
            alt=""
            className="object-fit-cover w-100 h-100"
          />
        ) : (
          <video
            src={apiAssetUrl(current.links.self)}
            controls
            className="w-100 h-100 object-fit-cover"
          />
        )}
      </div>
      {pictures.length > 1 ? (
        <div className="d-flex flex-wrap gap-2">
          {pictures.map((pic, index) => (
            <button
              key={pic.links.self}
              type="button"
              className={`btn btn-sm ${index === active ? 'btn-primary' : 'btn-outline-secondary'}`}
              onClick={() => setActive(index)}
            >
              {index + 1}
            </button>
          ))}
        </div>
      ) : null}
    </div>
  );
}

function StarRating({ value }: { value: number }) {
  const floor = Math.floor(value);
  return (
    <span className="text-warning" aria-hidden="true">
      {[1, 2, 3, 4, 5].map((star) => (
        <i key={star} className={`bi bi-star${star <= floor ? '-fill' : ''}`}></i>
      ))}
    </span>
  );
}

function ReviewRow({ review }: { review: ReviewDto }) {
  const { t } = useTranslation();
  return (
    <article className="border-bottom pb-3 mb-3">
      <div className="d-flex align-items-center gap-2 mb-1">
        {review.rating != null ? (
          <>
            <StarRating value={review.rating} />
            <span className="fw-semibold">{review.rating.toFixed(1)}</span>
          </>
        ) : null}
        <span className="small text-secondary">{new Date(review.createdAt).toLocaleDateString()}</span>
      </div>
      <p className="mb-0">{review.comment?.trim() || t('browse.detail.noComment')}</p>
      {review.links.image ? (
        <img
          src={apiAssetUrl(review.links.image)}
          alt=""
          className="img-fluid rounded mt-2"
          style={{ maxHeight: 160 }}
        />
      ) : null}
    </article>
  );
}

export default function CarDetailPage() {
  const { t } = useTranslation();
  const { id } = useParams<{ id: string }>();
  const carQuery = useCar(id);
  const car = carQuery.data;
  const picturesQuery = useCarPictures(car?.links.pictures);
  const ownerQuery = useCarOwner(car?.links.owner);
  const availQuery = useCarAvailabilities(car?.links.availabilities);
  const reviewsQuery = useCarReviews(car?.links.reviews);
  const isLoggedIn = useSessionStore((s) => s.status === 'authenticated');
  const favoriteQuery = useIsFavorite(car?.links.self);
  const toggleFavorite = useToggleFavorite();

  const minPrice = useMemo(
    () => minOfferedPrice(availQuery.data ?? []),
    [availQuery.data],
  );
  const reviewCount = reviewsTotal(reviewsQuery.data);
  const allReviews = reviewsQuery.data?.pages.flatMap((p) => p.items) ?? [];
  const ownerId = ownerQuery.data ? idFromUri(ownerQuery.data.links.self) : null;

  if (carQuery.isLoading) {
    return (
      <main className="container py-4">
        <p className="text-secondary" role="status">{t('app.loading')}</p>
      </main>
    );
  }

  if (carQuery.isError || !car) {
    return (
      <main className="container py-4">
        <div className="alert alert-danger" role="alert">{t('browse.detail.loadError')}</div>
        <Link to="/buscar" className="btn btn-link">{t('browse.detail.backToSearch')}</Link>
      </main>
    );
  }

  const title = `${car.brandName} ${car.modelName}`.trim();
  const favoriteBusy = toggleFavorite.isPending;

  return (
    <main className="container py-4">
      <div className="mb-3">
        <Link to="/buscar" className="btn btn-link ps-0">{t('browse.detail.backToSearch')}</Link>
      </div>

      <div className="row g-4">
        <div className="col-lg-7">
          <section className="bg-white rounded-4 shadow-sm p-4">
            <h1 className="h3 fw-semibold mb-3">{title}</h1>
            <h2 className="h6 text-secondary mb-3">{t('browse.detail.gallery')}</h2>
            {picturesQuery.isLoading ? (
              <p className="text-secondary small">{t('app.loading')}</p>
            ) : (
              <Gallery pictures={picturesQuery.data ?? []} />
            )}
          </section>

          {car.description ? (
            <section className="bg-white rounded-4 shadow-sm p-4 mt-4">
              <h2 className="h5 fw-semibold mb-3">{t('browse.detail.description')}</h2>
              <p className="mb-0">{car.description}</p>
            </section>
          ) : null}

          <section className="bg-white rounded-4 shadow-sm p-4 mt-4">
            <h2 className="h5 fw-semibold mb-3">{t('browse.detail.specification')}</h2>
            <ul className="list-unstyled mb-0 small">
              <li><strong>{t('browse.search.category')}:</strong> {t(`browse.carType.${car.type}`)}</li>
              <li><strong>{t('browse.search.transmission')}:</strong> {t(`browse.transmission.${car.transmission}`)}</li>
              <li><strong>{t('browse.search.powertrain')}:</strong> {t(`browse.powertrain.${car.powertrain}`)}</li>
              {car.year != null ? <li><strong>Año:</strong> {car.year}</li> : null}
              <li><strong>{t('browse.detail.minRentalDays')}:</strong> {car.minimumRentalDays}</li>
              {car.ratingAvg != null ? (
                <li><strong>{t('browse.detail.rating')}:</strong> {car.ratingAvg.toFixed(1)}</li>
              ) : null}
            </ul>
          </section>

          <section className="bg-white rounded-4 shadow-sm p-4 mt-4">
            <h2 className="h5 fw-semibold mb-3">
              {t('browse.detail.reviews')}
              {reviewCount != null ? (
                <span className="text-secondary fw-normal ms-1">
                  {t('browse.detail.reviewsCount', { count: reviewCount })}
                </span>
              ) : null}
            </h2>
            {reviewsQuery.isLoading ? (
              <p className="text-secondary small">{t('app.loading')}</p>
            ) : null}
            {allReviews.length === 0 && !reviewsQuery.isLoading ? (
              <p className="text-secondary mb-0">{t('browse.detail.noReviews')}</p>
            ) : null}
            {allReviews.map((review) => (
              <ReviewRow key={review.links.self} review={review} />
            ))}
            {reviewsQuery.hasNextPage ? (
              <button
                type="button"
                className="btn btn-outline-primary btn-sm"
                disabled={reviewsQuery.isFetchingNextPage}
                onClick={() => void reviewsQuery.fetchNextPage()}
              >
                {reviewsQuery.isFetchingNextPage ? t('app.loading') : t('browse.detail.moreReviews')}
              </button>
            ) : null}
          </section>
        </div>

        <div className="col-lg-5">
          <aside className="bg-white rounded-4 shadow-sm p-4 sticky-top" style={{ top: '1rem' }}>
            {minPrice != null ? (
              <p className="mb-2">
                <span className="text-secondary small">{t('browse.detail.priceFrom')} </span>
                <span className="fs-4 fw-bold">{formatCurrency(minPrice)}</span>
                <span className="text-secondary"> / {t('browse.detail.perDay')}</span>
              </p>
            ) : null}

            {id ? (
              <Link to={`/reservar/${id}`} className="btn btn-primary w-100 mb-3">
                {t('browse.detail.reserve')}
              </Link>
            ) : null}

            {isLoggedIn ? (
              <button
                type="button"
                className="btn btn-outline-secondary w-100 mb-3"
                disabled={favoriteBusy || favoriteQuery.isLoading}
                onClick={() =>
                  void toggleFavorite.mutateAsync({
                    car,
                    makeFavorite: !(favoriteQuery.data ?? false),
                  })
                }
              >
                {favoriteQuery.data ? t('browse.detail.unfavorite') : t('browse.detail.favorite')}
              </button>
            ) : null}

            <h2 className="h6 fw-semibold mb-3">{t('browse.detail.availability')}</h2>
            {availQuery.isLoading ? (
              <p className="text-secondary small">{t('app.loading')}</p>
            ) : null}
            {(availQuery.data ?? []).filter((a) => a.kind === 'offered').length === 0 ? (
              <p className="text-secondary small mb-0">{t('browse.detail.noAvailability')}</p>
            ) : (
              <ul className="list-unstyled small mb-0">
                {(availQuery.data ?? [])
                  .filter((a) => a.kind === 'offered')
                  .map((a) => (
                    <li key={a.links.self} className="mb-2 pb-2 border-bottom">
                      <div className="fw-semibold">
                        {a.startDate} — {a.endDate}
                      </div>
                      <div>
                        {formatCurrency(a.dayPrice)} / {t('browse.detail.perDay')}
                      </div>
                      <div className="text-secondary">
                        {a.startPointStreet}
                        {a.startPointNumber ? ` ${a.startPointNumber}` : ''}
                      </div>
                    </li>
                  ))}
              </ul>
            )}

            {ownerQuery.data && ownerId ? (
              <div className="mt-4 pt-3 border-top">
                <Link to={`/usuarios/${ownerId}`} className="btn btn-link ps-0">
                  {t('browse.detail.owner')}
                </Link>
                <p className="mb-0 fw-semibold">
                  {ownerQuery.data.forename} {ownerQuery.data.surname}
                </p>
              </div>
            ) : null}
          </aside>
        </div>
      </div>
    </main>
  );
}
