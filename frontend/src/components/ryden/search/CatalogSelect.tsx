import { useEffect, useRef, useState } from 'react';

export interface CatalogSelectOption {
  value: string;
  label: string;
  pending?: boolean;
}

export interface CatalogSelectProps {
  id: string;
  placeholder: string;
  searchPlaceholder: string;
  pendingLabel: string;
  options: CatalogSelectOption[];
  value: string;
  onChange: (value: string) => void;
  disabled?: boolean;
  disabledPlaceholder?: string;
}

/**
 * Dropdown de catálogo con buscador, réplica del picker de marca/modelo de
 * `publishCarForm.jsp` + `publish-wizard.js` (`ryden-select-btn`, `ryden-catalog-scroll`)
 * sin depender del JS de Bootstrap (estado propio + click-outside, misma técnica
 * que el menú de avatar de `MyProfilePage.tsx`).
 */
export default function CatalogSelect({
  id,
  placeholder,
  searchPlaceholder,
  pendingLabel,
  options,
  value,
  onChange,
  disabled,
  disabledPlaceholder,
}: CatalogSelectProps) {
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState('');
  const rootRef = useRef<HTMLDivElement>(null);
  const searchRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (!open) return;
    const onDocClick = (e: MouseEvent) => {
      if (rootRef.current && !rootRef.current.contains(e.target as Node)) setOpen(false);
    };
    document.addEventListener('mousedown', onDocClick);
    return () => document.removeEventListener('mousedown', onDocClick);
  }, [open]);

  useEffect(() => {
    if (!open) {
      setQuery('');
      return;
    }
    const raf = requestAnimationFrame(() => searchRef.current?.focus());
    return () => cancelAnimationFrame(raf);
  }, [open]);

  useEffect(() => {
    if (disabled) setOpen(false);
  }, [disabled]);

  const selected = options.find((o) => o.value === value);
  const q = query.trim().toLowerCase();
  const filtered = q ? options.filter((o) => o.label.toLowerCase().includes(q)) : options;

  return (
    <div className="dropdown" ref={rootRef}>
      <button
        type="button"
        id={id}
        className="form-select dropdown-toggle ryden-select-btn text-start w-100"
        aria-expanded={open}
        aria-haspopup="true"
        disabled={disabled}
        onClick={() => setOpen((o) => !o)}
      >
        <span className="text-truncate">
          {disabled ? (disabledPlaceholder ?? placeholder) : selected ? selected.label : placeholder}
        </span>
      </button>
      {open ? (
        <div
          className="dropdown-menu shadow p-0 w-100 show"
          style={{ minWidth: 0, position: 'absolute', top: '100%', left: 0, marginTop: '0.125rem' }}
        >
          <div className="px-3 pt-2 pb-1">
            <input
              ref={searchRef}
              type="search"
              className="form-control form-control-sm"
              autoComplete="off"
              placeholder={searchPlaceholder}
              value={query}
              onChange={(e) => setQuery(e.target.value)}
            />
          </div>
          <div className="ryden-catalog-scroll px-2 pb-2" style={{ maxHeight: 220, overflowY: 'auto' }}>
            <ul className="list-unstyled mb-0">
              {filtered.map((opt) => (
                <li key={opt.value}>
                  <button
                    type="button"
                    className={`dropdown-item d-flex gap-2 align-items-center py-2 px-2 mb-0 rounded-2${
                      opt.value === value ? ' active' : ''
                    }`}
                    onClick={() => {
                      onChange(opt.value);
                      setOpen(false);
                    }}
                  >
                    <span className="small">
                      {opt.label}
                      {opt.pending ? ` (${pendingLabel})` : ''}
                    </span>
                  </button>
                </li>
              ))}
            </ul>
          </div>
        </div>
      ) : null}
    </div>
  );
}
