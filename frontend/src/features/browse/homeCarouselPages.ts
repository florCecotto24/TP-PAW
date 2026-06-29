/** 0-based home carousel page indices from URL (JSP: cheapestPage, recentPage). */

export function parseHomeCarouselPages(searchParams: URLSearchParams): {
  cheapestPage: number;
  recentPage: number;
} {
  const cheapestRaw = Number(searchParams.get('cheapestPage') ?? '0');
  const recentRaw = Number(searchParams.get('recentPage') ?? '0');
  return {
    cheapestPage:
      Number.isFinite(cheapestRaw) && cheapestRaw >= 0 ? Math.floor(cheapestRaw) : 0,
    recentPage: Number.isFinite(recentRaw) && recentRaw >= 0 ? Math.floor(recentRaw) : 0,
  };
}

export function buildHomeCarouselHref(
  searchParams: URLSearchParams,
  carousel: 'cheapest' | 'recent',
  page: number,
): string {
  const params = new URLSearchParams(searchParams);
  const key = carousel === 'cheapest' ? 'cheapestPage' : 'recentPage';
  if (page <= 0) {
    params.delete(key);
  } else {
    params.set(key, String(page));
  }
  const qs = params.toString();
  return qs ? `/?${qs}` : '/';
}
