import { NavLink } from 'react-router-dom';
import { useTranslation } from 'react-i18next';

const linkClass = ({ isActive }: { isActive: boolean }) =>
  `nav-link${isActive ? ' active fw-semibold' : ''}`;

export default function AdminNav() {
  const { t } = useTranslation();
  return (
    <ul className="nav nav-pills flex-wrap gap-1 mb-4">
      <li className="nav-item">
        <NavLink to="/admin" end className={linkClass}>
          {t('admin.nav.panel')}
        </NavLink>
      </li>
      <li className="nav-item">
        <NavLink to="/admin/usuarios" className={linkClass}>
          {t('admin.nav.users')}
        </NavLink>
      </li>
      <li className="nav-item">
        <NavLink to="/admin/catalogo" className={linkClass}>
          {t('admin.nav.catalog')}
        </NavLink>
      </li>
      <li className="nav-item">
        <NavLink to="/admin/autos" className={linkClass}>
          {t('admin.nav.cars')}
        </NavLink>
      </li>
      <li className="nav-item">
        <NavLink to="/admin/reservas" className={linkClass}>
          {t('admin.nav.reservations')}
        </NavLink>
      </li>
    </ul>
  );
}
