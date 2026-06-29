import { NavLink } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { paths } from '../../../routes/paths';

const linkClass = ({ isActive }: { isActive: boolean }) =>
  `nav-link${isActive ? ' active fw-semibold' : ''}`;

export default function AdminNav() {
  const { t } = useTranslation();
  return (
    <ul className="nav nav-pills flex-wrap gap-1 mb-4">
      <li className="nav-item">
        <NavLink to={paths.admin.panel} end className={linkClass}>
          {t('admin.nav.panel')}
        </NavLink>
      </li>
      <li className="nav-item">
        <NavLink to={paths.admin.users} className={linkClass}>
          {t('admin.nav.users')}
        </NavLink>
      </li>
      <li className="nav-item">
        <NavLink to={paths.admin.catalog} className={linkClass}>
          {t('admin.nav.catalog')}
        </NavLink>
      </li>
      <li className="nav-item">
        <NavLink to={paths.admin.cars} className={linkClass}>
          {t('admin.nav.cars')}
        </NavLink>
      </li>
      <li className="nav-item">
        <NavLink to={paths.admin.reservations} className={linkClass}>
          {t('admin.nav.reservations')}
        </NavLink>
      </li>
    </ul>
  );
}
