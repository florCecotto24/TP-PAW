import type { ButtonHTMLAttributes, ReactNode } from 'react';
import { Link } from 'react-router-dom';

export interface RydenButtonProps {
  /** Texto del botón (equiv. attribute {@code text} del tag JSP). */
  children?: ReactNode;
  text?: string;
  /** Variante semántica; el tag JSP la guarda pero las clases base usan {@code size}. */
  type?: string;
  size?: 'sm' | 'md' | 'lg';
  onClick?: ButtonHTMLAttributes<HTMLButtonElement>['onClick'];
  href?: string;
  disabled?: boolean;
  id?: string;
  cssClass?: string;
}

/**
 * Espejo de {@code ryden:button}: enlace si hay {@code href} y no está deshabilitado;
 * botón en caso contrario. Clases: {@code btn btn-{size}} + {@code cssClass}.
 */
export default function Button({
  children,
  text,
  size = 'md',
  onClick,
  href,
  disabled = false,
  id,
  cssClass = '',
}: RydenButtonProps) {
  const label = children ?? text ?? '';
  const classes = `btn btn-${size} ${cssClass}`.trim();

  if (href && !disabled) {
    const isInternal = href.startsWith('/') && !href.startsWith('//');
    if (isInternal) {
      return (
        <Link to={href} className={classes} id={id} onClick={onClick as never}>
          {label}
        </Link>
      );
    }
    return (
      <a href={href} className={classes} id={id} onClick={onClick as never}>
        {label}
      </a>
    );
  }

  return (
    <button type="button" className={classes} id={id} onClick={onClick} disabled={disabled}>
      {label}
    </button>
  );
}
