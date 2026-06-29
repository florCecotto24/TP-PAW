import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';

export interface BreadcrumbTrailProps {
  currentLabel: string;
  homeLabel?: string;
  homeHref?: string;
  midLabel?: string;
  midHref?: string;
  mid2Label?: string;
  mid2Href?: string;
  showHome?: boolean;
}

export default function BreadcrumbTrail({
  currentLabel,
  homeLabel,
  homeHref = '/',
  midLabel,
  midHref,
  mid2Label,
  mid2Href,
  showHome = true,
}: BreadcrumbTrailProps) {
  const { t } = useTranslation();
  const resolvedHomeLabel = homeLabel ?? t('breadcrumb.home');

  function CrumbLink({ href, label }: { href: string; label: string }) {
    const internal = href.startsWith('/') && !href.startsWith('//');
    if (internal) {
      return (
        <Link to={href} className="text-decoration-none">
          {label}
        </Link>
      );
    }
    return (
      <a href={href} className="text-decoration-none">
        {label}
      </a>
    );
  }

  return (
    <nav aria-label={t('breadcrumb.aria')} style={{ overflow: 'hidden' }}>
      <ol className="breadcrumb mb-2 small" style={{ flexWrap: 'nowrap', overflow: 'hidden' }}>
        {showHome ? (
          <li className="breadcrumb-item flex-shrink-0">
            <CrumbLink href={homeHref} label={resolvedHomeLabel} />
          </li>
        ) : null}
        {midLabel ? (
          <li className="breadcrumb-item flex-shrink-0">
            {midHref ? <CrumbLink href={midHref} label={midLabel} /> : midLabel}
          </li>
        ) : null}
        {mid2Label ? (
          <li className="breadcrumb-item flex-shrink-0">
            {mid2Href ? <CrumbLink href={mid2Href} label={mid2Label} /> : mid2Label}
          </li>
        ) : null}
        <li
          className="breadcrumb-item active text-muted ryden-text-fade-end"
          aria-current="page"
          title={currentLabel}
          style={{ flex: '1 1 0', minWidth: 0 }}
        >
          {currentLabel}
        </li>
      </ol>
    </nav>
  );
}
