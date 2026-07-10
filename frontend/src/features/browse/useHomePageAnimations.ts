import { useEffect } from 'react';


export function useHomePageAnimations(contentRevision = 0): void {
  useEffect(() => {
    const navbar = document.querySelector('.navbar');
    const onNavbarScroll = () => {
      navbar?.classList.toggle('navbar--scrolled', window.scrollY > 30);
    };
    window.addEventListener('scroll', onNavbarScroll, { passive: true });
    onNavbarScroll();

    const heroSection = document.querySelector('.hero-section') as HTMLElement | null;
    const onHeroScroll = () => {
      if (!heroSection) return;
      const scrolled = window.scrollY;
      if (scrolled < window.innerHeight * 1.5) {
        heroSection.style.setProperty('--parallax-y', `${scrolled * 0.22}px`);
      }
    };
    window.addEventListener('scroll', onHeroScroll, { passive: true });

    const revealHidden = () => {
      document.querySelectorAll('.animate-on-scroll:not(.is-visible)').forEach((el) => {
        el.classList.add('is-visible');
      });
      document.querySelectorAll('.animate-stagger-child:not(.is-visible)').forEach((el) => {
        el.classList.add('is-visible');
      });
    };

    const safetyTimer = window.setTimeout(revealHidden, 1800);

    let fadeObserver: IntersectionObserver | undefined;
    let staggerObserver: IntersectionObserver | undefined;
    let cardObserver: IntersectionObserver | undefined;

    if ('IntersectionObserver' in window) {
      fadeObserver = new IntersectionObserver(
        (entries) => {
          entries.forEach((entry) => {
            if (entry.isIntersecting) {
              entry.target.classList.add('is-visible');
            }
          });
        },
        { threshold: 0.04, rootMargin: '0px 0px -20px 0px' },
      );
      document.querySelectorAll('.animate-on-scroll').forEach((el) => fadeObserver!.observe(el));

      staggerObserver = new IntersectionObserver(
        (entries) => {
          entries.forEach((entry) => {
            if (!entry.isIntersecting) return;
            const children = entry.target.querySelectorAll('.animate-stagger-child');
            children.forEach((child, i) => {
              window.setTimeout(() => child.classList.add('is-visible'), i * 130);
            });
            staggerObserver!.unobserve(entry.target);
          });
        },
        { threshold: 0.1 },
      );
      document.querySelectorAll('.animate-stagger-parent').forEach((el) => staggerObserver!.observe(el));

      cardObserver = new IntersectionObserver(
        (entries) => {
          entries.forEach((entry) => {
            if (!entry.isIntersecting) return;
            const cards = entry.target.querySelectorAll('.carcard');
            cards.forEach((card, i) => {
              (card as HTMLElement).style.transitionDelay = `${i * 0.07}s`;
              card.classList.add('is-visible');
            });
            cardObserver!.unobserve(entry.target);
          });
        },
        { threshold: 0.05 },
      );
      document.querySelectorAll('.carouselSection').forEach((section) => {
        const grid = section.querySelector('.row, .d-flex');
        if (!grid) return;
        grid.querySelectorAll('.carcard').forEach((card) => {
          card.classList.add('animate-stagger-child');
        });
        cardObserver!.observe(grid);
      });
    } else {
      revealHidden();
    }

    return () => {
      window.clearTimeout(safetyTimer);
      window.removeEventListener('scroll', onNavbarScroll);
      window.removeEventListener('scroll', onHeroScroll);
      fadeObserver?.disconnect();
      staggerObserver?.disconnect();
      cardObserver?.disconnect();
    };
  }, [contentRevision]);
}
