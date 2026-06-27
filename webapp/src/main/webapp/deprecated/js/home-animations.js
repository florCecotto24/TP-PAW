/* home-animations.js — Ryden home page interactive effects */
document.addEventListener('DOMContentLoaded', function () {

    /* ── 1. Navbar: glassmorphism intensifies on scroll ──────────── */
    var navbar = document.querySelector('.navbar');
    if (navbar) {
        var onScroll = function () {
            navbar.classList.toggle('navbar--scrolled', window.scrollY > 30);
        };
        window.addEventListener('scroll', onScroll, { passive: true });
        onScroll();
    }

    /* ── 2. Hero parallax — background drifts slowly on scroll ───── */
    var heroSection = document.querySelector('.hero-section');
    if (heroSection) {
        window.addEventListener('scroll', function () {
            var scrolled = window.scrollY;
            if (scrolled < window.innerHeight * 1.5) {
                heroSection.style.setProperty('--parallax-y', (scrolled * 0.22) + 'px');
            }
        }, { passive: true });
    }

    /* ── 3. Intersection Observer: fade-up sections ──────────────── */
    /* Safety: reveal anything still hidden after 1.8 s (guards against
       elements already in viewport at load time that the observer misses) */
    setTimeout(function () {
        document.querySelectorAll('.animate-on-scroll:not(.is-visible)').forEach(function (el) {
            el.classList.add('is-visible');
        });
        document.querySelectorAll('.animate-stagger-child:not(.is-visible)').forEach(function (el) {
            el.classList.add('is-visible');
        });
    }, 1800);

    if ('IntersectionObserver' in window) {
        var fadeObserver = new IntersectionObserver(function (entries) {
            entries.forEach(function (entry) {
                if (entry.isIntersecting) {
                    entry.target.classList.add('is-visible');
                }
            });
        }, { threshold: 0.04, rootMargin: '0px 0px -20px 0px' });

        document.querySelectorAll('.animate-on-scroll').forEach(function (el) {
            fadeObserver.observe(el);
        });

        /* ── 4. Staggered children entrance ─────────────────────── */
        var staggerObserver = new IntersectionObserver(function (entries) {
            entries.forEach(function (entry) {
                if (entry.isIntersecting) {
                    var children = entry.target.querySelectorAll('.animate-stagger-child');
                    children.forEach(function (child, i) {
                        setTimeout(function () {
                            child.classList.add('is-visible');
                        }, i * 130);
                    });
                    staggerObserver.unobserve(entry.target);
                }
            });
        }, { threshold: 0.1 });

        document.querySelectorAll('.animate-stagger-parent').forEach(function (el) {
            staggerObserver.observe(el);
        });
    } else {
        /* Fallback: show everything immediately for old browsers */
        document.querySelectorAll('.animate-on-scroll, .animate-stagger-child').forEach(function (el) {
            el.classList.add('is-visible');
        });
    }

    /* ── 5. Feature icon — spring hover via JS (supplements CSS) ─── */
    document.querySelectorAll('.feature-item').forEach(function (item) {
        var icon = item.querySelector('.feature-icon-wrap');
        if (!icon) return;
        item.addEventListener('mouseenter', function () {
            icon.classList.add('feature-icon--hover');
        });
        item.addEventListener('mouseleave', function () {
            icon.classList.remove('feature-icon--hover');
        });
    });

    /* ── 6. How-it-works: 3D tilt on mousemove ──────────────────── */
    document.querySelectorAll('.how-it-works-step').forEach(function (card) {
        var raf = null;
        card.style.transformOrigin = 'center center';
        card.addEventListener('mousemove', function (e) {
            if (raf) return;
            raf = requestAnimationFrame(function () {
                raf = null;
                var rect = card.getBoundingClientRect();
                var dx = (e.clientX - (rect.left + rect.width  / 2)) / (rect.width  / 2);
                var dy = (e.clientY - (rect.top  + rect.height / 2)) / (rect.height / 2);
                card.style.transform = 'translateY(-10px) perspective(600px) rotateX(' + (-dy * 5).toFixed(1) + 'deg) rotateY(' + (dx * 5).toFixed(1) + 'deg)';
            });
        });
        card.addEventListener('mouseleave', function () {
            if (raf) { cancelAnimationFrame(raf); raf = null; }
            card.style.transform = '';
        });
    });

    /* ── 7. Carousel cards — stagger entrance when row scrolls in ── */
    var cardObserver = new IntersectionObserver(function (entries) {
        entries.forEach(function (entry) {
            if (entry.isIntersecting) {
                var cards = entry.target.querySelectorAll('.carcard');
                cards.forEach(function (card, i) {
                    card.style.transitionDelay = (i * 0.07) + 's';
                    card.classList.add('is-visible');
                });
                cardObserver.unobserve(entry.target);
            }
        });
    }, { threshold: 0.05 });

    document.querySelectorAll('.carouselSection').forEach(function (section) {
        var grid = section.querySelector('.row, .d-flex');
        if (grid) {
            grid.querySelectorAll('.carcard').forEach(function (card) {
                card.classList.add('animate-stagger-child');
            });
            cardObserver.observe(grid);
        }
    });

    /* ── 8. Skeleton shimmer — car card images while loading ─────── */
    document.querySelectorAll('.carcard-image img').forEach(function (img) {
        if (!img.complete || img.naturalWidth === 0) {
            img.classList.add('img-loading-state');
            img.closest('.carcard-image').classList.add('img-loading');
            function onLoaded() {
                img.classList.remove('img-loading-state');
                img.classList.add('img-loaded');
                img.closest('.carcard-image').classList.remove('img-loading');
            }
            img.addEventListener('load',  onLoaded);
            img.addEventListener('error', onLoaded);
        }
    });

});
