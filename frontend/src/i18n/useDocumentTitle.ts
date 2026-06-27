import { useEffect } from 'react';
import { useLocation } from 'react-router-dom';
import { useTranslation } from 'react-i18next';

// Título dinámico por ruta (corrección devolución "Zeke 2": el <title> era estático "Ryden" en TODA
// ruta, así que cada entrada del history del browser se veía igual). Mapea la ruta client-side activa
// a una clave i18n y setea `document.title = "<página> · Ryden"`. El idioma se respeta (depende de t).
const ROUTE_TITLES: Array<{ test: RegExp; key: string }> = [
  { test: /^\/$/, key: 'title.home' },
  { test: /^\/buscar/, key: 'title.search' },
  { test: /^\/autos\//, key: 'title.carDetail' },
  { test: /^\/ingresar/, key: 'title.login' },
  { test: /^\/registrarse/, key: 'title.register' },
  { test: /^\/verificar-email/, key: 'title.verifyEmail' },
  { test: /^\/recuperar-clave/, key: 'title.forgot' },
  { test: /^\/publicar/, key: 'title.publish' },
  { test: /^\/mis-autos/, key: 'title.myCars' },
  { test: /^\/reservar\//, key: 'title.newReservation' },
  { test: /^\/mis-reservas/, key: 'title.myReservations' },
  { test: /^\/reservas\//, key: 'title.reservationDetail' },
  { test: /^\/perfil/, key: 'title.profile' },
  { test: /^\/favoritos/, key: 'title.favorites' },
  { test: /^\/usuarios\//, key: 'title.publicProfile' },
  { test: /^\/admin/, key: 'title.admin' },
];

/** Mantiene `document.title` sincronizado con la ruta y el idioma activos. */
export function useDocumentTitle(): void {
  const { pathname } = useLocation();
  const { t, i18n } = useTranslation();

  useEffect(() => {
    const match = ROUTE_TITLES.find((r) => r.test.test(pathname));
    const brand = t('app.name');
    const page = t(match ? match.key : 'title.notFound');
    document.title = `${page} · ${brand}`;
  }, [pathname, t, i18n.language]);
}
