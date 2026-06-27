import { useTranslation } from 'react-i18next';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import { Navbar, Nav, Container, NavDropdown } from 'react-bootstrap';
import i18n, { SUPPORTED_LOCALES, type Locale } from '../i18n';
import { useSessionStore } from '../session/sessionStore';
import logoUrl from '../assets/images/Ryden_logo.ico';

/**
 * Navbar fija superior, espejando la estética del sitio JSP original
 * (navbar-expand · nav-pills · toggle ES/EN · menú de usuario con iniciales).
 */
export default function NavBar() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const status = useSessionStore((s) => s.status);
  const currentUser = useSessionStore((s) => s.currentUser);
  const logout = useSessionStore((s) => s.logout);

  const isAuthenticated = status === 'authenticated';
  const isAdmin = currentUser?.role === 'admin';
  const lang = i18n.language;

  function onLogout() {
    logout();
    navigate('/');
  }

  function setLanguage(next: Locale) {
    if (next !== lang) void i18n.changeLanguage(next);
  }

  const initials =
    `${currentUser?.forename?.[0] ?? ''}${currentUser?.surname?.[0] ?? ''}`.toUpperCase() || '?';

  return (
    <Navbar
      expand
      className="shadow-sm fixed-top mb-0 bg-body"
      style={{ minHeight: 'var(--ryden-navbar-h)' }}
    >
      <Container fluid>
        <Navbar.Brand as={Link} to="/" className="ms-3 fw-semibold">
          <img
            src={logoUrl}
            alt="Logo"
            width={30}
            height={24}
            className="d-inline-block align-text-top me-1"
          />
          {t('app.name')}
        </Navbar.Brand>

        <div className="d-flex flex-row justify-content-end align-items-center">
          <Nav className="nav-pills align-items-center mb-0">
            <Nav.Link as={NavLink} to="/" end className="my-nav-item">
              {t('nav.home')}
            </Nav.Link>
            <Nav.Link as={NavLink} to="/buscar" className="my-nav-item">
              {t('nav.search')}
            </Nav.Link>
            <Nav.Link as={NavLink} to="/publicar" className="px-1">
              {t('nav.publish')}
            </Nav.Link>
            {isAuthenticated && (
              <Nav.Link as={NavLink} to="/mis-reservas" className="px-1">
                {t('nav.myReservations')}
              </Nav.Link>
            )}

            {/* Toggle de idioma ES/EN (mismas clases que el sitio original) */}
            <div className="ryden-language-toggle ms-1 me-1" role="group" aria-label={t('nav.language')}>
              {SUPPORTED_LOCALES.map((l) => (
                <button
                  key={l}
                  type="button"
                  className={`ryden-language-toggle__btn${lang === l ? ' is-active' : ''}`}
                  aria-pressed={lang === l}
                  onClick={() => setLanguage(l)}
                >
                  {l.toUpperCase()}
                </button>
              ))}
            </div>

            {!isAuthenticated && (
              <Nav.Link as={NavLink} to="/ingresar" className="px-1">
                {t('nav.login')}
              </Nav.Link>
            )}

            {isAuthenticated && (
              <NavDropdown
                align="end"
                title={
                  <span
                    className="navbar-user-menu-toggle rounded-circle d-inline-flex align-items-center justify-content-center"
                    style={{ width: 40, height: 40 }}
                  >
                    <span className="fw-semibold text-primary small">{initials}</span>
                  </span>
                }
                id="navbar-user-menu"
              >
                {isAdmin && (
                  <>
                    <NavDropdown.Item as={Link} to="/admin">
                      {t('nav.admin')}
                    </NavDropdown.Item>
                    <NavDropdown.Divider />
                  </>
                )}
                <NavDropdown.Item as={Link} to="/perfil">
                  {t('nav.profile')}
                </NavDropdown.Item>
                <NavDropdown.Item as={Link} to="/mis-autos">
                  {t('nav.myCars')}
                </NavDropdown.Item>
                <NavDropdown.Item as={Link} to="/mis-autos/reservas">
                  {t('nav.ownerReservations')}
                </NavDropdown.Item>
                <NavDropdown.Item as={Link} to="/favoritos">
                  {t('nav.favorites')}
                </NavDropdown.Item>
                <NavDropdown.Divider />
                <NavDropdown.Item className="text-danger" onClick={onLogout}>
                  {t('nav.logout')}
                </NavDropdown.Item>
              </NavDropdown>
            )}
          </Nav>
        </div>
      </Container>
    </Navbar>
  );
}
