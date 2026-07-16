import { sessionClient } from '../../session/sessionStore';
import { collectionQueryPath, getCollectionPath } from '../../api/apiDiscovery';
import { MediaTypes } from '../../api/mediaTypes';
import { openAuthenticatedBinary as openAuthenticatedBinaryCore } from '../../api/openAuthenticatedBinary';
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
  return openAuthenticatedBinaryCore(link);
}

export function userDocumentPath(
  user: Pick<UserDto, 'links'>,
  type: 'license' | 'identity',
): string {
  const self = user.links?.self;
  const documents = user.links?.documents;
  const base = documents ?? (self ? `${self.replace(/\/$/, '')}/documents` : null);
  if (!base) throw new Error('admin.user.missingDocumentsLink');
  const normalized = base.endsWith('/') ? base.slice(0, -1) : base;
  return `${normalized}/${type}`;
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
  return sessionClient.get<ModelDto[]>(
    collectionQueryPath('models', { validated: 'false' }),
    { accept: MediaTypes.model },
  );
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

export interface CreateAdminUserPayload {
  forename: string;
  surname: string;
  email: string;
}

/** POST /users con Content-Type admincreateuser: crea un admin pre-verificado; set-password vía OTP. */
export async function createAdminUser(
  payload: CreateAdminUserPayload,
): Promise<ApiResponse<UserDto>> {
  return sessionClient.post<UserDto>(getCollectionPath('users'), payload, {
    accept: MediaTypes.userPrivate,
    contentType: MediaTypes.adminCreateUser,
  });
}
