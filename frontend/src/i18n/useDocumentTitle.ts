import { useEffect } from 'react';
import { useLocation } from 'react-router-dom';
import { useTranslation } from 'react-i18next';

const ROUTE_TITLES: Array<{ test: RegExp; key: string }> = [
  { test: /^\/$/, key: 'title.home' },
  { test: /^\/search/, key: 'title.search' },
  { test: /^\/cars\//, key: 'title.carDetail' },
  { test: /^\/login/, key: 'title.login' },
  { test: /^\/register/, key: 'title.register' },
  { test: /^\/verify-email/, key: 'title.verifyEmail' },
  { test: /^\/forgot-password/, key: 'title.forgot' },
  { test: /^\/publish-car/, key: 'title.publish' },
  { test: /^\/my-cars\/reservations/, key: 'title.ownerReservations' },
  { test: /^\/my-cars/, key: 'title.myCars' },
  { test: /^\/cars\/[^/]+\/reservation\/new/, key: 'title.newReservation' },
  { test: /^\/my-reservations/, key: 'title.myReservations' },
  { test: /^\/profile/, key: 'title.profile' },
  { test: /^\/my-favorites/, key: 'title.favorites' },
  { test: /^\/users\//, key: 'title.publicProfile' },
  { test: /^\/admin/, key: 'title.admin' },
];

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
