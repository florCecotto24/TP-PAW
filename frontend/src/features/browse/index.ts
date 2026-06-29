import { createElement } from 'react';
import type { RouteObject } from 'react-router-dom';
import { RedirectWithSearch } from '../../router/redirects';
import { paths } from '../../routes/paths';
import BrowseHomePage from './pages/HomePage';
import SearchPage from './pages/SearchPage';
import CarDetailPage from './pages/CarDetailPage';

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
