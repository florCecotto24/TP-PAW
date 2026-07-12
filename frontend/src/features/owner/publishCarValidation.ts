import { getClientConfig, megabytesToBytes } from '../../api/clientConfig';

export function currentCarYearMax(): number {
  return new Date().getFullYear();
}

function carLimits() {
  return getClientConfig().car;
}

function uploadLimits() {
  return getClientConfig().upload;
}

export function normalizePlate(raw: string): string {
  return raw.toUpperCase().replace(/[^A-Z0-9]/g, '').slice(0, carLimits().plateMaxLength);
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
  const limits = carLimits();
  const yearRaw = year.trim();
  if (!yearRaw) return null;
  if (!/^\d{1,4}$/.test(yearRaw)) return 'owner.publish.errors.yearInvalid';
  const yearNum = Number(yearRaw);
  if (yearNum < limits.yearMin) return 'owner.publish.errors.yearMin';
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
  const limits = carLimits();
  const upload = uploadLimits();
  const maxImageBytes = megabytesToBytes(upload.maxImageMegabytes);
  const maxVideoBytes = megabytesToBytes(upload.maxCarGalleryVideoMegabytes);
  const maxInsuranceBytes = megabytesToBytes(upload.maxProfileDocumentMegabytes);
  const OTHER = '__other__';

  if (!input.brandSel) return 'owner.publish.errors.brandRequired';

  if (input.brandSel === OTHER) {
    const brand = input.newBrandName.trim();
    if (!brand) return 'owner.publish.errors.brandRequired';
    if (brand.length < limits.brandMinLength || brand.length > limits.brandMaxLength) {
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
    if (model.length > limits.modelMaxLength) return 'owner.publish.errors.modelSize';
  }

  const plate = normalizePlate(input.plate);
  if (!plate) return 'owner.publish.errors.plateRequired';
  if (plate.length < limits.plateMinLength || plate.length > limits.plateMaxLength) {
    return 'owner.publish.errors.plateSize';
  }

  if (input.description.trim().length > limits.descriptionMaxLength) {
    return 'owner.publish.errors.descriptionSize';
  }

  const galleryError = validateGalleryFiles(input.pictures, maxImageBytes, maxVideoBytes);
  if (galleryError) return galleryError;

  const insuranceError = validateInsuranceFile(input.insurance, maxInsuranceBytes);
  if (insuranceError) return insuranceError;

  return null;
}

export function validateGalleryFiles(
  files: File[],
  maxImageBytes = megabytesToBytes(uploadLimits().maxImageMegabytes),
  maxVideoBytes = megabytesToBytes(uploadLimits().maxCarGalleryVideoMegabytes),
): string | null {
  const galleryMaxItems = carLimits().galleryMaxItems;
  if (files.length === 0) return 'owner.publish.errors.picturesRequired';
  if (files.length > galleryMaxItems) return 'owner.publish.errors.picturesMax';

  for (const file of files) {
    if (!isAllowedGalleryMedia(file)) return 'owner.publish.errors.notGalleryMedia';
    if (isImageFile(file)) {
      if (file.size > maxImageBytes) return 'owner.publish.errors.imageTooLarge';
    } else if (file.size > maxVideoBytes) {
      return 'owner.publish.errors.videoTooLarge';
    }
  }
  return null;
}

export function validateInsuranceFile(
  file: File | null,
  maxInsuranceBytes = megabytesToBytes(uploadLimits().maxProfileDocumentMegabytes),
): string | null {
  if (!file) return null;
  if (file.size > maxInsuranceBytes) return 'owner.publish.errors.insuranceTooLarge';
  return null;
}

export function publishValidationI18nParams() {
  const limits = carLimits();
  const upload = uploadLimits();
  return {
    min: limits.yearMin,
    max: currentCarYearMax(),
    brandMin: limits.brandMinLength,
    brandMax: limits.brandMaxLength,
    modelMax: limits.modelMaxLength,
    plateMin: limits.plateMinLength,
    plateMax: limits.plateMaxLength,
    descriptionMax: limits.descriptionMaxLength,
    galleryMax: limits.galleryMaxItems,
    imageMaxMb: upload.maxImageMegabytes,
    videoMaxMb: upload.maxCarGalleryVideoMegabytes,
    insuranceMaxMb: upload.maxProfileDocumentMegabytes,
  };
}

/** UI maxlength / size hints for the publish form (from client config). */
export function carValidationLimits() {
  const limits = carLimits();
  const upload = uploadLimits();
  return {
    brandMaxLength: limits.brandMaxLength,
    modelMaxLength: limits.modelMaxLength,
    plateMaxLength: limits.plateMaxLength,
    descriptionMaxLength: limits.descriptionMaxLength,
    maxInsuranceBytes: megabytesToBytes(upload.maxProfileDocumentMegabytes),
  };
}
