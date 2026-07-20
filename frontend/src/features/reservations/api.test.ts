import { beforeEach, describe, expect, it, vi } from 'vitest';

import { getCollectionPath } from '../../api/apiDiscovery';
import { getLinkCollectionPage } from '../../api/client';
import { MediaTypes } from '../../api/mediaTypes';
import { sessionClient } from '../../session/sessionStore';
import { listReservations, postReview } from './api';

type LinkPageCall = {
  path: string;
  opts: {
    collectionAccept?: string;
    itemAccept?: string;
    query?: Record<string, unknown>;
  };
};

type PostCall = {
  path: string;
  body: unknown;
  opts: { accept?: string; contentType?: string };
};

const linkPageCalls: LinkPageCall[] = [];
const collectionPathCalls: string[] = [];
const postCalls: PostCall[] = [];

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
  getCollectionPath: vi.fn((name: string) => {
    collectionPathCalls.push(name);
    return `/${name}`;
  }),
}));

describe('reservations api hypermedia', () => {
  beforeEach(() => {
    linkPageCalls.length = 0;
    collectionPathCalls.length = 0;
    postCalls.length = 0;
    vi.mocked(sessionClient.post).mockReset();
    vi.mocked(getLinkCollectionPage).mockReset();
    vi.mocked(getCollectionPath).mockClear();
    vi.mocked(getLinkCollectionPage).mockImplementation(async (_client, path, opts) => {
      linkPageCalls.push({ path: String(path), opts: opts ?? {} });
      return {
        data: [],
        status: 200,
        page: { total: 0 },
        location: null,
        headers: new Headers(),
      };
    });
    vi.mocked(sessionClient.post).mockImplementation(async (path, body, opts) => {
      postCalls.push({ path: String(path), body, opts: opts ?? {} });
      return {
        data: { rating: 5, comment: 'ok', links: { self: '/reviews/1' } },
        status: 201,
        page: {},
        location: '/reviews/1',
        headers: new Headers(),
      };
    });
  });

  it('testListReservationsRejectsBlankLink', async () => {
    // 1.Arrange / 2.Act / 3.Assert — the thrown error is the observable contract
    await expect(listReservations({ page: 1 }, '   ')).rejects.toThrow(
      'reservations.list.missingLink',
    );
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
    // 3.Assert — follow user link, never invent global /reservations
    expect(collectionPathCalls).toEqual([]);
    expect(linkPageCalls).toHaveLength(1);
    expect(linkPageCalls[0].path).toBe(userLink);
    expect(linkPageCalls[0].opts.collectionAccept).toBe(MediaTypes.reservationLinks);
    expect(linkPageCalls[0].opts.itemAccept).toBe(MediaTypes.reservationSummary);
    expect(linkPageCalls[0].opts.query).toMatchObject({
      carId: 7,
      status: ['accepted'],
      page: 1,
      pageSize: 10,
    });
    expect(linkPageCalls[0].opts.query?.riderId).toBeUndefined();
    expect(linkPageCalls[0].opts.query?.ownerId).toBeUndefined();
  });

  it('testPostReviewPostsToDiscoveredReviewsCollection', async () => {
    // 1.Arrange
    const reservationUri = '/reservations/42';
    // 2.Act
    await postReview(reservationUri, { rating: 4, comment: 'bien' });
    // 3.Assert
    expect(collectionPathCalls).toEqual(['reviews']);
    expect(postCalls).toHaveLength(1);
    expect(postCalls[0].path).toBe('/reviews');
    expect(postCalls[0].body).toBeInstanceOf(Uint8Array);
    expect(postCalls[0].opts.accept).toBe(MediaTypes.review);
    expect(postCalls[0].opts.contentType).toMatch(/^multipart\/form-data; boundary=/);
  });
});
