import { sessionClient, useSessionStore } from '../../session/sessionStore';
import { appBasePath } from '../../appBasePath';
import { MediaTypes } from '../../api/mediaTypes';
import type { ApiResponse } from '../../api/client';
import type { UserDto } from '../../api/types';
import type { BrandDto, CarDto, CarStatus, ModelDto } from './types';

export type AdminUserPatch = Partial<{
  role: 'user' | 'admin';
  blocked: boolean;
  licenseValidated: boolean;
  identityValidated: boolean;
}>;

export async function patchUser(
  userSelfLink: string,
  body: AdminUserPatch,
): Promise<ApiResponse<UserDto>> {
  return sessionClient.patch<UserDto>(userSelfLink, body, {
    accept: MediaTypes.userPrivate,
    contentType: MediaTypes.user,
  });
}

/** Abre un recurso binario autenticado (documento KYC, adjunto de chat). */
export async function openAuthenticatedBinary(link: string): Promise<boolean> {
  const token = useSessionStore.getState().accessToken;
  const path = link.startsWith('http')
    ? link
    : `${appBasePath()}${link.startsWith('/') ? link : `/${link}`}`;
  const res = await fetch(path, {
    headers: token ? { Authorization: `Bearer ${token}` } : undefined,
  });
  if (!res.ok) return false;
  const blob = await res.blob();
  const objectUrl = URL.createObjectURL(blob);
  const win = window.open(objectUrl, '_blank', 'noopener,noreferrer');
  if (!win) {
    URL.revokeObjectURL(objectUrl);
    return false;
  }
  setTimeout(() => URL.revokeObjectURL(objectUrl), 60_000);
  return true;
}

export function userDocumentPath(userSelfLink: string, type: 'license' | 'identity'): string {
  const base = userSelfLink.endsWith('/') ? userSelfLink.slice(0, -1) : userSelfLink;
  return `${base}/documents/${type}`;
}

export async function approveBrand(brandSelfLink: string): Promise<ApiResponse<BrandDto>> {
  return sessionClient.patch<BrandDto>(
    brandSelfLink,
    { validated: true },
    { accept: MediaTypes.brand, contentType: MediaTypes.brand },
  );
}

export async function rejectBrand(brandSelfLink: string): Promise<ApiResponse<void>> {
  return sessionClient.del(brandSelfLink);
}

export async function approveModel(modelSelfLink: string): Promise<ApiResponse<ModelDto>> {
  return sessionClient.patch<ModelDto>(
    modelSelfLink,
    { validated: true },
    { accept: MediaTypes.model, contentType: MediaTypes.model },
  );
}

export async function rejectModel(modelSelfLink: string): Promise<ApiResponse<void>> {
  return sessionClient.del(modelSelfLink);
}

export async function fetchPendingModels(): Promise<ApiResponse<ModelDto[]>> {
  return sessionClient.get<ModelDto[]>('/models?validated=false', { accept: MediaTypes.model });
}

export async function patchCarStatus(
  carSelfLink: string,
  status: Extract<CarStatus, 'admin_paused' | 'active'>,
): Promise<ApiResponse<CarDto>> {
  return sessionClient.patch<CarDto>(carSelfLink, { status }, {
    accept: MediaTypes.car,
    contentType: MediaTypes.car,
  });
}

export async function fetchUserPublic(userUri: string): Promise<ApiResponse<UserDto>> {
  return sessionClient.get<UserDto>(userUri, { accept: MediaTypes.user });
}
