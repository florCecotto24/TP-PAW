import { describe, expect, it } from 'vitest';

import {
  filtersToApiParams,
  filtersToSearchParams,
  isEmptyFilters,
  parseFilters,
} from './searchFilters';

describe('searchFilters multi-select', () => {
  it('testParseFiltersReadsRepeatedPowertrainParams', () => {
    // 1.Arrange
    const params = new URLSearchParams(
      'powertrain=gasoline&powertrain=diesel&powertrain=hybrid&powertrain=electric&powertrain=cng',
    );
    // 2.Act
    const filters = parseFilters(params);
    // 3.Assert
    expect(filters.powertrain).toEqual(['gasoline', 'diesel', 'hybrid', 'electric', 'cng']);
  });

  it('testFiltersToApiParamsEmitsRepeatedPowertrainKeys', () => {
    // 1.Arrange
    const filters = {
      powertrain: ['gasoline', 'diesel', 'hybrid'] as const,
    };
    // 2.Act
    const api = filtersToApiParams({ ...filters, powertrain: [...filters.powertrain] });
    // 3.Assert
    expect(api.powertrain).toEqual(['gasoline', 'diesel', 'hybrid']);
  });

  it('testRoundTripPreservesMultiCategoryAndNeighborhoods', () => {
    // 1.Arrange
    const original = {
      category: ['sedan', 'suv'] as const,
      neighborhoodIds: [1, 3],
      rating: ['4', '5'],
      priceMarket: ['below_market', 'at_market'] as const,
    };
    // 2.Act
    const params = filtersToSearchParams({
      category: [...original.category],
      neighborhoodIds: original.neighborhoodIds,
      rating: original.rating,
      priceMarket: [...original.priceMarket],
    });
    const parsed = parseFilters(params);
    // 3.Assert
    expect(parsed.category).toEqual(['sedan', 'suv']);
    expect(parsed.neighborhoodIds).toEqual([1, 3]);
    expect(parsed.rating).toEqual(['4', '5']);
    expect(parsed.priceMarket).toEqual(['below_market', 'at_market']);
  });

  it('testIsEmptyFiltersTreatsEmptyArraysAsEmpty', () => {
    // 2.Act / 3.Assert
    expect(isEmptyFilters({ powertrain: [] })).toBe(true);
    expect(isEmptyFilters({ powertrain: ['gasoline'] })).toBe(false);
  });
});
