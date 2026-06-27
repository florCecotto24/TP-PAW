import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import AdminPageHeader from '../components/AdminPageHeader';

const sections = [
  { to: '/admin/usuarios', titleKey: 'admin.users.title', descKey: 'admin.users.subtitle', icon: 'bi-people' },
  { to: '/admin/catalogo', titleKey: 'admin.catalog.title', descKey: 'admin.catalog.subtitle', icon: 'bi-tags' },
  { to: '/admin/autos', titleKey: 'admin.cars.title', descKey: 'admin.cars.subtitle', icon: 'bi-car-front' },
  { to: '/admin/reservas', titleKey: 'admin.reservations.title', descKey: 'admin.reservations.subtitle', icon: 'bi-calendar-check' },
] as const;

export default function AdminPanelPage() {
  const { t } = useTranslation();
  return (
    <>
      <AdminPageHeader title={t('admin.panel.title')} subtitle={t('admin.panel.subtitle')} />
      <div className="row g-3">
        {sections.map((section) => (
          <div key={section.to} className="col-12 col-md-6">
            <Link to={section.to} className="text-decoration-none text-reset">
              <article className="card border-0 shadow-sm bg-white h-100">
                <div className="card-body p-4">
                  <i className={`bi ${section.icon} fs-3 text-primary mb-2 d-block`} aria-hidden="true" />
                  <h2 className="h5 fw-semibold mb-1">{t(section.titleKey)}</h2>
                  <p className="text-secondary small mb-0">{t(section.descKey)}</p>
                </div>
              </article>
            </Link>
          </div>
        ))}
      </div>
    </>
  );
}
