import { useEffect, type RefObject } from 'react';

/**
 * Espejo del indicador deslizante de {@code components.js} (JSP):
 * barra azul bajo el {@code .nav-link.active}, con preview al hover.
 */
export function useNavActivePill(
  navRef: RefObject<HTMLDivElement | null>,
  /** Reposiciona cuando cambia la ruta / auth (clase {@code active}). */
  deps: readonly unknown[] = [],
): void {
  useEffect(() => {
    const navList = navRef.current;
    if (!navList) return;

    const activeLink = navList.querySelector<HTMLElement>('.nav-link.active');
    if (!activeLink) {
      navList.querySelector('.nav-active-pill')?.remove();
      return;
    }

    navList.style.position = 'relative';
    let pill = navList.querySelector<HTMLElement>('.nav-active-pill');
    if (!pill) {
      pill = document.createElement('span');
      pill.className = 'nav-active-pill';
      navList.appendChild(pill);
    }

    const moveTo = (link: HTMLElement) => {
      const nr = navList.getBoundingClientRect();
      const lr = link.getBoundingClientRect();
      pill!.style.left = `${lr.left - nr.left}px`;
      pill!.style.width = `${lr.width}px`;
    };

    moveTo(activeLink);

    const links = Array.from(navList.querySelectorAll<HTMLElement>('.nav-link'));
    const onEnter = (e: Event) => moveTo(e.currentTarget as HTMLElement);
    const onLeave = () => moveTo(activeLink);
    for (const link of links) {
      link.addEventListener('mouseenter', onEnter);
      link.addEventListener('mouseleave', onLeave);
    }

    const onResize = () => moveTo(activeLink);
    window.addEventListener('resize', onResize);

    return () => {
      for (const link of links) {
        link.removeEventListener('mouseenter', onEnter);
        link.removeEventListener('mouseleave', onLeave);
      }
      window.removeEventListener('resize', onResize);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps -- deps explícitas del caller
  }, deps);
}
