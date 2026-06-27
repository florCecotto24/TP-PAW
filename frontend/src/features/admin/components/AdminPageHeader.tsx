import type { ReactNode } from 'react';

interface AdminPageHeaderProps {
  title: string;
  subtitle?: string;
  actions?: ReactNode;
}

export default function AdminPageHeader({ title, subtitle, actions }: AdminPageHeaderProps) {
  return (
    <header className="d-flex flex-wrap align-items-end justify-content-between gap-3 mb-4">
      <div>
        <h1 className="h3 fw-bold mb-1">{title}</h1>
        {subtitle ? <p className="text-secondary mb-0">{subtitle}</p> : null}
      </div>
      {actions}
    </header>
  );
}
