import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useMyUserUri } from './hooks';
import { favoritesPath, fetchFavorites, fetchUser, removeFavorite } from './api';
import CarCard from './CarCard';
import type { UserDto } from './types';

// =============================================================================
// FavoritesPage — autos favoritos del usuario (/favoritos).
//   GET /users/{myId}/favorites (accept car, paginado por header Link).
//   Quitar favorito → DELETE /users/{myId}/favorites/{carId} (idempotente).
// Paginación: se navega next/prev del header Link (PageLinks), no se arman URLs.
// =============================================================================

export default function FavoritesPage() {
  const { t } = useTranslation();
  const myUri = useMyUserUri();

  // Necesitamos el UserDto para resolver el link de favoritos (y el path de
  // borrado). Se cachea junto al perfil propio.
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
  // `cursor` es el path/link de la página actual; arranca en la colección base.
  const [cursor, setCursor] = useState<string>(() => favoritesPath(user));

  const favQuery = useQuery({
    queryKey: ['profile', 'favorites', cursor],
    queryFn: () => fetchFavorites(cursor),
  });

  const remove = useMutation({
    mutationFn: (carSelfLink: string) => removeFavorite(user, carSelfLink),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['profile', 'favorites'] });
    },
  });

  return (
    <div className="container my-4">
      <div className="mb-3">
        <h1 className="h3 mb-1 fw-semibold">{t('profile.favorites.title')}</h1>
        <p className="text-secondary mb-0">{t('profile.favorites.subtitle')}</p>
      </div>

      {remove.isError && (
        <div className="alert alert-warning" role="alert">
          {t('profile.common.error')}
        </div>
      )}

      {favQuery.isLoading ? (
        <p role="status" className="text-secondary">
          {t('profile.common.loading')}
        </p>
      ) : favQuery.isError ? (
        <div className="alert alert-danger" role="alert">
          {t('profile.common.error')}
        </div>
      ) : (
        <FavoritesContent
          cars={favQuery.data?.data ?? []}
          page={favQuery.data?.page}
          onRemove={(self) => remove.mutate(self)}
          removing={remove.isPending}
          onCursor={setCursor}
        />
      )}
    </div>
  );
}

function FavoritesContent({
  cars,
  page,
  onRemove,
  removing,
  onCursor,
}: {
  cars: Awaited<ReturnType<typeof fetchFavorites>>['data'];
  page?: Awaited<ReturnType<typeof fetchFavorites>>['page'];
  onRemove: (self: string) => void;
  removing: boolean;
  onCursor: (c: string) => void;
}) {
  const { t } = useTranslation();

  if (cars.length === 0) {
    return (
      <div className="text-center py-5">
        <div className="display-4 text-secondary-subtle mb-3" aria-hidden="true">
          <i className="bi bi-heart"></i>
        </div>
        <h2 className="h4 fw-semibold mb-2">{t('profile.favorites.emptyTitle')}</h2>
        <p className="text-secondary mb-4">{t('profile.favorites.empty')}</p>
        <Link to="/buscar" className="btn btn-primary">
          {t('profile.favorites.emptyCta')}
        </Link>
      </div>
    );
  }

  return (
    <>
      <div className="row row-cols-1 row-cols-md-2 row-cols-lg-4 g-3 pt-2">
        {cars.map((car) => (
          <div key={car.links.self} className="col d-flex justify-content-center">
            <CarCard
              car={car}
              action={
                <div className="carcard-favorite-form">
                  <button
                    type="button"
                    className="carcard-favorite-btn carcard-favorite-btn--on"
                    onClick={() => onRemove(car.links.self)}
                    disabled={removing}
                    aria-label={t('profile.favorites.remove')}
                    title={t('profile.favorites.remove')}
                  >
                    <i className="bi bi-heart-fill" aria-hidden="true"></i>
                  </button>
                </div>
              }
            />
          </div>
        ))}
      </div>

      {(page?.prev || page?.next) && (
        <nav className="d-flex justify-content-center gap-2 mt-4" aria-label="pagination">
          <button
            type="button"
            className="btn btn-outline-primary"
            disabled={!page?.prev}
            onClick={() => page?.prev && onCursor(page.prev)}
          >
            <i className="bi bi-chevron-left" aria-hidden="true"></i> {t('profile.favorites.prev')}
          </button>
          <button
            type="button"
            className="btn btn-outline-primary"
            disabled={!page?.next}
            onClick={() => page?.next && onCursor(page.next)}
          >
            {t('profile.favorites.next')} <i className="bi bi-chevron-right" aria-hidden="true"></i>
          </button>
        </nav>
      )}
    </>
  );
}
