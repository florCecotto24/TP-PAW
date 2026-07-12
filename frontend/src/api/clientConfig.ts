import { create } from 'zustand';

import type { HypermediaClient } from './client';
import { MediaTypes } from './mediaTypes';
import { hrefToRelativeApiPath } from './uri';

/**
 * Public SPA limits. {@link CLIENT_CONFIG_FALLBACK} mirrors {@code application.properties}
 * when {@code GET /config} is unavailable at boot.
 */
export interface ClientConfigCarLimits {
  brandMinLength: number;
  brandMaxLength: number;
  modelMaxLength: number;
  plateMinLength: number;
  plateMaxLength: number;
  descriptionMaxLength: number;
  yearMin: number;
  galleryMaxItems: number;
}

export interface ClientConfigUploadLimits {
  maxImageMegabytes: number;
  maxCarGalleryVideoMegabytes: number;
  maxProfileDocumentMegabytes: number;
  maxPaymentReceiptMegabytes: number;
}

export interface ClientConfigMoneyLimits {
  currency: string;
  formatLocale: string;
}

export interface ClientConfigUserLimits {
  displayNamePartMaxLength: number;
  profileAboutMaxLength: number;
  registrationPasswordMinLength: number;
  registrationPasswordMaxLength: number;
  registrationEmailMaxLength: number;
  profilePhoneMaxLength: number;
}

export interface ClientConfigChatLimits {
  maxAttachmentMegabytes: number;
  messageMaxLength: number;
  historyPageSize: number;
}

export interface ClientConfigReviewLimits {
  commentMaxLength: number;
}

export interface ClientConfigListingLimits {
  pricePerDayMin: number;
  addressStreetMaxLength: number;
  addressNumberMaxLength: number;
  pricePerDayIntegerDigits: number;
  pricePerDayFractionDigits: number;
}

export interface ClientConfigDto {
  cbuRequiredDigits: number;
  maxBillableDays: number;
  car: ClientConfigCarLimits;
  upload: ClientConfigUploadLimits;
  money: ClientConfigMoneyLimits;
  user: ClientConfigUserLimits;
  chat: ClientConfigChatLimits;
  review: ClientConfigReviewLimits;
  listing: ClientConfigListingLimits;
}

/** Documented JVM fallbacks aligned with application.properties defaults. */
export const CLIENT_CONFIG_FALLBACK: ClientConfigDto = {
  cbuRequiredDigits: 22,
  maxBillableDays: 30,
  car: {
    brandMinLength: 2,
    brandMaxLength: 30,
    modelMaxLength: 30,
    plateMinLength: 6,
    plateMaxLength: 10,
    descriptionMaxLength: 200,
    yearMin: 1886,
    galleryMaxItems: 8,
  },
  upload: {
    maxImageMegabytes: 20,
    maxCarGalleryVideoMegabytes: 25,
    maxProfileDocumentMegabytes: 5,
    maxPaymentReceiptMegabytes: 5,
  },
  money: {
    currency: 'ARS',
    formatLocale: 'es-AR',
  },
  user: {
    displayNamePartMaxLength: 50,
    profileAboutMaxLength: 500,
    registrationPasswordMinLength: 8,
    registrationPasswordMaxLength: 72,
    registrationEmailMaxLength: 50,
    profilePhoneMaxLength: 20,
  },
  chat: {
    maxAttachmentMegabytes: 25,
    messageMaxLength: 1000,
    historyPageSize: 12,
  },
  review: {
    commentMaxLength: 200,
  },
  listing: {
    pricePerDayMin: 0.01,
    addressStreetMaxLength: 250,
    addressNumberMaxLength: 10,
    pricePerDayIntegerDigits: 8,
    pricePerDayFractionDigits: 2,
  },
};

interface ClientConfigState {
  ready: boolean;
  config: ClientConfigDto | null;
  load: (client: HypermediaClient, configLink?: string | null) => Promise<void>;
}

export const useClientConfigStore = create<ClientConfigState>((set, get) => ({
  ready: false,
  config: null,
  load: async (client, configLink) => {
    if (get().ready) {
      return;
    }
    const path = configLink ? hrefToRelativeApiPath(configLink) : '/config';
    try {
      const res = await client.get<ClientConfigDto>(path, {
        accept: MediaTypes.config,
        anonymous: true,
      });
      set({ config: res.data, ready: true });
    } catch {
      set({ config: null, ready: true });
    }
  },
}));

export function getClientConfig(): ClientConfigDto {
  return useClientConfigStore.getState().config ?? CLIENT_CONFIG_FALLBACK;
}

export function megabytesToBytes(megabytes: number): number {
  return megabytes * 1024 * 1024;
}

export function getMaxReceiptBytes(): number {
  return megabytesToBytes(getClientConfig().upload.maxPaymentReceiptMegabytes);
}
