import { useEffect } from 'react';
import { QueryClientProvider } from '@tanstack/react-query';
import { I18nextProvider } from 'react-i18next';
import i18n from './i18n';
import AppRouter from './router/AppRouter';
import { useSessionStore, sessionClient } from './session/sessionStore';
import { useApiDiscoveryStore } from './api/apiDiscovery';
import { queryClient } from './queryClient';
import { LoadingBlock } from './components/ryden';

function DiscoveryGate({ children }: { children: React.ReactNode }) {
  const ready = useApiDiscoveryStore((s) => s.ready);
  const index = useApiDiscoveryStore((s) => s.index);

  useEffect(() => {
    if (ready && index != null) return;
    let cancelled = false;
    const attempt = () => {
      if (cancelled) return;
      void useApiDiscoveryStore.getState().load(sessionClient).then(() => {
        if (cancelled) return;
        if (!useApiDiscoveryStore.getState().ready) {
          window.setTimeout(attempt, 1500);
        }
      });
    };
    attempt();
    return () => {
      cancelled = true;
    };
  }, [ready, index]);

  if (!ready || index == null) {
    return (
      <main className="container py-5">
        <LoadingBlock variant="page" className="py-5" />
      </main>
    );
  }

  return children;
}

export default function App() {
  // Rehidrata la sesión (tokens + URN) desde localStorage al arrancar.
  useEffect(() => {
    useSessionStore.getState().loadFromStorage();
  }, []);

  return (
    <I18nextProvider i18n={i18n}>
      <QueryClientProvider client={queryClient}>
        <DiscoveryGate>
          <AppRouter />
        </DiscoveryGate>
      </QueryClientProvider>
    </I18nextProvider>
  );
}
