import { beforeEach, describe, expect, it, vi } from 'vitest';
import { MediaTypes } from '../../api/mediaTypes';
import { sessionClient } from '../../session/sessionStore';
import type { AvailabilityDto, CarDto } from './types';
import {
  availabilityIdentity,
  deleteAvailability,
  updateAvailability,
} from './api';

type HttpOp =
  | { op: 'del'; path: string; opts: unknown }
  | { op: 'patch'; path: string; body: unknown; opts: unknown }
  | { op: 'request'; path: string; opts: unknown };

const httpOps: HttpOp[] = [];

vi.mock('../../session/sessionStore', () => ({
  sessionClient: {
    del: vi.fn(),
    patch: vi.fn(),
    request: vi.fn(),
    follow: vi.fn(),
    get: vi.fn(),
    post: vi.fn(),
  },
}));

const car = {
  links: {
    self: '/cars/7',
    availabilities: '/cars/7/availabilities',
  },
} as unknown as CarDto;

const projection: AvailabilityDto = {
  startDate: '2026-08-01',
  endDate: '2026-08-10',
  dayPrice: 100,
  startPointStreet: 'Calle',
  checkInTime: '10:00',
  checkOutTime: '20:00',
  kind: 'offered',
  links: { car: '/cars/7', neighborhood: '/neighborhoods/1' },
};

const persisted: AvailabilityDto = {
  ...projection,
  links: {
    self: '/cars/7/availabilities/3',
    car: '/cars/7',
    neighborhood: '/neighborhoods/1',
  },
};

describe('owner availability hypermedia', () => {
  beforeEach(() => {
    httpOps.length = 0;
    vi.mocked(sessionClient.del).mockReset();
    vi.mocked(sessionClient.patch).mockReset();
    vi.mocked(sessionClient.request).mockReset();
    vi.mocked(sessionClient.del).mockImplementation(async (path, opts) => {
      httpOps.push({ op: 'del', path: String(path), opts });
      return { data: undefined, status: 204, page: {}, location: null, headers: new Headers() };
    });
    vi.mocked(sessionClient.patch).mockImplementation(async (path, body, opts) => {
      httpOps.push({ op: 'patch', path: String(path), body, opts });
      return { data: persisted, status: 200, page: {}, location: null, headers: new Headers() };
    });
    vi.mocked(sessionClient.request).mockImplementation(async (path, opts) => {
      httpOps.push({ op: 'request', path: String(path), opts });
      return { data: persisted, status: 201, page: {}, location: null, headers: new Headers() };
    });
  });

  it('testAvailabilityIdentityUsesRangeWhenSelfMissing', () => {
    // 1.Arrange / 2.Act / 3.Assert
    expect(availabilityIdentity(projection)).toBe('offered:2026-08-01:2026-08-10');
    expect(availabilityIdentity(persisted)).toBe('/cars/7/availabilities/3');
  });

  it('testDeleteProjectionUsesCollectionFromUntil', async () => {
    // 1.Arrange / 2.Act
    await deleteAvailability(car, projection);
    // 3.Assert
    expect(httpOps).toEqual([
      {
        op: 'del',
        path: '/cars/7/availabilities',
        opts: {
          accept: MediaTypes.availability,
          query: { from: '2026-08-01', until: '2026-08-10' },
        },
      },
    ]);
  });

  it('testDeletePersistedUsesSelf', async () => {
    // 1.Arrange / 2.Act
    await deleteAvailability(car, persisted);
    // 3.Assert
    expect(httpOps).toEqual([
      {
        op: 'del',
        path: '/cars/7/availabilities/3',
        opts: { accept: MediaTypes.availability },
      },
    ]);
  });

  it('testUpdateProjectionDeletesThenCreates', async () => {
    // 1.Arrange
    const body = {
      startDate: '2026-08-02',
      endDate: '2026-08-11',
      dayPrice: 120,
      startPointStreet: 'Calle',
      neighborhoodUri: '/neighborhoods/1',
      checkInTime: '10:00',
      checkOutTime: '20:00',
    };
    // 2.Act
    const result = await updateAvailability(projection, car, body);
    // 3.Assert — transcript is delete + create; no patch entry
    expect(result.data).toEqual(persisted);
    expect(httpOps.map((o) => o.op)).toEqual(['del', 'request']);
    expect(httpOps[0]).toMatchObject({ op: 'del', path: '/cars/7/availabilities' });
    expect(httpOps[1]).toMatchObject({ op: 'request', path: '/cars/7/availabilities' });
  });

  it('testUpdatePersistedPatchesSelf', async () => {
    // 1.Arrange
    const body = {
      startDate: '2026-08-01',
      endDate: '2026-08-10',
      dayPrice: 110,
      startPointStreet: 'Calle',
      neighborhoodUri: '/neighborhoods/1',
      checkInTime: '10:00',
      checkOutTime: '20:00',
    };
    // 2.Act
    const result = await updateAvailability(persisted, car, body);
    // 3.Assert
    expect(result.data).toEqual(persisted);
    expect(httpOps).toEqual([
      {
        op: 'patch',
        path: '/cars/7/availabilities/3',
        body,
        opts: {
          accept: MediaTypes.availability,
          contentType: MediaTypes.availability,
        },
      },
    ]);
  });
});
