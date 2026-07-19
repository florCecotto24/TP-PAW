import type { ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { BreadcrumbTrail } from '../../../components/ryden';
import { paths } from '../../../routes/paths';

interface AdminPageHeaderProps {
  title: string;
  subtitle?: string;
  actions?: ReactNode;
  /**
   * Optional second crumb between Administración and the current page
   * (e.g. Usuarios → Crear Admin, Reservas → Chat).
   */
  midLabel?: string;
  midHref?: string;
}

export default function AdminPageHeader({
  title,
  subtitle,
  actions,
  midLabel,
  midHref,
}: AdminPageHeaderProps) {
  const { t } = useTranslation();

  return (
    <div className="mb-4">
      <BreadcrumbTrail
        homeLabel={t('admin.panel.title')}
        homeHref={paths.admin.panel}
        midLabel={midLabel}
        midHref={midHref}
        currentLabel={title}
      />
      <header className="d-flex flex-wrap align-items-end justify-content-between gap-3">
        <div>
          <h1 className="h3 fw-bold mb-1">{title}</h1>
          {subtitle ? <p className="text-secondary mb-0">{subtitle}</p> : null}
        </div>
        {actions}
      </header>
    </div>
  );
}
