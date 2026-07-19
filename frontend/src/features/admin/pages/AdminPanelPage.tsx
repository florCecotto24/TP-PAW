import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { paths } from '../../../routes/paths';

const sections = [
  { to: paths.admin.users, titleKey: 'admin.users.title', descKey: 'admin.users.subtitle', icon: 'bi-people-fill' },
  { to: paths.admin.cars, titleKey: 'admin.cars.title', descKey: 'admin.cars.subtitle', icon: 'bi-car-front-fill' },
  { to: paths.admin.catalog, titleKey: 'admin.catalog.title', descKey: 'admin.catalog.subtitle', icon: 'bi-collection-fill' },
  {
    to: paths.admin.reservations,
    titleKey: 'admin.reservations.title',
    descKey: 'admin.reservations.subtitle',
    icon: 'bi-calendar-check-fill',
  },
] as const;

// Réplica de panel.jsp: banda de cabecera + nav cards con hover-arrow + quick actions.
export default function AdminPanelPage() {
  const { t } = useTranslation();
  return (
    <>
      <section className="card border-0 shadow-sm rounded-4 bg-white mt-4 p-4">
        <h1 className="h3 fw-bold mb-0">{t('admin.panel.title')}</h1>
        <p className="text-muted mb-0 mt-1" style={{ fontSize: '0.9rem' }}>
          {t('admin.panel.subtitle')}
        </p>
      </section>

      <div className="py-5">
        <p className="section-label">{t('admin.panel.title')}</p>
        <div className="row g-4 mb-5">
          {sections.map((section) => (
            <div key={section.to} className="col-md-6">
              <Link to={section.to} className="text-decoration-none">
                <div className="card bg-white border-0 shadow-sm rounded-4 admin-nav-card h-100">
                  <div className="card-body p-4">
                    <div className="d-flex align-items-center gap-3">
                      <div className="admin-nav-card__icon-wrap">
                        <i className={`bi ${section.icon}`} aria-hidden="true" />
                      </div>
                      <div className="flex-grow-1">
                        <h2 className="h6 fw-semibold mb-1 text-body">{t(section.titleKey)}</h2>
                        <p className="text-muted mb-0" style={{ fontSize: '0.82rem' }}>
                          {t(section.descKey)}
                        </p>
                      </div>
                      <i className="bi bi-chevron-right text-muted admin-nav-card__arrow" aria-hidden="true" />
                    </div>
                  </div>
                </div>
              </Link>
            </div>
          ))}
        </div>

        <p className="section-label">{t('admin.panel.quickActions')}</p>
        <div className="d-flex flex-wrap gap-2">
          <Link to={paths.admin.createAdmin} className="admin-action-btn">
            <i className="bi bi-person-plus-fill admin-action-btn__icon" aria-hidden="true" />
            {t('admin.panel.createAdmin')}
          </Link>
        </div>
      </div>
    </>
  );
}
