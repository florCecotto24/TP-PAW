import { useCallback, useEffect, useState } from 'react';
import { sessionClient } from '../../session/sessionStore';
import { ApiError, getLinkCollectionPage } from '../../api/client';
import type { PageLinks } from '../../api/types';

interface PagedState<T> {
  items: T[];
  page: PageLinks;
  loading: boolean;
  error: ApiError | Error | null;
}

export interface PagedList<T> extends PagedState<T> {
  /** Recarga la colección desde `initialPath`. */
  reload: () => void;
  /** Navega un link de paginación (rel next/prev/first/last). */
  goTo: (link: string | undefined) => void;
}

/**
 * Lista paginada genérica del área admin. Hace el primer GET a `initialPath`
 * con el `accept` dado y luego navega por los links de paginación del header
 * `Link` (RFC 5988) — nunca arma URLs a mano. Un 204 (sin resultados) se
 * traduce a lista vacía.
 *
 * `deps` reinicia la carga cuando cambian (p.ej. filtros).
 */
export function usePagedList<T>(
  initialPath: string,
  accept: string,
  deps: unknown[] = [],
  itemAccept?: string,
): PagedList<T> {
  const [state, setState] = useState<PagedState<T>>({
    items: [],
    page: {},
    loading: true,
    error: null,
  });

  const fetchPath = useCallback(
    async (path: string) => {
      setState((s) => ({ ...s, loading: true, error: null }));
      try {
        const res = itemAccept
          ? await getLinkCollectionPage<T>(sessionClient, path, { collectionAccept: accept, itemAccept })
          : await sessionClient.get<T[] | undefined>(path, { accept });
        setState({
          items: Array.isArray(res.data) ? res.data : [],
          page: res.page,
          loading: false,
          error: null,
        });
      } catch (err) {
        setState((s) => ({
          ...s,
          loading: false,
          error: err instanceof Error ? err : new Error(String(err)),
        }));
      }
    },
    [accept, itemAccept],
  );

  useEffect(() => {
    if (!initialPath) {
      setState({ items: [], page: {}, loading: false, error: null });
      return;
    }
    void fetchPath(initialPath);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [initialPath, accept, itemAccept, ...deps]);

  const reload = useCallback(() => {
    void fetchPath(initialPath);
  }, [fetchPath, initialPath]);

  const goTo = useCallback(
    (link: string | undefined) => {
      if (link) void fetchPath(link);
    },
    [fetchPath],
  );

  return { ...state, reload, goTo };
}
