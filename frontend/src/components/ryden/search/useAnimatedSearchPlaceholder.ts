import { useEffect, useState, type RefObject } from 'react';

/** Mismos ejemplos que {@code components.js} (JSP animated search placeholder). */
export const SEARCH_PLACEHOLDER_EXAMPLES = [
  'BMW X5...',
  'Volkswagen Golf...',
  'Toyota Corolla...',
  'Ford Ranger...',
  'Chevrolet Onix...',
  'Honda Civic...',
] as const;

const ROTATE_MS = 2800;

/**
 * Rota placeholders de ejemplo en el input de búsqueda (espejo del JSP).
 * Se detiene con focus / texto; reanuda al blur si el campo queda vacío.
 */
export function useAnimatedSearchPlaceholder(
  inputRef: RefObject<HTMLInputElement | null>,
  value: string,
): { placeholder: string; animating: boolean } {
  const [index, setIndex] = useState(0);
  const [animating, setAnimating] = useState(() => !value.trim());
  const [focused, setFocused] = useState(false);

  useEffect(() => {
    const hasValue = Boolean(value.trim());
    setAnimating(!hasValue && !focused);
  }, [value, focused]);

  useEffect(() => {
    const input = inputRef.current;
    if (!input) return;

    const onFocus = () => setFocused(true);
    const onBlur = () => setFocused(false);
    input.addEventListener('focus', onFocus);
    input.addEventListener('blur', onBlur);
    return () => {
      input.removeEventListener('focus', onFocus);
      input.removeEventListener('blur', onBlur);
    };
  }, [inputRef]);

  useEffect(() => {
    if (!animating) return;
    const timer = window.setInterval(() => {
      setIndex((i) => (i + 1) % SEARCH_PLACEHOLDER_EXAMPLES.length);
    }, ROTATE_MS);
    return () => window.clearInterval(timer);
  }, [animating]);

  const placeholder = animating ? SEARCH_PLACEHOLDER_EXAMPLES[index] : ' ';

  return { placeholder, animating };
}
