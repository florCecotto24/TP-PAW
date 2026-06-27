import { describe, expect, it } from 'vitest';
import {
  filtersToParams,
  filtersToSearchParams,
  isEmptyFilters,
  parseFilters,
  type SearchFilters,
} from './searchFilters';

const sp = (q: string) => new URLSearchParams(q);

describe('parseFilters', () => {
  it('parses valid filters from the URL', () => {
    // 1.Arrange
    const params = sp(
      'q=corolla&category=sedan&transmission=automatic&powertrain=hybrid' +
        '&priceMin=10&priceMax=50&rating=4&neighborhoodId=7' +
        '&from=2026-07-01&until=2026-07-10&sort=price_asc',
    );

    // 2.Act
    const filters = parseFilters(params);

    // 3.Assert
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

  it('drops invalid enum values', () => {
    // 1.Arrange
    const params = sp('category=spaceship&transmission=warp&sort=random');

    // 2.Act
    const filters = parseFilters(params);

    // 3.Assert
    expect(filters.category).toBeUndefined();
    expect(filters.transmission).toBeUndefined();
    expect(filters.sort).toBeUndefined();
  });

  it('drops empty q and blank strings', () => {
    // 2.Act / 3.Assert
    expect(parseFilters(sp('q=')).q).toBeUndefined();
    expect(parseFilters(sp('q=%20%20')).q).toBeUndefined();
  });

  it('rejects non-numeric and negative prices', () => {
    // 1.Arrange
    const params = sp('priceMin=abc&priceMax=-5');

    // 2.Act
    const filters = parseFilters(params);

    // 3.Assert
    expect(filters.priceMin).toBeUndefined();
    expect(filters.priceMax).toBeUndefined();
  });

  it('clamps rating to 0..5', () => {
    // 2.Act / 3.Assert
    expect(parseFilters(sp('rating=9')).rating).toBeUndefined();
    expect(parseFilters(sp('rating=3.5')).rating).toBe(3.5);
  });

  it('only accepts positive integer neighborhoodId', () => {
    // 2.Act / 3.Assert
    expect(parseFilters(sp('neighborhoodId=0')).neighborhoodId).toBeUndefined();
    expect(parseFilters(sp('neighborhoodId=2.5')).neighborhoodId).toBeUndefined();
    expect(parseFilters(sp('neighborhoodId=12')).neighborhoodId).toBe(12);
  });

  it('validates ISO date format', () => {
    // 2.Act / 3.Assert
    expect(parseFilters(sp('from=2026/07/01')).from).toBeUndefined();
    expect(parseFilters(sp('from=2026-07-01')).from).toBe('2026-07-01');
  });

  it('discards an incoherent price range (min > max)', () => {
    // 1.Arrange
    const params = sp('priceMin=100&priceMax=10');

    // 2.Act
    const filters = parseFilters(params);

    // 3.Assert
    expect(filters.priceMin).toBeUndefined();
    expect(filters.priceMax).toBeUndefined();
  });
});

describe('filtersToParams', () => {
  it('omits undefined and empty values', () => {
    // 1.Arrange
    const filters: SearchFilters = { q: 'x', category: undefined, priceMin: 0, sort: undefined };

    // 2.Act
    const params = filtersToParams(filters);

    // 3.Assert
    expect(params).toEqual({ q: 'x', priceMin: '0' });
  });

  it('round-trips through the URL', () => {
    // 1.Arrange
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

    // 2.Act
    const reparsed = parseFilters(filtersToSearchParams(original));

    // 3.Assert
    expect(reparsed).toEqual(original);
  });
});

describe('isEmptyFilters', () => {
  it('is true for an empty filter set', () => {
    // 2.Act / 3.Assert
    expect(isEmptyFilters({})).toBe(true);
    expect(isEmptyFilters(parseFilters(sp('')))).toBe(true);
  });

  it('is false when any filter is set', () => {
    // 2.Act / 3.Assert
    expect(isEmptyFilters({ q: 'a' })).toBe(false);
  });
});
