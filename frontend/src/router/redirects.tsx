import { Navigate, useLocation } from 'react-router-dom';

/** Preserves the query string on redirect (filters, tokens, pagination). */
export function RedirectWithSearch({ to }: { to: string }) {
  const { search } = useLocation();
  return <Navigate to={`${to}${search}`} replace />;
}
