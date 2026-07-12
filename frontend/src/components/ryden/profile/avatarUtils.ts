/** Shared initials + palette for profile, review, chat and nav avatars. */

export const AVATAR_PALETTE = [
  '#6E8DB8',
  '#8E6EB8',
  '#B8896E',
  '#6EB8A8',
  '#8B9E6E',
  '#B86E8E',
  '#B8A86E',
  '#6E9EB8',
] as const;

function hashName(str: string): number {
  let h = 0;
  for (let i = 0; i < str.length; i++) {
    h = (str.charCodeAt(i) + ((h << 5) - h)) | 0;
  }
  return Math.abs(h);
}

export function initialsFromNames(forename?: string | null, surname?: string | null): string {
  const f = (forename ?? '').trim();
  const s = (surname ?? '').trim();
  const initials = `${f.charAt(0)}${s.charAt(0)}`.toUpperCase();
  return initials || '?';
}

export function avatarColorFromNames(forename?: string | null, surname?: string | null): string {
  const key = `${(forename ?? '').trim()}${(surname ?? '').trim()}`;
  return AVATAR_PALETTE[hashName(key) % AVATAR_PALETTE.length];
}
