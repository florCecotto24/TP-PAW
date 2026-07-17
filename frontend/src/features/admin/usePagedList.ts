import { useCallback, useEffect, useRef, useState } from 'react';
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
  /** Recarga la página actual desde el servidor (tras una mutación). */
  reload: () => void;
}

/**
 * Inyecta `?page=N` (1-based) en una ruta de colección preservando cualquier
 * query previo (filtros). No arma URNs a mano: `page` es un query param de
 * colección legítimo (§1.6/§16, @QueryParam("page") @DefaultValue("1")).
 */
function pathWithPage(basePath: string, page: number): string {
  const [path, existing = ''] = basePath.split('?');
  const usp = new URLSearchParams(existing);
  usp.set('page', String(Math.max(1, page)));
  return `${path}?${usp.toString()}`;
}

/**
 * Lista paginada genérica del área admin, URL-driven: la PÁGINA la decide el
 * caller (que la lee de la URL vía useSearchParams) y se la pasa por `page`
 * (1-based). El hook hace el GET a `basePath?page=N` con el `accept` dado — o,
 * si se pasa `itemAccept`, resuelve una colección de links (patrón A) siguiendo
 * cada `self`. La navegación entre páginas es re-render por cambio de `page`
 * (no se siguen los links del header, pero `res.page` se expone para
 * habilitar/inhabilitar prev/next y mostrar el total). Un 204 (sin resultados)
 * -> lista vacía; `basePath` vacío -> lista vacía sin fetch (guards, sin id).
 *
 * `deps` reinicia la carga cuando cambian (p.ej. filtros).
 */
export function usePagedList<T>(
  basePath: string,
  accept: string,
  page: number,
  deps: unknown[] = [],
  itemAccept?: string,
): PagedList<T> {
  const [state, setState] = useState<PagedState<T>>({
    items: [],
    page: {},
    loading: true,
    error: null,
  });
  const requestSeq = useRef(0);

  const fetchPath = useCallback(
    async (path: string) => {
      const seq = ++requestSeq.current;
      setState((s) => ({ ...s, loading: true, error: null }));
      try {
        const res = itemAccept
          ? await getLinkCollectionPage<T>(sessionClient, path, { collectionAccept: accept, itemAccept })
          : await sessionClient.get<T[] | undefined>(path, { accept });
        if (seq !== requestSeq.current) return;
        setState({
          items: Array.isArray(res.data) ? res.data : [],
          page: res.page,
          loading: false,
          error: null,
        });
      } catch (err) {
        if (seq !== requestSeq.current) return;
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
    if (!basePath) {
      requestSeq.current += 1;
      setState({ items: [], page: {}, loading: false, error: null });
      return;
    }
    void fetchPath(pathWithPage(basePath, page));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [basePath, accept, itemAccept, page, ...deps]);

  const reload = useCallback(() => {
    if (basePath) void fetchPath(pathWithPage(basePath, page));
  }, [fetchPath, basePath, page]);

  return { ...state, reload };
}
