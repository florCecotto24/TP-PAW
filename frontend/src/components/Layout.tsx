import { useEffect } from 'react';
import { Outlet, useLocation } from 'react-router-dom';
import NavBar from './NavBar';
import Footer from './Footer';
import { BlockedUserBanner } from './ryden';
import { useDocumentTitle } from '../i18n/useDocumentTitle';
import { useSessionStore } from '../session/sessionStore';
import { paths } from '../routes/paths';

/** Root layout: fixed navbar, optional blocked-user banner, routed content. */
export default function Layout() {
  useDocumentTitle();
  const location = useLocation();

  const currentUser = useSessionStore((s) => s.currentUser);
  const showBlockedBanner = currentUser?.blocked === true;
  const isHomeRoute = location.pathname === paths.home || location.pathname === '/';

  useEffect(() => {
    document.body.classList.add('bg-light');
    return () => {
      document.body.classList.remove('bg-light');
    };
  }, []);

  return (
    <div className="bg-cream min-vh-100">
      <NavBar />
      <div className={isHomeRoute ? 'app-shell app-shell--flush' : 'app-shell'}>
        {showBlockedBanner ? (
          <BlockedUserBanner singleReservationId={currentUser?.blockedOverdueReservationId} />
        ) : null}
        <main>
          <Outlet />
        </main>
        <Footer />
      </div>
    </div>
  );
}
