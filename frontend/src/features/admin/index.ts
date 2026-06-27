// Barrel público del área ADMIN. El host consume desde acá:
//   - `adminRoutes`: RouteObject[] a anidar bajo `/admin` en el AppRouter.
//   - `adminI18n`: recursos i18n {es, en} bajo el prefijo `admin.`.
// (index.ts queda libre de JSX a propósito; las rutas viven en routes.tsx
//  porque el tsconfig usa jsx:react-jsx y los .ts no admiten JSX.)

export { adminRoutes } from './routes';
export { adminI18n } from './i18n';
export type * from './types';
