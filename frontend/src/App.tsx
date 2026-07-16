import { useEffect } from 'react';
import { QueryClientProvider } from '@tanstack/react-query';
import { I18nextProvider } from 'react-i18next';
import i18n from './i18n';
import AppRouter from './router/AppRouter';
import { useSessionStore, sessionClient } from './session/sessionStore';
import { useApiDiscoveryStore } from './api/apiDiscovery';
import { queryClient } from './queryClient';

export default function App() {
  // Rehidrata la sesión (tokens + URN) desde localStorage al arrancar.
  useEffect(() => {
    useSessionStore.getState().loadFromStorage();
    void useApiDiscoveryStore.getState().load(sessionClient);
  }, []);

  return (
    <I18nextProvider i18n={i18n}>
      <QueryClientProvider client={queryClient}>
        <AppRouter />
      </QueryClientProvider>
    </I18nextProvider>
  );
}
