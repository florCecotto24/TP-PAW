import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { paths } from '../routes/paths';

/** Espejo de footer.jsp: nav Inicio/Explorar/Publicar + copyright. */
export default function Footer() {
  const { t } = useTranslation();
  const year = new Date().getFullYear();

  return (
    <footer className="py-3 my-4">
      <ul className="nav justify-content-center border-bottom pb-3 mb-3">
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
      <p className="text-center text-body-secondary">
        {t('footer.copyright', { year, name: 'Ryden' })}
      </p>
    </footer>
  );
}
