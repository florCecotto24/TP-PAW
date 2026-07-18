import { beforeEach, describe, expect, it, vi } from 'vitest';

import { getCollectionPath } from '../../api/apiDiscovery';
import { getLinkCollectionPage } from '../../api/client';
import { MediaTypes } from '../../api/mediaTypes';
import { sessionClient } from '../../session/sessionStore';
import { listReservations, postReview } from './api';

vi.mock('../../session/sessionStore', () => ({
  sessionClient: {
    post: vi.fn(),
    get: vi.fn(),
    follow: vi.fn(),
  },
}));

vi.mock('../../api/client', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../../api/client')>();
  return {
    ...actual,
    getLinkCollectionPage: vi.fn(),
  };
});

vi.mock('../../api/apiDiscovery', () => ({
  getCollectionPath: vi.fn((name: string) => `/${name}`),
}));

describe('reservations api hypermedia', () => {
  beforeEach(() => {
    vi.mocked(sessionClient.post).mockReset();
    vi.mocked(getLinkCollectionPage).mockReset();
    vi.mocked(getCollectionPath).mockClear();
    vi.mocked(getLinkCollectionPage).mockResolvedValue({
      data: [],
      status: 200,
      page: { total: 0 },
      location: null,
      headers: new Headers(),
    });
    vi.mocked(sessionClient.post).mockResolvedValue({
      data: { rating: 5, comment: 'ok', links: { self: '/reviews/1' } },
      status: 201,
      page: {},
      location: '/reviews/1',
      headers: new Headers(),
    });
  });

  it('testListReservationsRejectsBlankLink', async () => {
    // 1.Arrange / 2.Act / 3.Assert
    await expect(listReservations({ page: 1 }, '   ')).rejects.toThrow(
      'reservations.list.missingLink',
    );
    expect(getLinkCollectionPage).not.toHaveBeenCalled();
    expect(getCollectionPath).not.toHaveBeenCalledWith('reservations');
  });

  it('testListReservationsUsesProvidedUserLinkNotGlobalCollection', async () => {
    // 1.Arrange
    const userLink = '/users/5/reservations';
    // 2.Act
    await listReservations(
      {
        riderId: 5,
        carId: 7,
        status: ['accepted'],
        page: 1,
        pageSize: 10,
      },
      userLink,
    );
    // 3.Assert
    expect(getCollectionPath).not.toHaveBeenCalledWith('reservations');
    expect(getLinkCollectionPage).toHaveBeenCalledWith(
      sessionClient,
      userLink,
      expect.objectContaining({
        collectionAccept: MediaTypes.reservationLinks,
        itemAccept: MediaTypes.reservationSummary,
        query: expect.objectContaining({
          carId: 7,
          status: ['accepted'],
          page: 1,
          pageSize: 10,
        }),
      }),
    );
    const opts = vi.mocked(getLinkCollectionPage).mock.calls[0][2] as {
      query: Record<string, unknown>;
    };
    expect(opts.query.riderId).toBeUndefined();
    expect(opts.query.ownerId).toBeUndefined();
  });

  it('testPostReviewPostsToDiscoveredReviewsCollection', async () => {
    // 1.Arrange
    const reservationUri = '/reservations/42';
    // 2.Act
    await postReview(reservationUri, { rating: 4, comment: 'bien' });
    // 3.Assert
    expect(getCollectionPath).toHaveBeenCalledWith('reviews');
    expect(sessionClient.post).toHaveBeenCalledWith(
      '/reviews',
      expect.any(Uint8Array),
      expect.objectContaining({
        accept: MediaTypes.review,
        contentType: expect.stringMatching(/^multipart\/form-data; boundary=/),
      }),
    );
  });
});
