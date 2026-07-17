import { useEffect, useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useSearchParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import CarCardImage from '../../components/CarCardImage';
import { ConsumerCarCard, LoadingBlock, Pagination } from '../../components/ryden';
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
        <LoadingBlock variant="page" className="py-4" />
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
  const [searchParams, setSearchParams] = useSearchParams();
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
      void queryClient.invalidateQueries({ queryKey: ['browse', 'favorites'] });
    },
  });

  const cars = favQuery.data?.data ?? [];
  const total = favQuery.data?.page.total;
  const totalPages = pageCount(total, FAVORITES_PAGE_SIZE);

  // N-41/N-44: clamp out-of-range page (incl. after removing last item on page > 0).
  useEffect(() => {
    if (favQuery.isLoading || favQuery.isFetching || total == null) return;
    const maxPage = Math.max(0, totalPages - 1);
    if (pageIndex > maxPage) {
      setSearchParams(
        (prev) => {
          const next = new URLSearchParams(prev);
          if (maxPage <= 0) next.delete('page');
          else next.set('page', String(maxPage));
          return next;
        },
        { replace: true },
      );
    }
  }, [favQuery.isLoading, favQuery.isFetching, total, totalPages, pageIndex, setSearchParams]);

  const showEmpty = !favQuery.isLoading && cars.length === 0 && pageIndex === 0 && (total == null || total === 0);

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
        <LoadingBlock variant="grid" />
      ) : favQuery.isError ? (
        <div className="alert alert-danger" role="alert">
          {t('profile.common.error')}
        </div>
      ) : showEmpty ? (
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
                      favoriteBusy={remove.isPending}
                      onToggleFavorite={() => {
                        if (remove.isPending) return;
                        remove.mutate(car.links.self);
                      }}
                      imageSlot={
                        <CarCardImage coverUri={car.links.cover} authenticated>
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
