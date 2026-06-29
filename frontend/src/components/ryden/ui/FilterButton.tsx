import { useTranslation } from 'react-i18next';

export interface FilterButtonProps {
  label: string;
  options?: string[];
  onSelect?: (option: string) => void;
}

export default function FilterButton({ label, options = [], onSelect }: FilterButtonProps) {
  const { t } = useTranslation();

  return (
    <div className="dropdown-center mx-2">
      <button
        className="btn btn-light border dropdown-toggle rounded-4"
        type="button"
        data-bs-toggle="dropdown"
        aria-expanded="false"
      >
        {label}
      </button>
      <ul className="dropdown-menu">
        {options.length === 0 ? (
          <li>
            <span className="dropdown-item-text text-muted">{t('filterButton.noOptions')}</span>
          </li>
        ) : (
          options.map((option) => (
            <li key={option}>
              <button className="dropdown-item" type="button" onClick={() => onSelect?.(option)}>
                {option}
              </button>
            </li>
          ))
        )}
      </ul>
    </div>
  );
}
