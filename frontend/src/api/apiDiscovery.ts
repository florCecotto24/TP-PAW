import { create } from 'zustand';

import type { HypermediaClient } from './client';
import { useClientConfigStore } from './clientConfig';
import { MediaTypes } from './mediaTypes';
import { hrefToRelativeApiPath } from './uri';

export interface ApiIndexResourceDescriptor {
  href: string;
  queryParams?: string[];
}

export interface ApiIndexDto {
  links: Record<string, string>;
  resources: Record<string, ApiIndexResourceDescriptor>;
}

const COLLECTION_FALLBACKS = {
  cars: '/cars',
  brands: '/brands',
  models: '/models',
  neighborhoods: '/neighborhoods',
  reservations: '/reservations',
  users: '/users',
  reviews: '/reviews',
} as const;

export const API_COLLECTION_FALLBACK_PATHS = COLLECTION_FALLBACKS;

export type ApiCollectionName = keyof typeof COLLECTION_FALLBACKS;

interface ApiDiscoveryState {
  ready: boolean;
  index: ApiIndexDto | null;
  load: (client: HypermediaClient) => Promise<void>;
  collectionPath: (name: ApiCollectionName) => string;
}

export const useApiDiscoveryStore = create<ApiDiscoveryState>((set, get) => ({
  ready: false,
  index: null,
  load: async (client) => {
    if (get().ready) {
      return;
    }
    try {
      const res = await client.get<ApiIndexDto>('/', {
        accept: MediaTypes.api,
        anonymous: true,
      });
      set({ index: res.data, ready: true });
      void useClientConfigStore.getState().load(client, res.data?.links?.config);
    } catch {
      set({ index: null, ready: true });
    }
  },
  collectionPath: (name) => {
    const index = get().index;
    const href = index?.resources?.[name]?.href ?? index?.links?.[name];
    if (href) {
      return hrefToRelativeApiPath(href);
    }
    return COLLECTION_FALLBACKS[name];
  },
}));

/** Synchronous accessor for modules that cannot subscribe to the store. */
export function getCollectionPath(name: ApiCollectionName): string {
  return useApiDiscoveryStore.getState().collectionPath(name);
}

export function collectionQueryPath(
  name: ApiCollectionName,
  params: Record<string, string | undefined | null> = {},
): string {
  const base = getCollectionPath(name);
  const search = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value != null && value !== '') {
      search.set(key, value);
    }
  }
  const qs = search.toString();
  return qs ? `${base}?${qs}` : base;
}
