import { describe, expect, it } from 'vitest';
import {
  filtersToApiParams,
  filtersToSearchParams,
  isEmptyFilters,
  parseFilters,
  type SearchFilters,
} from './searchFilters';

const sp = (q: string) => new URLSearchParams(q);

describe('parseFilters', () => {
  it('parses valid filters from the URL (JSP query param)', () => {
    const params = sp(
      'query=corolla&category=sedan&transmission=automatic&powertrain=hybrid' +
        '&priceMin=10&priceMax=50&rating=4&neighborhoodId=7' +
        '&from=2026-07-01&until=2026-07-10&sort=price,asc',
    );

    const filters = parseFilters(params);

    expect(filters).toEqual<SearchFilters>({
      q: 'corolla',
      category: 'sedan',
      transmission: 'automatic',
      powertrain: 'hybrid',
      priceMin: 10,
      priceMax: 50,
      rating: 4,
      neighborhoodId: 7,
      from: '2026-07-01',
      until: '2026-07-10',
      sort: 'price_asc',
    });
  });

  it('accepts legacy q param and API sort values', () => {
    const params = sp('q=civic&sort=price_asc');
    const filters = parseFilters(params);
    expect(filters.q).toBe('civic');
    expect(filters.sort).toBe('price_asc');
  });

  it('parses flexible date filters', () => {
    const filters = parseFilters(sp('flexible=true&flexMonth=2026-08&flexDays=5'));
    expect(filters.flexible).toBe(true);
    expect(filters.flexMonth).toBe('2026-08');
    expect(filters.flexDays).toBe(5);
    expect(filters.from).toBeUndefined();
    expect(filters.until).toBeUndefined();
  });

  it('serializes flexible filters to URL', () => {
    const params = filtersToSearchParams({
      flexible: true,
      flexMonth: '2026-09',
      flexDays: 3,
    });
    expect(params.get('flexible')).toBe('true');
    expect(params.get('flexMonth')).toBe('2026-09');
    expect(params.get('flexDays')).toBe('3');
    expect(params.has('from')).toBe(false);
  });

  it('drops invalid enum values', () => {
    const params = sp('category=spaceship&transmission=warp&sort=random');

    const filters = parseFilters(params);

    expect(filters.category).toBeUndefined();
    expect(filters.transmission).toBeUndefined();
    expect(filters.sort).toBe('recent');
  });

  it('drops empty query and blank strings', () => {
    expect(parseFilters(sp('query=')).q).toBeUndefined();
    expect(parseFilters(sp('query=%20%20')).q).toBeUndefined();
  });

  it('rejects non-numeric and negative prices', () => {
    const params = sp('priceMin=abc&priceMax=-5');

    const filters = parseFilters(params);

    expect(filters.priceMin).toBeUndefined();
    expect(filters.priceMax).toBeUndefined();
  });

  it('clamps rating to 0..5', () => {
    expect(parseFilters(sp('rating=9')).rating).toBeUndefined();
    expect(parseFilters(sp('rating=3.5')).rating).toBe(3.5);
  });

  it('only accepts positive integer neighborhoodId', () => {
    expect(parseFilters(sp('neighborhoodId=0')).neighborhoodId).toBeUndefined();
    expect(parseFilters(sp('neighborhoodId=2.5')).neighborhoodId).toBeUndefined();
    expect(parseFilters(sp('neighborhoodId=12')).neighborhoodId).toBe(12);
  });

  it('validates ISO date format', () => {
    expect(parseFilters(sp('from=2026/07/01')).from).toBeUndefined();
    expect(parseFilters(sp('from=2026-07-01')).from).toBe('2026-07-01');
  });

  it('discards an incoherent price range (min > max)', () => {
    const params = sp('priceMin=100&priceMax=10');

    const filters = parseFilters(params);

    expect(filters.priceMin).toBeUndefined();
    expect(filters.priceMax).toBeUndefined();
  });
});

describe('filtersToApiParams', () => {
  it('omits undefined and empty values', () => {
    const filters: SearchFilters = { q: 'x', category: undefined, priceMin: 0, sort: undefined };

    const params = filtersToApiParams(filters);

    expect(params).toEqual({ q: 'x', priceMin: '0' });
  });
});

describe('filtersToSearchParams', () => {
  it('serializes JSP-style URL params', () => {
    const original: SearchFilters = {
      q: 'civic',
      category: 'hatchback',
      priceMin: 5,
      priceMax: 80,
      rating: 4.5,
      neighborhoodId: 3,
      from: '2026-08-01',
      until: '2026-08-05',
      sort: 'rating_desc',
    };

    const url = filtersToSearchParams(original);
    expect(url.get('query')).toBe('civic');
    expect(url.get('sort')).toBe('rating,desc');
    expect(url.get('q')).toBeNull();
  });

  it('round-trips through the URL', () => {
    const original: SearchFilters = {
      q: 'civic',
      category: 'hatchback',
      priceMin: 5,
      priceMax: 80,
      rating: 4.5,
      neighborhoodId: 3,
      from: '2026-08-01',
      until: '2026-08-05',
      sort: 'rating_desc',
    };

    const reparsed = parseFilters(filtersToSearchParams(original));

    expect(reparsed).toEqual(original);
  });
});

describe('isEmptyFilters', () => {
  it('is true for an empty filter set', () => {
    expect(isEmptyFilters({})).toBe(true);
    expect(isEmptyFilters(parseFilters(sp('')))).toBe(true);
  });

  it('is false when any filter is set', () => {
    expect(isEmptyFilters({ q: 'a' })).toBe(false);
  });
});
