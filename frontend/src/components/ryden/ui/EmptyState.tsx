import type { ReactNode } from 'react';

export interface EmptyStateProps {
  /** Bootstrap Icons class suffix, e.g. `bi-search` or `search` */
  icon?: string;
  title: string;
  description?: string;
  actions?: ReactNode;
  className?: string;
  /** Wrap in a white card (admin tables, etc.) */
  inCard?: boolean;
}

function iconClass(icon: string): string {
  return icon.startsWith('bi-') ? icon : `bi-${icon}`;
}

/** Centered empty state with optional icon circle — uses `.search-empty-state` theme. */
export default function EmptyState({
  icon,
  title,
  description,
  actions,
  className = '',
  inCard = false,
}: EmptyStateProps) {
  const extra = className.trim();
  const content = (
    <div className={`search-empty-state text-center${extra ? ` ${extra}` : ''}`}>
      {icon ? (
        <div className="search-empty-state__icon" aria-hidden="true">
          <i className={`bi ${iconClass(icon)}`} />
        </div>
      ) : null}
      <h2 className="h5 fw-semibold mb-2">{title}</h2>
      {description ? (
        <p className="text-secondary mb-0 search-empty-state__text">{description}</p>
      ) : null}
      {actions ? <div className="search-empty-state__actions">{actions}</div> : null}
    </div>
  );

  if (inCard) {
    return (
      <div className="card border-0 shadow-sm bg-white rounded-4 overflow-hidden">
        <div className="card-body py-4">{content}</div>
      </div>
    );
  }

  return content;
}
