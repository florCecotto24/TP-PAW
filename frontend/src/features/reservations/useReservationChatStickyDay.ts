import { useCallback, useEffect, useRef, useState, type RefObject } from 'react';

/** Barra de día flotante al hacer scroll — espejo de `reservation-chat.js`. */
export function useReservationChatStickyDay(
  messagesRef: RefObject<HTMLElement | null>,
  hasMessages: boolean,
) {
  const [stickyDayKey, setStickyDayKey] = useState<string | null>(null);
  const [scrolling, setScrolling] = useState(false);
  const lastStickyDayKey = useRef<string | null>(null);
  const fadeTimer = useRef<number | null>(null);
  const separatorMap = useRef<Map<string, HTMLElement>>(new Map());

  const setStickyLabel = useCallback((dayKey: string | null) => {
    if (!dayKey || dayKey === lastStickyDayKey.current) return;
    lastStickyDayKey.current = dayKey;
    setStickyDayKey(dayKey);
  }, []);

  const hideFloatingDayBar = useCallback(() => {
    setScrolling(false);
  }, []);

  const shouldShowFloatingDayBar = useCallback(() => {
    const el = messagesRef.current;
    const key = lastStickyDayKey.current;
    if (!el || !key) return false;
    const separator = separatorMap.current.get(key);
    if (!separator) return false;
    const containerTop = el.getBoundingClientRect().top;
    return separator.getBoundingClientRect().top < containerTop + 4;
  }, [messagesRef]);

  const showDayBarWhileScrolling = useCallback(() => {
    if (!shouldShowFloatingDayBar()) {
      hideFloatingDayBar();
      return;
    }
    setScrolling(true);
    if (fadeTimer.current != null) window.clearTimeout(fadeTimer.current);
    fadeTimer.current = window.setTimeout(hideFloatingDayBar, 1000);
  }, [hideFloatingDayBar, shouldShowFloatingDayBar]);

  const updateStickyFromScroll = useCallback(() => {
    const el = messagesRef.current;
    if (!el) return;
    const separators = el.querySelectorAll<HTMLElement>('.reservation-chat__day-separator');
    if (!separators.length) return;
    const containerTop = el.getBoundingClientRect().top;
    let activeKey: string | null = null;
    for (const sep of separators) {
      const rect = sep.getBoundingClientRect();
      const key = sep.getAttribute('data-day-key');
      if (rect.top <= containerTop + 8) {
        activeKey = key;
      } else if (!activeKey) {
        activeKey = key;
        break;
      }
    }
    if (!activeKey) activeKey = separators[0].getAttribute('data-day-key');
    if (activeKey) setStickyLabel(activeKey);
  }, [messagesRef, setStickyLabel]);

  const registerSeparator = useCallback((dayKey: string, node: HTMLElement | null) => {
    if (node) {
      separatorMap.current.set(dayKey, node);
    } else {
      separatorMap.current.delete(dayKey);
    }
  }, []);

  useEffect(() => {
    const el = messagesRef.current;
    if (!el || !hasMessages) {
      setStickyDayKey(null);
      lastStickyDayKey.current = null;
      return;
    }

    const onScroll = () => {
      if (el.querySelector('.reservation-chat__empty')) return;
      updateStickyFromScroll();
      showDayBarWhileScrolling();
    };

    el.addEventListener('scroll', onScroll, { passive: true });

    let observer: IntersectionObserver | null = null;
    const separators = el.querySelectorAll('.reservation-chat__day-separator');
    if (separators.length && typeof IntersectionObserver !== 'undefined') {
      observer = new IntersectionObserver(
        (entries) => {
          const containerTop = el.getBoundingClientRect().top;
          let bestKey: string | null = null;
          let bestTop = Infinity;
          for (const entry of entries) {
            if (!entry.isIntersecting) continue;
            const key = (entry.target as HTMLElement).getAttribute('data-day-key');
            const top = entry.boundingClientRect.top;
            if (key && top <= containerTop + 12 && top < bestTop) {
              bestTop = top;
              bestKey = key;
            }
          }
          if (bestKey) {
            setStickyLabel(bestKey);
            showDayBarWhileScrolling();
          } else {
            updateStickyFromScroll();
          }
        },
        {
          root: el,
          rootMargin: '-4px 0px -70% 0px',
          threshold: [0, 0.25, 0.5, 1],
        },
      );
      separators.forEach((sep) => observer!.observe(sep));
    } else {
      updateStickyFromScroll();
    }

    const lastSep = separators[separators.length - 1] as HTMLElement | undefined;
    if (lastSep) {
      const key = lastSep.getAttribute('data-day-key');
      if (key) setStickyLabel(key);
    }

    return () => {
      el.removeEventListener('scroll', onScroll);
      observer?.disconnect();
      if (fadeTimer.current != null) window.clearTimeout(fadeTimer.current);
    };
  }, [hasMessages, messagesRef, setStickyLabel, showDayBarWhileScrolling, updateStickyFromScroll]);

  return {
    stickyDayKey,
    dayBarScrolling: scrolling,
    showDayBar: hasMessages,
    registerSeparator,
    syncStickyToLastDay: updateStickyFromScroll,
  };
}
