/** Límites alineados con `application.properties` / `CarValidationPolicy` / legacy JSP. */
export const CAR_VALIDATION = {
  brandMinLength: 2,
  brandMaxLength: 30,
  modelMaxLength: 30,
  plateMinLength: 6,
  plateMaxLength: 10,
  descriptionMaxLength: 200,
  yearMin: 1886,
  galleryMaxItems: 8,
  maxImageBytes: 20 * 1024 * 1024,
  maxVideoBytes: 25 * 1024 * 1024,
  maxInsuranceBytes: 5 * 1024 * 1024,
} as const;

export function currentCarYearMax(): number {
  return new Date().getFullYear();
}

export function normalizePlate(raw: string): string {
  return raw.toUpperCase().replace(/[^A-Z0-9]/g, '').slice(0, CAR_VALIDATION.plateMaxLength);
}

export function normalizeYearDigits(raw: string): string {
  return raw.replace(/\D/g, '').slice(0, 4);
}

/**
 * Validación del año de fabricación (opcional), alineada con
 * {@code CarCreateFormValidator} / mensajes {@code validation.year.min|max}.
 * Vacío = OK. Devuelve clave i18n o null.
 */
export function validatePublishCarYear(year: string): string | null {
  const yearRaw = year.trim();
  if (!yearRaw) return null;
  if (!/^\d{1,4}$/.test(yearRaw)) return 'owner.publish.errors.yearInvalid';
  const yearNum = Number(yearRaw);
  if (yearNum < CAR_VALIDATION.yearMin) return 'owner.publish.errors.yearMin';
  if (yearNum > currentCarYearMax()) return 'owner.publish.errors.yearMax';
  return null;
}

function isAllowedGalleryMedia(file: File): boolean {
  const type = (file.type || '').toLowerCase();
  return (
    type.startsWith('image/')
    || type === 'video/mp4'
    || type === 'video/webm'
    || type === 'video/quicktime'
  );
}

function isImageFile(file: File): boolean {
  return (file.type || '').toLowerCase().startsWith('image/');
}

export type PublishCarValidationInput = {
  brandSel: string;
  modelSel: string;
  newBrandName: string;
  newModelName: string;
  plate: string;
  year: string;
  description: string;
  pictures: File[];
  insurance: File | null;
};

/** Devuelve la clave i18n (`owner.publish.errors.*`) del primer error encontrado, o null si OK. */
export function firstPublishCarValidationError(input: PublishCarValidationInput): string | null {
  const OTHER = '__other__';

  if (!input.brandSel) return 'owner.publish.errors.brandRequired';

  if (input.brandSel === OTHER) {
    const brand = input.newBrandName.trim();
    if (!brand) return 'owner.publish.errors.brandRequired';
    if (brand.length < CAR_VALIDATION.brandMinLength
      || brand.length > CAR_VALIDATION.brandMaxLength) {
      return 'owner.publish.errors.brandSize';
    }
    if (input.modelSel !== OTHER || !input.newModelName.trim()) {
      return 'owner.publish.errors.modelRequired';
    }
  } else if (!input.modelSel) {
    return 'owner.publish.errors.modelSelectRequired';
  }

  if (input.modelSel === OTHER) {
    const model = input.newModelName.trim();
    if (!model) return 'owner.publish.errors.modelRequired';
    if (model.length > CAR_VALIDATION.modelMaxLength) return 'owner.publish.errors.modelSize';
  }

  const plate = normalizePlate(input.plate);
  if (!plate) return 'owner.publish.errors.plateRequired';
  if (plate.length < CAR_VALIDATION.plateMinLength
    || plate.length > CAR_VALIDATION.plateMaxLength) {
    return 'owner.publish.errors.plateSize';
  }

  // El año se valida aparte ({@link validatePublishCarYear}) y se muestra bajo el
  // campo, como `form:errors path="year"` en publishCarForm.jsp.

  if (input.description.trim().length > CAR_VALIDATION.descriptionMaxLength) {
    return 'owner.publish.errors.descriptionSize';
  }

  const galleryError = validateGalleryFiles(input.pictures);
  if (galleryError) return galleryError;

  const insuranceError = validateInsuranceFile(input.insurance);
  if (insuranceError) return insuranceError;

  return null;
}

export function validateGalleryFiles(files: File[]): string | null {
  if (files.length === 0) return 'owner.publish.errors.picturesRequired';
  if (files.length > CAR_VALIDATION.galleryMaxItems) return 'owner.publish.errors.picturesMax';

  for (const file of files) {
    if (!isAllowedGalleryMedia(file)) return 'owner.publish.errors.notGalleryMedia';
    if (isImageFile(file)) {
      if (file.size > CAR_VALIDATION.maxImageBytes) return 'owner.publish.errors.imageTooLarge';
    } else if (file.size > CAR_VALIDATION.maxVideoBytes) {
      return 'owner.publish.errors.videoTooLarge';
    }
  }
  return null;
}

export function validateInsuranceFile(file: File | null): string | null {
  if (!file) return null;
  if (file.size > CAR_VALIDATION.maxInsuranceBytes) return 'owner.publish.errors.insuranceTooLarge';
  return null;
}

export function publishValidationI18nParams() {
  return {
    min: CAR_VALIDATION.yearMin,
    max: currentCarYearMax(),
    brandMin: CAR_VALIDATION.brandMinLength,
    brandMax: CAR_VALIDATION.brandMaxLength,
    modelMax: CAR_VALIDATION.modelMaxLength,
    plateMin: CAR_VALIDATION.plateMinLength,
    plateMax: CAR_VALIDATION.plateMaxLength,
    descriptionMax: CAR_VALIDATION.descriptionMaxLength,
    galleryMax: CAR_VALIDATION.galleryMaxItems,
    imageMaxMb: 20,
    videoMaxMb: 25,
    insuranceMaxMb: 5,
  };
}
