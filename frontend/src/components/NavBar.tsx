import { useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, NavLink, useLocation, useNavigate } from 'react-router-dom';
import { Navbar, Nav, Container, NavDropdown } from 'react-bootstrap';
import i18n, { SUPPORTED_LOCALES, type Locale } from '../i18n';
import { useSessionStore } from '../session/sessionStore';
import { paths } from '../routes/paths';
import {
  isMyCarsNavActive,
  isOwnerReservationsNavActive,
  isRiderReservationsNavActive,
} from '../routes/navActive';
import { resolveProfilePictureAssetUrl } from '../api/uri';
import { LogoutConfirmModal } from './ryden';
import { useNavActivePill } from './useNavActivePill';
import logoUrl from '../assets/images/Ryden_logo.ico';

export default function NavBar() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const location = useLocation();
  const status = useSessionStore((s) => s.status);
  const currentUser = useSessionStore((s) => s.currentUser);
  const currentUserUri = useSessionStore((s) => s.currentUserUri);
  const logout = useSessionStore((s) => s.logout);
  const [logoutOpen, setLogoutOpen] = useState(false);
  const [avatarFailed, setAvatarFailed] = useState(false);
  const navRef = useRef<HTMLDivElement>(null);

  const isAuthenticated = status === 'authenticated';
  const isAdmin = currentUser?.role === 'admin';
  const lang = i18n.language;

  function onLogoutConfirm() {
    logout();
    setLogoutOpen(false);
    navigate(paths.home);
  }

  function setLanguage(next: Locale) {
    if (next !== lang) void i18n.changeLanguage(next);
  }

  const initials =
    `${currentUser?.forename?.[0] ?? ''}${currentUser?.surname?.[0] ?? ''}`.toUpperCase();
  const avatarUrl = avatarFailed
    ? null
    : resolveProfilePictureAssetUrl(currentUser?.links, currentUserUri);

  useEffect(() => {
    setAvatarFailed(false);
  }, [currentUser?.links?.profilePicture, currentUserUri]);

  const riderReservationsActive = isRiderReservationsNavActive(location.pathname, location.search);
  const myCarsActive = isMyCarsNavActive(location.pathname, location.search);
  const ownerReservationsActive = isOwnerReservationsNavActive(location.pathname, location.search);

  useNavActivePill(navRef, [
    location.pathname,
    location.search,
    isAuthenticated,
    riderReservationsActive,
  ]);

  return (
    <>
      <Navbar
        expand
        className="shadow-sm fixed-top mb-0 bg-body"
      >
        <Container fluid>
          <Navbar.Brand as={Link} to={paths.home} className="ms-3 fw-semibold">
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
            <Nav ref={navRef} className="nav-pills align-items-center mb-0">
              <Nav.Link as={NavLink} to={paths.home} end className="my-nav-item">
                {t('nav.home')}
              </Nav.Link>
              <Nav.Link as={NavLink} to={paths.search} className="my-nav-item">
                {t('nav.search')}
              </Nav.Link>
              <Nav.Link as={NavLink} to={paths.publishCar} className="px-1">
                {t('nav.publish')}
              </Nav.Link>
              {isAuthenticated && (
                <NavLink
                  to={paths.myReservations}
                  className={`nav-link px-1${riderReservationsActive ? ' active' : ''}`}
                  aria-current={riderReservationsActive ? 'page' : undefined}
                >
                  {t('nav.myReservations')}
                </NavLink>
              )}

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
                <Nav.Link as={NavLink} to={paths.login} className="px-1">
                  {t('nav.login')}
                </Nav.Link>
              )}

              {isAuthenticated && (
                <NavDropdown
                  align="end"
                  title={
                    <span
                      className="navbar-user-menu-toggle rounded-circle d-inline-flex align-items-center justify-content-center p-0 overflow-hidden"
                      style={{ width: 40, height: 40 }}
                    >
                      {avatarUrl ? (
                        <img
                          src={avatarUrl}
                          alt={t('navbar.avatar.alt')}
                          width={40}
                          height={40}
                          className="navbar-user-menu-toggle__img"
                          onError={() => setAvatarFailed(true)}
                        />
                      ) : initials ? (
                        <span className="fw-semibold text-primary small user-select-none">{initials}</span>
                      ) : (
                        <i className="bi bi-person-fill text-primary" aria-hidden="true" />
                      )}
                    </span>
                  }
                  id="navbar-user-menu"
                >
                  {isAdmin && (
                    <>
                      <NavDropdown.Item as={Link} to={paths.admin.panel}>
                        {t('nav.admin')}
                      </NavDropdown.Item>
                      <NavDropdown.Divider />
                    </>
                  )}
                  <NavDropdown.Item as={Link} to={paths.profile}>
                    {t('nav.profile')}
                  </NavDropdown.Item>
                  <NavDropdown.Divider />
                  <NavDropdown.Item as={Link} to={paths.myCars} active={myCarsActive}>
                    {t('nav.myCars')}
                  </NavDropdown.Item>
                  <NavDropdown.Item
                    as={Link}
                    to={paths.ownerReservations}
                    active={ownerReservationsActive}
                  >
                    {t('nav.ownerReservations')}
                  </NavDropdown.Item>
                  <NavDropdown.Item as={Link} to={paths.myFavorites}>
                    {t('nav.favorites')}
                  </NavDropdown.Item>
                  <NavDropdown.Divider />
                  <NavDropdown.Item className="text-danger" onClick={() => setLogoutOpen(true)}>
                    {t('nav.logout')}
                  </NavDropdown.Item>
                </NavDropdown>
              )}
            </Nav>
          </div>
        </Container>
      </Navbar>

      <LogoutConfirmModal
        open={logoutOpen}
        onClose={() => setLogoutOpen(false)}
        onConfirm={onLogoutConfirm}
      />
    </>
  );
}
