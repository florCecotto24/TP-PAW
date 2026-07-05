import { describe, expect, it } from 'vitest';
import {
  CAR_VALIDATION,
  currentCarYearMax,
  firstPublishCarValidationError,
  normalizePlate,
  validateGalleryFiles,
} from './publishCarValidation';

describe('publishCarValidation', () => {
  it('testNormalizePlateStripsInvalidCharacters', () => {
    expect(normalizePlate('ab-12 3')).toBe('AB123');
  });

  it('testYearBelowMinReturnsYearMinError', () => {
    const key = firstPublishCarValidationError({
      brandSel: '/brands/1',
      modelSel: '/models/1',
      newBrandName: '',
      newModelName: '',
      plate: 'ABC123',
      year: '1800',
      description: '',
      pictures: [new File(['x'], 'a.jpg', { type: 'image/jpeg' })],
      insurance: null,
    });
    expect(key).toBe('owner.publish.errors.yearMin');
  });

  it('testYearAboveMaxReturnsYearMaxError', () => {
    const key = firstPublishCarValidationError({
      brandSel: '/brands/1',
      modelSel: '/models/1',
      newBrandName: '',
      newModelName: '',
      plate: 'ABC123',
      year: String(currentCarYearMax() + 1),
      description: '',
      pictures: [new File(['x'], 'a.jpg', { type: 'image/jpeg' })],
      insurance: null,
    });
    expect(key).toBe('owner.publish.errors.yearMax');
  });

  it('testOtherBrandRequiresOtherModelName', () => {
    const key = firstPublishCarValidationError({
      brandSel: '__other__',
      modelSel: '',
      newBrandName: 'Tesla',
      newModelName: '',
      plate: 'ABC123',
      year: '',
      description: '',
      pictures: [new File(['x'], 'a.jpg', { type: 'image/jpeg' })],
      insurance: null,
    });
    expect(key).toBe('owner.publish.errors.modelRequired');
  });

  it('testValidateGalleryFilesRequiresAtLeastOne', () => {
    expect(validateGalleryFiles([])).toBe('owner.publish.errors.picturesRequired');
  });

  it('testValidateGalleryFilesRejectsMoreThanMaxItems', () => {
    const files = Array.from({ length: CAR_VALIDATION.galleryMaxItems + 1 }, (_, i) =>
      new File(['x'], `a${i}.jpg`, { type: 'image/jpeg' }),
    );
    expect(validateGalleryFiles(files)).toBe('owner.publish.errors.picturesMax');
  });
});
