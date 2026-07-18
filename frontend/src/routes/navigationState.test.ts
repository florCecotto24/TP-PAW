import { describe, expect, it } from 'vitest';

import {
  carDetailTo,
  isAppLinkTarget,
  myCarDetailTo,
  publicProfileTo,
  selfQueryValue,
} from './navigationState';

describe('navigationState', () => {
  it('testSelfQueryValueNormalizesAbsoluteApiHref', () => {
    // 1.Arrange / 2.Act / 3.Assert
    expect(selfQueryValue('/cars/7')).toBe('/cars/7');
    expect(selfQueryValue('/api/cars/7')).toBe('/cars/7');
  });

  it('testCarDetailToWithoutSelfHasPathnameOnly', () => {
    // 1.Arrange / 2.Act
    const target = carDetailTo(7);
    // 3.Assert
    expect(target).toEqual({ pathname: '/cars/7' });
    expect(target.state).toBeUndefined();
  });

  it('testCarDetailToMergesSelfQueryAndState', () => {
    // 1.Arrange / 2.Act
    const target = carDetailTo(7, '/cars/7', { src: 'search' });
    // 3.Assert
    expect(target.pathname).toContain('/cars/7?');
    expect(target.pathname).toContain('src=search');
    expect(target.pathname).toContain('self=%2Fcars%2F7');
    expect(target.state).toEqual({ carSelf: '/cars/7' });
  });

  it('testMyCarDetailToCarriesOwnerSelf', () => {
    // 1.Arrange / 2.Act
    const target = myCarDetailTo(3, '/cars/3');
    // 3.Assert
    expect(target.pathname).toContain('/my-cars/car/3?');
    expect(target.pathname).toContain('self=%2Fcars%2F3');
    expect(target.state).toEqual({ carSelf: '/cars/3' });
  });

  it('testPublicProfileToMergesCarIdQueryAndUserSelf', () => {
    // 1.Arrange / 2.Act
    const target = publicProfileTo(9, '/users/9', { carId: '7' });
    // 3.Assert
    expect(target.pathname).toContain('/users/9/profile?');
    expect(target.pathname).toContain('carId=7');
    expect(target.pathname).toContain('self=%2Fusers%2F9');
    expect(target.state).toEqual({ userSelf: '/users/9' });
  });

  it('testIsAppLinkTargetAcceptsPathnameShape', () => {
    // 2.Act / 3.Assert
    expect(isAppLinkTarget({ pathname: '/cars/1', state: { carSelf: '/cars/1' } })).toBe(true);
    expect(isAppLinkTarget('/cars/1')).toBe(false);
    expect(isAppLinkTarget({ path: '/cars/1' })).toBe(false);
  });
});
