import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { paths } from '../../../routes/paths';

export interface SimilarVehiclesHeaderProps {
  seeAllHref?: string;
  title?: string;
  subtitle?: string;
}

/** Espejo de {@code ryden-car:similarVehiclesHeader}. */
export default function SimilarVehiclesHeader({
  seeAllHref = paths.search,
  title,
  subtitle,
}: SimilarVehiclesHeaderProps) {
  const { t } = useTranslation();

  return (
    <div className="d-flex flex-wrap justify-content-between align-items-end gap-3 mb-4 similarVehiclesHeader">
      <div>
        <h2 className="h5 fw-bold mb-1">{title ?? t('similarVehicles.defaultTitle')}</h2>
        <p className="text-secondary small mb-0">{subtitle ?? t('similarVehicles.defaultSubtitle')}</p>
      </div>
      <Link
        to={seeAllHref}
        className="d-inline-flex align-items-center gap-1 text-decoration-none fw-semibold similarVehiclesSeeAll"
      >
        {t('common.seeAll')}
        <i className="bi bi-arrow-right" aria-hidden="true"></i>
      </Link>
    </div>
  );
}
