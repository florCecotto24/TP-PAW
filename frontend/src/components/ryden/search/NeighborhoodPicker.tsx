import { useMemo, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';

/** Closes an open Bootstrap dropdown via its toggle (data-api), without relying on {@code window.bootstrap}. */
function closeDropdownToggle(toggle: HTMLButtonElement) {
  if (toggle.getAttribute('aria-expanded') === 'true') {
    toggle.click();
  }
}

export interface NeighborhoodOption {
  id: number | string;
  name: string;
}

export interface NeighborhoodPickerProps {
  pickerId: string;
  neighborhoodList: NeighborhoodOption[];
  anyLabel: string;
  searchPlaceholder: string;
  selectFieldLabel: string;
  toggleAriaLabel: string;
  allowMultiple?: boolean;
  selectedNeighborhoodIds?: Array<number | string>;
  selectedNeighborhoodId?: number | string | null;
  formId?: string;
  searchBarInline?: boolean;
  wrapExtraClass?: string;
  onSelectionChange?: (ids: Array<number | string>) => void;
}

function levenshtein(a: string, b: string): number {
  if (!a.length) return b.length;
  if (!b.length) return a.length;
  const row = Array.from({ length: b.length + 1 }, (_, j) => j);
  for (let i = 1; i <= a.length; i++) {
    let prev = row[0];
    row[0] = i;
    for (let j = 1; j <= b.length; j++) {
      const cur = row[j];
      const cost = a.charAt(i - 1) === b.charAt(j - 1) ? 0 : 1;
      row[j] = Math.min(prev + cost, row[j] + 1, row[j - 1] + 1);
      prev = cur;
    }
  }
  return row[b.length];
}

function nameMatchesQuery(q: string, nameLower: string): boolean {
  if (!q) return true;
  if (nameLower.includes(q)) return true;
  return levenshtein(q, nameLower) <= 2;
}

/**
 * Espejo de {@code ryden-search:neighborhoodPicker} (modo GET con lista filtrable).
 * El modo Spring/form:select se delega al caller con un select nativo.
 */
export default function NeighborhoodPicker({
  pickerId,
  neighborhoodList,
  anyLabel,
  searchPlaceholder,
  selectFieldLabel,
  toggleAriaLabel,
  allowMultiple = true,
  selectedNeighborhoodIds = [],
  selectedNeighborhoodId = null,
  searchBarInline = false,
  wrapExtraClass = '',
  onSelectionChange,
}: NeighborhoodPickerProps) {
  const { t } = useTranslation();
  const [filter, setFilter] = useState('');
  const toggleRef = useRef<HTMLButtonElement>(null);

  const selectedSet = useMemo(() => {
    if (allowMultiple) return new Set(selectedNeighborhoodIds.map(String));
    return selectedNeighborhoodId != null ? new Set([String(selectedNeighborhoodId)]) : new Set<string>();
  }, [allowMultiple, selectedNeighborhoodIds, selectedNeighborhoodId]);

  const multiCount = allowMultiple ? selectedNeighborhoodIds.length : selectedNeighborhoodId != null ? 1 : 0;

  const displayLabel = useMemo(() => {
    if (multiCount === 0) return anyLabel;
    if (!allowMultiple) {
      const nb = neighborhoodList.find((n) => String(n.id) === String(selectedNeighborhoodId));
      return nb?.name ?? anyLabel;
    }
    if (multiCount === 1) {
      const id = selectedNeighborhoodIds[0];
      const nb = neighborhoodList.find((n) => String(n.id) === String(id));
      return nb?.name ?? anyLabel;
    }
    return t('search.filter.neighborhood.multiCount', { count: multiCount });
  }, [
    multiCount,
    allowMultiple,
    anyLabel,
    neighborhoodList,
    selectedNeighborhoodId,
    selectedNeighborhoodIds,
    t,
  ]);

  const filtered = neighborhoodList.filter((nb) =>
    nameMatchesQuery(filter.trim().toLowerCase(), nb.name.toLowerCase()),
  );

  const toggleId = (id: number | string) => {
    const s = String(id);
    if (!allowMultiple) {
      onSelectionChange?.([id]);
      if (toggleRef.current) closeDropdownToggle(toggleRef.current);
      return;
    }
    const next = selectedSet.has(s)
      ? selectedNeighborhoodIds.filter((x) => String(x) !== s)
      : [...selectedNeighborhoodIds, id];
    onSelectionChange?.(next);
  };

  const btnId = `nb_dd_btn_${pickerId}`;
  const textId = `nb_dd_text_${pickerId}`;
  const filterId = `nb_filter_${pickerId}`;
  const listId = `nb_list_${pickerId}`;

  return (
    <div
      className={`neighborhood-picker ${wrapExtraClass}${searchBarInline ? ' neighborhood-picker--search-bar' : ''}`}
    >
      {searchBarInline ? (
        <label className="form-label small text-secondary mb-1" htmlFor={btnId}>
          {selectFieldLabel}
        </label>
      ) : null}
      <div
        id={`nb_dd_wrap_${pickerId}`}
        className="dropdown explore-filter-dropdown neighborhood-picker__dropdown w-100"
        data-nb-any={anyLabel}
      >
        <button
          type="button"
          ref={toggleRef}
          className={
            searchBarInline
              ? 'form-control form-control-sm border-0 shadow-none dropdown-toggle neighborhood-picker__toggle neighborhood-picker__toggle--search-bar d-flex align-items-center gap-2 w-100 text-start'
              : 'form-select dropdown-toggle neighborhood-picker__toggle neighborhood-picker__toggle--form-select text-start'
          }
          id={btnId}
          data-bs-toggle="dropdown"
          data-bs-auto-close="outside"
          aria-expanded="false"
          aria-haspopup="true"
          aria-label={toggleAriaLabel}
        >
          <span
            className={searchBarInline ? 'text-truncate min-w-0' : 'text-truncate'}
            id={textId}
            data-placeholder={anyLabel}
          >
            {displayLabel}
          </span>
          {allowMultiple ? (
            <span
              id={`nb_dd_badge_${pickerId}`}
              className={`badge text-bg-primary rounded-pill flex-shrink-0${multiCount === 0 ? ' d-none' : ''}`}
              data-nb-count="true"
            >
              {multiCount}
            </span>
          ) : null}
        </button>
        <div
          className="dropdown-menu dropdown-menu-end shadow explore-filter-dropdown__panel p-0 w-100 neighborhood-picker__menu"
          style={{ minWidth: 0 }}
          aria-labelledby={btnId}
        >
          <div className="px-3 pt-2 pb-1">
            <label className="visually-hidden" htmlFor={filterId}>
              {searchPlaceholder}
            </label>
            <input
              type="search"
              className="form-control form-control-sm"
              id={filterId}
              autoComplete="off"
              placeholder={searchPlaceholder}
              aria-label={searchPlaceholder}
              value={filter}
              onChange={(e) => setFilter(e.target.value)}
            />
          </div>
          <div className="neighborhood-picker__scroll px-2 pb-2">
            <ul className="list-unstyled mb-0" id={listId}>
              {filtered.map((nb) => {
                const checked = selectedSet.has(String(nb.id));
                const inputId = allowMultiple
                  ? `nb_cb_${pickerId}_${nb.id}`
                  : `nb_rb_${pickerId}_${nb.id}`;
                return (
                  <li
                    key={nb.id}
                    className="neighborhood-picker__row mb-0"
                    data-nb-lookup={nb.name.toLowerCase()}
                  >
                    <label className="dropdown-item d-flex gap-2 align-items-center py-2 px-2 mb-0 rounded-2" htmlFor={inputId}>
                      <input
                        type={allowMultiple ? 'checkbox' : 'radio'}
                        className="form-check-input flex-shrink-0 js-neighborhood-pick mt-0"
                        name={allowMultiple ? 'neighborhoodId' : `nb_ui_${pickerId}`}
                        value={nb.id}
                        id={inputId}
                        data-picker-id={pickerId}
                        checked={checked}
                        onChange={() => toggleId(nb.id)}
                      />
                      <span className="small js-nb-row-name">{nb.name}</span>
                    </label>
                  </li>
                );
              })}
            </ul>
          </div>
        </div>
      </div>
      <div id={`nb_src_${pickerId}`} className="d-none" aria-hidden="true">
        {neighborhoodList.map((nb) => (
          <span key={nb.id} data-nid={nb.id} data-name={nb.name} />
        ))}
      </div>
    </div>
  );
}
