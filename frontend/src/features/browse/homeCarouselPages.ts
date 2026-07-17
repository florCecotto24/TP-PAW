/** 0-based home carousel page indices from URL (JSP: cheapestPage, recentPage). */

export type HomeCarouselPageKey = 'cheapestPage' | 'recentPage';

export function parseHomeCarouselPages(searchParams: URLSearchParams): {
  cheapestPage: number;
  recentPage: number;
} {
  return {
    cheapestPage: parseHomeCarouselPage(searchParams, 'cheapestPage'),
    recentPage: parseHomeCarouselPage(searchParams, 'recentPage'),
  };
}

export function parseHomeCarouselPage(
  searchParams: URLSearchParams,
  key: HomeCarouselPageKey,
): number {
  const raw = searchParams.get(key);
  if (raw == null || raw.trim() === '') return 0;
  const n = Number(raw);
  return Number.isInteger(n) && n >= 0 ? n : 0;
}

/** Writes a carousel API page index into the current URL (0 removes the param). */
export function withHomeCarouselPage(
  searchParams: URLSearchParams,
  key: HomeCarouselPageKey,
  pageIndex: number,
): URLSearchParams {
  const next = new URLSearchParams(searchParams);
  if (pageIndex <= 0) {
    next.delete(key);
  } else {
    next.set(key, String(pageIndex));
  }
  return next;
}
