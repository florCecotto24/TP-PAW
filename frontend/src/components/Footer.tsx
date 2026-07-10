import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { paths } from '../routes/paths';

/** Espejo de footer.jsp: nav Inicio/Explorar/Publicar + copyright. */
export default function Footer() {
  const { t } = useTranslation();
  const year = new Date().getFullYear();

  return (
    <footer className="ryden-footer py-3 my-4">
      <div className="container ryden-footer__inner">
        <nav className="ryden-footer__nav mb-3">
          <ul className="nav mb-0">
            <li className="nav-item">
              <Link to={paths.home} className="nav-link px-2 text-body-secondary">
                {t('nav.home')}
              </Link>
            </li>
            <li className="nav-item">
              <Link to={paths.search} className="nav-link px-2 text-body-secondary">
                {t('nav.search')}
              </Link>
            </li>
            <li className="nav-item">
              <Link to={paths.publishCar} className="nav-link px-2 text-body-secondary">
                {t('nav.publish')}
              </Link>
            </li>
          </ul>
        </nav>
        <p className="ryden-footer__copyright text-body-secondary mb-0">
          {t('footer.copyright', { year, name: 'Ryden' })}
        </p>
      </div>
    </footer>
  );
}
