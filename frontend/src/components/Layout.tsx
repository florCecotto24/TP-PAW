import { Outlet } from 'react-router-dom';
import NavBar from './NavBar';
import { useDocumentTitle } from '../i18n/useDocumentTitle';

/** Layout raíz: navbar fija + área de contenido (Outlet del router). */
export default function Layout() {
  // Título del documento por ruta (cada navegación SPA actualiza el <title>).
  useDocumentTitle();

  return (
    <div className="bg-cream min-vh-100">
      <NavBar />
      {/* padding-top = altura de la navbar fixed-top (var compartida con NavBar
          para que no puedan divergir). */}
      <main style={{ paddingTop: 'var(--ryden-navbar-h)' }}>
        <Outlet />
      </main>
    </div>
  );
}
