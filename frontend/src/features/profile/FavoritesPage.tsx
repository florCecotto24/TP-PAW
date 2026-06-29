import { useEffect, useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useSearchParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import CarCardImage from '../../components/CarCardImage';
import { ConsumerCarCard, Pagination } from '../../components/ryden';
import { pageCount } from '../browse/hooks';
import { carDetailHref, carDtoToConsumerCard } from '../browse/carCardAdapter';
import { useMyUserUri } from './hooks';
import { paths } from '../../routes/paths';
import { fetchFavoritesPage, fetchUser, removeFavorite } from './api';
import type { UserDto } from './types';

export const FAVORITES_PAGE_SIZE = 8;

export default function FavoritesPage() {
  const { t } = useTranslation();
  const myUri = useMyUserUri();

  useEffect(() => {
    document.body.classList.add('has-fixed-navbar');
    return () => document.body.classList.remove('has-fixed-navbar');
  }, []);

  const userQuery = useQuery({
    queryKey: ['profile', 'me', myUri],
    queryFn: () => fetchUser(myUri as string, { private: true }),
    enabled: !!myUri,
  });

  if (!myUri) {
    return (
      <div className="container my-4">
        <h1 className="h3 mb-3 fw-semibold">{t('profile.favorites.title')}</h1>
        <div className="alert alert-warning" role="alert">
          {t('profile.common.loginRequired')}
        </div>
      </div>
    );
  }
  if (userQuery.isLoading) {
    return (
      <div className="container my-4">
        <p role="status" className="text-secondary">
          {t('profile.common.loading')}
        </p>
      </div>
    );
  }
  if (userQuery.isError || !userQuery.data) {
    return (
      <div className="container my-4">
        <div className="alert alert-danger" role="alert">
          {t('profile.common.error')}
        </div>
      </div>
    );
  }

  return <FavoritesList user={userQuery.data} />;
}

function FavoritesList({ user }: { user: UserDto }) {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const [searchParams] = useSearchParams();
  const pageIndex = useMemo(() => {
    const raw = Number(searchParams.get('page') ?? '0');
    return Number.isFinite(raw) && raw >= 0 ? Math.floor(raw) : 0;
  }, [searchParams]);

  const favQuery = useQuery({
    queryKey: ['profile', 'favorites', user.links.self, pageIndex],
    queryFn: () => fetchFavoritesPage(user, pageIndex, FAVORITES_PAGE_SIZE),
  });

  const remove = useMutation({
    mutationFn: (carSelfLink: string) => removeFavorite(user, carSelfLink),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['profile', 'favorites'] });
    },
  });

  const cars = favQuery.data?.data ?? [];
  const total = favQuery.data?.page.total;
  const totalPages = pageCount(total, FAVORITES_PAGE_SIZE);

  return (
    <div className="container my-4">
      <div className="mb-3">
        <h1 className="h3 mb-1 fw-semibold">{t('profile.favorites.title')}</h1>
        <p className="text-secondary mb-0">{t('profile.favorites.subtitle')}</p>
      </div>

      {remove.isError ? (
        <div className="alert alert-warning" role="alert">
          {t('profile.common.error')}
        </div>
      ) : null}

      {favQuery.isLoading ? (
        <p role="status" className="text-secondary">
          {t('profile.common.loading')}
        </p>
      ) : favQuery.isError ? (
        <div className="alert alert-danger" role="alert">
          {t('profile.common.error')}
        </div>
      ) : cars.length === 0 ? (
        <div className="search-empty-state text-center">
          <div className="search-empty-state__icon" aria-hidden="true">
            <i className="bi bi-heart"></i>
          </div>
          <h2 className="h4 fw-semibold mb-2">{t('profile.favorites.emptyTitle')}</h2>
          <p className="text-secondary mb-0 search-empty-state__text">{t('profile.favorites.empty')}</p>
          <div className="search-empty-state__actions mt-4">
            <Link to={paths.search} className="btn btn-primary btn-action btn-action-md">
              {t('profile.favorites.emptyCta')}
            </Link>
          </div>
        </div>
      ) : (
        <>
          <div className="text-center">
            <div className="row row-cols-1 row-cols-md-2 row-cols-lg-4 pt-2 g-3">
              {cars.map((car) => {
                const card = {
                  ...carDtoToConsumerCard(car),
                  favoritable: true,
                  favorited: true,
                };
                const href = carDetailHref(car, { src: 'my-favorites' });
                return (
                  <div key={car.links.self} className="col d-flex justify-content-center">
                    <ConsumerCarCard
                      card={card}
                      href={href}
                      onToggleFavorite={() => remove.mutate(car.links.self)}
                      imageSlot={
                        <CarCardImage coverUri={car.links.cover}>
                          {href ? (
                            <span className="carcard-view-chip" aria-hidden="true">
                              {t('carCard.viewChip')}
                            </span>
                          ) : null}
                        </CarCardImage>
                      }
                    />
                  </div>
                );
              })}
            </div>
          </div>

          {totalPages > 1 ? (
            <Pagination
              currentPage={pageIndex}
              totalPages={totalPages}
              baseUrl={paths.myFavorites}
              pageParam="page"
            />
          ) : null}
        </>
      )}
    </div>
  );
}
