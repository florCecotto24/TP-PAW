// Punto de entrada público del área BROWSE (home + búsqueda + detalle).
// El host consume `browseRoutes` (las monta dentro del Layout) y `browseI18n`
// (lo mergea en el namespace translation, es/en).
//
// Se usa createElement (no JSX) para mantener este barrel como .ts puro, según
// el contrato de exports del módulo.

import { createElement } from 'react';
import type { RouteObject } from 'react-router-dom';
import BrowseHomePage from './pages/HomePage';
import SearchPage from './pages/SearchPage';
import CarDetailPage from './pages/CarDetailPage';

export const browseRoutes: RouteObject[] = [
  { index: true, element: createElement(BrowseHomePage) },
  { path: 'buscar', element: createElement(SearchPage) },
  { path: 'autos/:id', element: createElement(CarDetailPage) },
];

export { browseI18n } from './i18n';

export type {
  CarDto,
  AvailabilityDto,
  PictureDto,
  ReviewDto,
  NeighborhoodDto,
} from './types';
