import { create } from 'zustand';

import type { HypermediaClient } from './client';
import { useClientConfigStore } from './clientConfig';
import { MediaTypes } from './mediaTypes';
import { hrefToRelativeApiPath } from './uri';

export interface ApiIndexResourceDescriptor {
  href: string;
  /** RFC 6570-style canonical item URI template published by the API index. */
  itemTemplate?: string;
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
  itemTemplate: (name: ApiCollectionName) => string | null;
}

export const useApiDiscoveryStore = create<ApiDiscoveryState>((set, get) => ({
  ready: false,
  index: null,
  load: async (client) => {
    // ready means a successful discovery load; failures stay !ready so callers can retry
    // (N-18) and the shell can gate routes until templates exist (N-09).
    if (get().ready && get().index != null) {
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
      set({ index: null, ready: false });
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
  itemTemplate: (name) => get().index?.resources?.[name]?.itemTemplate ?? null,
}));

/** Synchronous accessor for modules that cannot subscribe to the store. */
export function getCollectionPath(name: ApiCollectionName): string {
  return useApiDiscoveryStore.getState().collectionPath(name);
}

/**
 * Expands an item template explicitly published by API discovery. Route IDs are
 * only used for bookmark/deep-link recovery; normal navigation must keep
 * following the item's {@code links.self}.
 */
export function expandItemTemplate(name: ApiCollectionName, id: string): string {
  const template = useApiDiscoveryStore.getState().itemTemplate(name);
  if (!template || !template.includes('{id}')) {
    throw new Error(`api.discovery.missingItemTemplate:${name}`);
  }
  return template.replace(/\{id\}/g, encodeURIComponent(id));
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
