import { createElement, lazy } from 'react';
import type { RouteObject } from 'react-router-dom';
import { RedirectWithSearch } from '../../router/redirects';
import { paths } from '../../routes/paths';
// BrowseHomePage se queda EAGER: es la ruta index (`/`), lo primero que ve
// cualquier visitante anónimo -- lazy-loadearla sumaría un roundtrip extra
// (bundle -> fetch del chunk -> primer paint) justo en la página de mayor
// tráfico. search / cars-detail SÍ son buenos candidatos: solo se llega ahí
// navegando, nunca son el primer render.
import BrowseHomePage from './pages/HomePage';
const SearchPage = lazy(() => import('./pages/SearchPage'));
const CarDetailPage = lazy(() => import('./pages/CarDetailPage'));

export const browseRoutes: RouteObject[] = [
  { index: true, element: createElement(BrowseHomePage) },
  /** Legacy bookmark from JSP (`/home`); canonical home is {@link paths.home} (`/`). */
  { path: 'home', element: createElement(RedirectWithSearch, { to: paths.home }) },
  { path: 'search', element: createElement(SearchPage) },
  { path: 'cars/:id', element: createElement(CarDetailPage) },
];

export { browseI18n } from './i18n';

export type {
  CarDto,
  AvailabilityDto,
  PictureDto,
  ReviewDto,
  NeighborhoodDto,
} from './types';
