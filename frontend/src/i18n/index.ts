import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import es from './locales/es.json';
import en from './locales/en.json';
import { authI18n } from '../features/auth';
import { browseI18n } from '../features/browse';
import { ownerI18n } from '../features/owner';
import { reservationsI18n } from '../features/reservations';
import { profileI18n } from '../features/profile';
import { adminI18n } from '../features/admin';

// i18n 100% client-side (LINEAMIENTOS §3 / §4). Default es, fallback en.
// La preferencia del usuario logueado se persiste como atributo `latestLocale`
// (PATCH /users/{id}); para el anónimo se usa este default + estado de SPA.

export const SUPPORTED_LOCALES = ['es', 'en'] as const;
export type Locale = (typeof SUPPORTED_LOCALES)[number];

type Tree = { [key: string]: string | Tree };

/**
 * Normaliza recursos i18n a estructura anidada (keySeparator '.'). Las features
 * se autoran con formas distintas — algunas con claves planas ("auth.login.title")
 * y otras anidadas ({ browse: { home: {...} } }); esto las unifica para que
 * `t('a.b.c')` resuelva en todos los casos.
 */
function unflatten(source: Record<string, unknown>): Tree {
  const out: Tree = {};
  for (const [key, value] of Object.entries(source)) {
    const nestedValue: string | Tree =
      value !== null && typeof value === 'object'
        ? unflatten(value as Record<string, unknown>)
        : String(value);
    const parts = key.split('.');
    let node = out;
    for (let i = 0; i < parts.length - 1; i++) {
      const part = parts[i];
      if (typeof node[part] !== 'object' || node[part] === null) {
        node[part] = {};
      }
      node = node[part] as Tree;
    }
    const leaf = parts[parts.length - 1];
    const existing = node[leaf];
    if (typeof nestedValue === 'object' && typeof existing === 'object' && existing !== null) {
      node[leaf] = deepMerge(existing, nestedValue);
    } else {
      node[leaf] = nestedValue;
    }
  }
  return out;
}

function deepMerge(a: Tree, b: Tree): Tree {
  const out: Tree = { ...a };
  for (const [key, value] of Object.entries(b)) {
    const existing = out[key];
    if (typeof existing === 'object' && existing !== null && typeof value === 'object') {
      out[key] = deepMerge(existing, value as Tree);
    } else {
      out[key] = value;
    }
  }
  return out;
}

function bundle(...parts: Array<Record<string, unknown>>): Tree {
  return parts.map(unflatten).reduce(deepMerge, {} as Tree);
}

const esBundle = bundle(es, authI18n.es, browseI18n.es, ownerI18n.es, reservationsI18n.es, profileI18n.es, adminI18n.es);
const enBundle = bundle(en, authI18n.en, browseI18n.en, ownerI18n.en, reservationsI18n.en, profileI18n.en, adminI18n.en);

void i18n.use(initReactI18next).init({
  resources: {
    es: { translation: esBundle },
    en: { translation: enBundle },
  },
  lng: 'es',
  fallbackLng: 'en',
  interpolation: { escapeValue: false }, // React ya escapa
  returnNull: false,
});

export default i18n;
