import { useTranslation } from 'react-i18next';

export interface ExploreFilterOption {
  key: string;
  label: string;
}

export interface ExploreFilterDropdownProps {
  filterLabel: string;
  helperText?: string;
  paramName: string;
  ariaGroup: string;
  options: ExploreFilterOption[];
  selectedKeys?: string[];
  onChange?: (paramName: string, selectedKeys: string[]) => void;
}

/** Espejo de {@code ryden-search:exploreFilterDropdown}. */
export default function ExploreFilterDropdown({
  filterLabel,
  helperText,
  paramName,
  ariaGroup,
  options,
  selectedKeys = [],
  onChange,
}: ExploreFilterDropdownProps) {
  const { t } = useTranslation();
  const selCount = selectedKeys.length;

  const toggle = (key: string, checked: boolean) => {
    const next = checked ? [...selectedKeys, key] : selectedKeys.filter((k) => k !== key);
    onChange?.(paramName, next);
  };

  return (
    <div className="dropdown explore-filter-dropdown mx-1 my-1">
      <button
        className="btn btn-light border dropdown-toggle rounded-4 d-inline-flex align-items-center gap-1"
        type="button"
        id={`explore_dd_${ariaGroup}`}
        data-bs-toggle="dropdown"
        data-bs-auto-close="outside"
        aria-expanded="false"
        aria-haspopup="true"
        aria-label={t('exploreFilterDropdown.ariaLabel', { filter: filterLabel })}
      >
        <span className="explore-filter-dropdown__label">{filterLabel}</span>
        <span
          className={`badge text-bg-primary rounded-pill${selCount === 0 ? ' d-none' : ''}`}
          data-filter-count="true"
        >
          {selCount}
        </span>
      </button>
      <ul className="dropdown-menu shadow explore-filter-dropdown__panel p-0" aria-labelledby={`explore_dd_${ariaGroup}`}>
        <li>
          <h6 className="dropdown-header mb-0">{filterLabel}</h6>
        </li>
        {helperText ? (
          <li>
            <span className="dropdown-item-text small text-body-secondary px-3 pb-2 d-block">{helperText}</span>
          </li>
        ) : null}
        <li>
          <hr className="dropdown-divider my-0" />
        </li>
        {options.map((opt) => {
          const isOptSel = selectedKeys.includes(opt.key);
          const inputId = `explore_${ariaGroup}${opt.key}`;
          return (
            <li key={opt.key}>
              <label className="dropdown-item d-flex gap-2 align-items-center py-2 px-3 mb-0" htmlFor={inputId}>
                <input
                  className="form-check-input flex-shrink-0 js-explore-filter mt-0"
                  type="checkbox"
                  name={paramName}
                  value={opt.key}
                  id={inputId}
                  checked={isOptSel}
                  onChange={(e) => toggle(opt.key, e.target.checked)}
                />
                <span className="small">{opt.label}</span>
              </label>
            </li>
          );
        })}
      </ul>
    </div>
  );
}
