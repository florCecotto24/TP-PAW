import { useEffect } from 'react';
import { Outlet, useLocation } from 'react-router-dom';
import NavBar from './NavBar';
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
  const isHome = location.pathname === paths.home || location.pathname === '/';

  useEffect(() => {
    document.body.classList.add('bg-light');
    if (!isHome) {
      document.body.classList.add('has-fixed-navbar');
    }
    return () => {
      document.body.classList.remove('has-fixed-navbar', 'bg-light');
    };
  }, [isHome]);

  return (
    <div className="bg-cream min-vh-100">
      <NavBar />
      {showBlockedBanner ? <BlockedUserBanner /> : null}
      <main>
        <Outlet />
      </main>
    </div>
  );
}
