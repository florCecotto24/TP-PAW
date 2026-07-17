// Sanitized admin list filters from URL search params (V-10).
// Invalid / unknown values are dropped so deep links and F5 cannot break the page
// or push garbage into API query strings.

export type AdminCarStatusFilter =
  | ''
  | 'active'
  | 'paused'
  | 'admin_paused'
  | 'lack_doc'
  | 'unavailable'
  | 'deactivated';

export type AdminRoleFilter = '' | 'user' | 'admin';
export type AdminBlockedFilter = '' | 'true' | 'false';

export interface AdminUserListFilters {
  q: string;
  role: AdminRoleFilter;
  blocked: AdminBlockedFilter;
}

export const ADMIN_CAR_STATUS_FILTERS: readonly AdminCarStatusFilter[] = [
  '',
  'active',
  'paused',
  'admin_paused',
  'lack_doc',
  'unavailable',
  'deactivated',
];

const ROLE_FILTERS: readonly AdminRoleFilter[] = ['', 'user', 'admin'];
const BLOCKED_FILTERS: readonly AdminBlockedFilter[] = ['', 'true', 'false'];

function inEnum<T extends string>(value: string | null, allowed: readonly T[]): T | undefined {
  return value != null && (allowed as readonly string[]).includes(value) ? (value as T) : undefined;
}

/** Reads `?status=` for admin cars; empty / unknown → all (`''`). */
export function parseAdminCarStatus(params: URLSearchParams): AdminCarStatusFilter {
  const raw = params.get('status');
  if (raw == null || raw.trim() === '' || raw === 'all') return '';
  return inEnum(raw.trim(), ADMIN_CAR_STATUS_FILTERS) ?? '';
}

/** Writes status into search params (omits when all). Merges; does not mutate input. */
export function withAdminCarStatus(
  params: URLSearchParams,
  status: AdminCarStatusFilter,
): URLSearchParams {
  const next = new URLSearchParams(params);
  if (!status) next.delete('status');
  else next.set('status', status);
  return next;
}

/** Reads `q` / `role` / `blocked` for admin users; invalid enums dropped. */
export function parseAdminUserFilters(params: URLSearchParams): AdminUserListFilters {
  const q = params.get('q')?.trim() ?? '';
  const role = inEnum(params.get('role'), ROLE_FILTERS) ?? '';
  const blocked = inEnum(params.get('blocked'), BLOCKED_FILTERS) ?? '';
  return { q, role, blocked };
}

/** Serializes applied user filters into URL (empty fields omitted). Merges page etc. */
export function withAdminUserFilters(
  params: URLSearchParams,
  filters: AdminUserListFilters,
): URLSearchParams {
  const next = new URLSearchParams(params);
  const q = filters.q.trim();
  if (q) next.set('q', q);
  else next.delete('q');
  if (filters.role) next.set('role', filters.role);
  else next.delete('role');
  if (filters.blocked) next.set('blocked', filters.blocked);
  else next.delete('blocked');
  return next;
}
