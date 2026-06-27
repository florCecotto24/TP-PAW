import { describe, expect, it } from 'vitest';
import i18n from './index';

function leafKeys(obj: Record<string, unknown>, prefix = ''): string[] {
  const keys: string[] = [];
  for (const [k, v] of Object.entries(obj)) {
    const path = prefix ? `${prefix}.${k}` : k;
    if (v !== null && typeof v === 'object') {
      keys.push(...leafKeys(v as Record<string, unknown>, path));
    } else {
      keys.push(path);
    }
  }
  return keys;
}

describe('i18n es/en parity', () => {
  const es = i18n.getResourceBundle('es', 'translation') as Record<string, unknown>;
  const en = i18n.getResourceBundle('en', 'translation') as Record<string, unknown>;

  it('tiene exactamente el mismo conjunto de claves en es y en', () => {
    // 1.Arrange
    const esKeys = new Set(leafKeys(es));
    const enKeys = new Set(leafKeys(en));

    // 2.Act
    const missingInEn = [...esKeys].filter((k) => !enKeys.has(k)).sort();
    const missingInEs = [...enKeys].filter((k) => !esKeys.has(k)).sort();

    // 3.Assert
    expect({ missingInEn, missingInEs }).toEqual({ missingInEn: [], missingInEs: [] });
  });
});
