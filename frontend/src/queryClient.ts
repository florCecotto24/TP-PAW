import { QueryClient } from '@tanstack/react-query';

/** Shared QueryClient for the SPA (logout must clear it to avoid cross-user cache leaks). */
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: { retry: false, refetchOnWindowFocus: false },
  },
});
