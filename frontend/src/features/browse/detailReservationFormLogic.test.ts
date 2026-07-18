import { describe, expect, it } from 'vitest';

import { initialDefaultDates, type BookableSegment } from './detailReservationFormLogic';

describe('detailReservationFormLogic', () => {
  const segments: BookableSegment[] = [
    {
      from: '2026-08-01',
      to: '2026-08-05',
      dayPrice: 100,
      checkInTime: '10:00',
      checkOutTime: '20:00',
      location: 'Palermo',
      neighborhoodUri: '/neighborhoods/3',
    },
    {
      from: '2026-09-01',
      to: '2026-09-03',
      dayPrice: 120,
      checkInTime: '10:00',
      checkOutTime: '20:00',
      location: 'Recoleta',
      neighborhoodUri: '/api/neighborhoods/9?x=1',
    },
  ];

  it('testInitialDefaultDatesReturnsEmptyWhenNoSearchNeighborhoods', () => {
    // 2.Act / 3.Assert
    expect(initialDefaultDates(segments, [])).toEqual([]);
  });

  it('testInitialDefaultDatesMatchesNeighborhoodByIdFromUri', () => {
    // 1.Arrange / 2.Act
    const dates = initialDefaultDates(segments, [9]);
    // 3.Assert — idFromUri strips query; does not rely on endsWith path shape
    expect(dates).toHaveLength(2);
    expect(dates[0].getFullYear()).toBe(2026);
    expect(dates[0].getMonth()).toBe(8); // September
    expect(dates[0].getDate()).toBe(1);
    expect(dates[1].getDate()).toBe(3);
  });

  it('testInitialDefaultDatesReturnsEmptyWhenNoSegmentMatches', () => {
    // 2.Act / 3.Assert
    expect(initialDefaultDates(segments, [99])).toEqual([]);
  });
});
