/**
 * Inline create/edit form accordion and period-card highlight logic for manageCarPeriods.jsp.
 *
 * How the date picker works: components.js attaches a Flatpickr POPUP to every
 * [data-publish-avail-row] .ryden-avail-range-input at DOMContentLoaded.
 * The hidden #publish_avail_add button triggers adding a new initialized row.
 * We MUST use addBtn.click() — never clone the template directly — so that
 * Flatpickr is properly initialized on every row.
 *
 * First-period flow: when isFirstPeriod is true, clicking "Add period" shows a
 * Bootstrap modal to capture minimumRentalDays before opening the form.
 * After confirming, confirmMinRentalDays() sets the hidden field and opens the form.
 *
 * Exposes window.RydenManagePeriods:
 *   openCreate()           — reset to create mode and expand (or show modal if first period)
 *   openEdit(card)         — populate from card data-* and expand in edit mode
 *   close()                — collapse and reset
 *   openOnLoad(mode)       — auto-open after error re-renders
 *   confirmMinRentalDays() — called by the first-period modal confirm button
 *   isOpen()               — returns whether the accordion is visible
 */
(function () {
    'use strict';

    var formSection    = null;
    var formEl         = null;
    var titleEl        = null;
    var submitLabelEl  = null;
    var addBtn         = null;  // #publish_avail_add
    var rowsRoot       = null;  // #publish_availability_rows
    var createUrl      = '';
    var editUrlPrefix  = '';
    var editUrlSuffix  = '';
    var originalCbuAttr = '';
    var titleCreate    = '';
    var titleEdit      = '';
    var submitLabelCreate = '';
    var submitLabelEdit   = '';
    var isFirstPeriod  = false;
    // Initial pre-filled neighborhood (for restoring create mode state)
    var initialNbId   = '';
    var initialNbText = '';
    // Active edit only: reserved ranges (Date pairs) of the period currently being edited.
    // Used by submit guard + click-outside guard. Reset by close()/setCreateMode().
    var currentReservedRanges = [];

    function isOpen() {
        return formSection && formSection.classList.contains('show');
    }

    /** Destroy Flatpickr on a row (stored as row._rydenAvailFp) then clear the container. */
    function clearAvailabilityRows() {
        if (!rowsRoot) { return; }
        rowsRoot.querySelectorAll('[data-publish-avail-row]').forEach(function (row) {
            if (row._rydenAvailFp && typeof row._rydenAvailFp.destroy === 'function') {
                row._rydenAvailFp.destroy();
                row._rydenAvailFp = null;
            }
        });
        rowsRoot.innerHTML = '';
    }

    /**
     * Add one new empty row with Flatpickr initialized via the hidden add button.
     * components.js listens to addBtn.click and calls its own initRow().
     */
    function addOneEmptyRow() {
        if (addBtn) { addBtn.click(); }
    }

    /**
     * After addOneEmptyRow(), set the date range on the first row using Flatpickr's API.
     * Falls back to direct hidden-input manipulation if the Flatpickr instance is not found.
     *
     * Also clears the picker's `disable` list: in edit mode the prefilled range may overlap a
     * day already held by an active reservation (a reservation against THIS availability itself),
     * so the user must be able to keep / shrink / extend the existing range. Server-side, the
     * existing edit-conflict guard catches the cases that matter (removing reserved days).
     */
    function setFirstRowDates(from, to) {
        if (!rowsRoot || !from || !to) { return; }
        var row = rowsRoot.querySelector('[data-publish-avail-row]');
        if (!row) { return; }
        var fp = row._rydenAvailFp && row._rydenAvailFp.fp ? row._rydenAvailFp.fp : null;
        if (fp) {
            fp.set('minDate', null);
            fp.set('disable', []);
            var fromDate = new Date(from + 'T00:00:00');
            var toDate   = new Date(to   + 'T00:00:00');
            fp.setDate([fromDate, toDate], true);
            // setDate jumps to latestSelectedDateObj (the "to" end). With showMonths:2
            // that leaves the picker on (to, to+1) instead of (from, from+1), which is
            // disorienting when the range spans months. Re-anchor on the start.
            fp.jumpToDate(fromDate, false);
        } else {
            var fromH = row.querySelector('.ryden-avail-from');
            var untilH = row.querySelector('.ryden-avail-until');
            var display = row.querySelector('.ryden-avail-range-input');
            if (fromH)   { fromH.value   = from; }
            if (untilH)  { untilH.value  = to; }
            if (display) { display.value = from + ' – ' + to; }
        }
    }

    function parseYmdLocal(ymd) {
        return new Date(ymd + 'T00:00:00');
    }

    function startOfDay(d) {
        return new Date(d.getFullYear(), d.getMonth(), d.getDate());
    }

    /**
     * Returns true if every reservedRange is fully covered by [selStart, selEnd].
     * selStart/selEnd are Date objects (local midnight). Reserved-range entries can have
     * {from, to} either as ISO ymd strings or as Date objects (so the same helper works
     * for the picker's onChange and for the form-submit guard).
     */
    function rangeCoversAllReservations(selStart, selEnd, reservedRanges) {
        if (!reservedRanges || reservedRanges.length === 0) { return true; }
        var s = startOfDay(selStart).getTime();
        var e = startOfDay(selEnd).getTime();
        for (var i = 0; i < reservedRanges.length; i++) {
            var r = reservedRanges[i];
            if (!r || !r.from || !r.to) { continue; }
            var rsRaw = r.from instanceof Date ? r.from : parseYmdLocal(r.from);
            var reRaw = r.to   instanceof Date ? r.to   : parseYmdLocal(r.to);
            var rs = startOfDay(rsRaw).getTime();
            var re = startOfDay(reRaw).getTime();
            if (rs < s || re > e) { return false; }
        }
        return true;
    }

    function showInlineAvailError(message) {
        var box = document.getElementById('publishClientAvailError');
        if (!box) { return; }
        box.textContent = message;
        box.classList.remove('d-none');
    }

    function clearInlineAvailError() {
        var box = document.getElementById('publishClientAvailError');
        if (!box) { return; }
        box.textContent = '';
        box.classList.add('d-none');
    }

    /**
     * In edit mode, surface the days already held by active reservations of the period
     * being edited (red marker via CSS class) and prevent the owner from selecting a new
     * range that would orphan any of them. Hooks are pushed onto the live Flatpickr
     * config arrays, so they run alongside the hidden-input updater registered by
     * components.js. The picker is recreated on every openEdit, so accumulating hooks
     * across edits is not a concern.
     */
    function applyReservedDayMarkers(reservedRanges) {
        if (!rowsRoot) { return; }
        var row = rowsRoot.querySelector('[data-publish-avail-row]');
        if (!row) { return; }
        var fp = row._rydenAvailFp && row._rydenAvailFp.fp ? row._rydenAvailFp.fp : null;
        if (!fp) { return; }

        // Pre-parse to Date once per ranges list for cheaper checks.
        var parsed = (reservedRanges || []).map(function (r) {
            if (!r || !r.from || !r.to) { return null; }
            return { from: parseYmdLocal(r.from), to: parseYmdLocal(r.to) };
        }).filter(function (r) { return r !== null; });
        // Expose to the submit guard (read in init's submit listener and reset in close()).
        currentReservedRanges = parsed;

        if (!fp.config.onDayCreate) { fp.config.onDayCreate = []; }
        if (!Array.isArray(fp.config.onDayCreate)) {
            fp.config.onDayCreate = [fp.config.onDayCreate];
        }
        var publishSec = document.getElementById('publishAvailabilitySection');
        var reservedShrinkMsg = publishSec
                ? (publishSec.getAttribute('data-publish-reserved-shrink-msg') || '') : '';
        var reservedDayTitle = publishSec
                ? (publishSec.getAttribute('data-publish-reserved-day-title') || '') : '';

        fp.config.onDayCreate.push(function (_dObj, _dStr, _fp, dayElem) {
            if (!dayElem || !dayElem.dateObj) { return; }
            var t = startOfDay(dayElem.dateObj).getTime();
            for (var i = 0; i < parsed.length; i++) {
                if (t >= startOfDay(parsed[i].from).getTime()
                        && t <= startOfDay(parsed[i].to).getTime()) {
                    dayElem.classList.add('has-active-reservation');
                    var baseTitle = dayElem.getAttribute('aria-label') || '';
                    dayElem.setAttribute(
                            'title',
                            baseTitle ? baseTitle + (reservedDayTitle ? ' — ' + reservedDayTitle : '')
                                      : (reservedDayTitle || ''));
                    return;
                }
            }
        });

        if (!fp.config.onChange) { fp.config.onChange = []; }
        if (!Array.isArray(fp.config.onChange)) {
            fp.config.onChange = [fp.config.onChange];
        }
        fp.config.onChange.push(function (selectedDates) {
            if (parsed.length === 0) { return; }
            if (selectedDates.length < 2) {
                // Mid-selection (only "from" picked so far): don't validate yet, but also
                // clear any stale error so the user knows we're listening.
                clearInlineAvailError();
                return;
            }
            var ok = rangeCoversAllReservations(selectedDates[0], selectedDates[1], reservedRanges);
            if (ok) {
                clearInlineAvailError();
                return;
            }
            showInlineAvailError(reservedShrinkMsg || 'El rango nuevo dejaría afuera reservas activas.');
            // Revert to the prefilled range. The hiddens will be re-synced by the
            // components.js onChange that runs FIRST on the next setDate call.
            var fromH = row.querySelector('.ryden-avail-from');
            var untilH = row.querySelector('.ryden-avail-until');
            // Use the period's original bounds from data-* attrs of the active edit card.
            // Fall back to whatever was previously valid via the picker's internal state.
            // setDate(..., false) avoids re-firing this very onChange (silent revert).
            var origFrom = fromH && fromH.dataset.lastValidYmd
                ? parseYmdLocal(fromH.dataset.lastValidYmd) : null;
            var origUntil = untilH && untilH.dataset.lastValidYmd
                ? parseYmdLocal(untilH.dataset.lastValidYmd) : null;
            if (origFrom && origUntil) {
                fp.setDate([origFrom, origUntil], false);
            } else {
                // First invalid pick after opening edit: snap back to data-period-from/to,
                // which the picker was seeded with in setFirstRowDates.
                var card = document.querySelector('.manage-period-card.is-editing')
                        || document.querySelector('.manage-period-card');
                // Not strictly needed; the next openEdit re-seeds anyway.
                if (card) {
                    var bf = card.getAttribute('data-period-from');
                    var bt = card.getAttribute('data-period-to');
                    if (bf && bt) {
                        fp.setDate([parseYmdLocal(bf), parseYmdLocal(bt)], false);
                    }
                }
            }
        });

        // Remember the last valid (initial) range so future reverts have a target.
        var fromH = row.querySelector('.ryden-avail-from');
        var untilH = row.querySelector('.ryden-avail-until');
        if (fromH && fromH.value) { fromH.dataset.lastValidYmd = fromH.value; }
        if (untilH && untilH.value) { untilH.dataset.lastValidYmd = untilH.value; }

        // Also track every accepted (valid) selection as the new "last valid".
        fp.config.onChange.push(function (selectedDates) {
            if (selectedDates.length !== 2) { return; }
            if (!rangeCoversAllReservations(selectedDates[0], selectedDates[1], reservedRanges)) {
                return;
            }
            if (fromH) { fromH.dataset.lastValidYmd = fromH.value; }
            if (untilH) { untilH.dataset.lastValidYmd = untilH.value; }
        });

        // Apply onDayCreate to days already in the DOM (the picker is created and shown
        // before our hook is attached).
        fp.redraw();
    }

    function resetNeighborhoodPicker() {
        var hidden = document.getElementById('nb_hid_publish');
        if (hidden) { hidden.value = ''; }
        document.querySelectorAll('[id^="nb_rb_publish_"]:checked').forEach(function (rb) {
            rb.checked = false;
        });
        var text = document.getElementById('nb_dd_text_publish');
        var wrap = document.getElementById('nb_dd_wrap_publish');
        if (text) {
            var placeholder = text.getAttribute('data-placeholder')
                || (wrap && wrap.getAttribute('data-nb-any'))
                || '';
            text.textContent = placeholder;
        }
    }

    function restoreInitialNeighborhood() {
        resetNeighborhoodPicker();
        if (initialNbId) { selectNeighborhood(initialNbId); }
    }

    function selectNeighborhood(id) {
        if (!id) { return; }
        var rb = document.getElementById('nb_rb_publish_' + id);
        if (rb) { rb.click(); }
    }

    function openFormSection() {
        if (!formSection) { return; }
        if (window.bootstrap && window.bootstrap.Collapse) {
            window.bootstrap.Collapse.getOrCreateInstance(formSection, { toggle: false }).show();
        } else {
            formSection.classList.add('show');
        }
        setTimeout(function () {
            formSection.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
        }, 150);
    }

    function closeFormSection() {
        if (!formSection) { return; }
        if (window.bootstrap && window.bootstrap.Collapse) {
            window.bootstrap.Collapse.getOrCreateInstance(formSection, { toggle: false }).hide();
        } else {
            formSection.classList.remove('show');
        }
    }

    function setCreateMode() {
        if (!formEl) { return; }
        formEl.setAttribute('action', createUrl);
        formEl.setAttribute('data-ryden-user-has-cbu', originalCbuAttr);
        if (titleEl) { titleEl.textContent = titleCreate; }
        if (submitLabelEl) { submitLabelEl.textContent = submitLabelCreate; }
    }

    function setEditMode(id) {
        if (!formEl) { return; }
        formEl.setAttribute('action', editUrlPrefix + id + editUrlSuffix);
        formEl.setAttribute('data-ryden-user-has-cbu', 'true');
        if (titleEl) { titleEl.textContent = titleEdit; }
        if (submitLabelEl) { submitLabelEl.textContent = submitLabelEdit; }
    }

    /** Core open-create logic (called after modal confirmation if first period). */
    function doOpenCreate() {
        if (!formEl) { return; }
        clearAvailabilityRows();
        if (formEl.reset) { formEl.reset(); }
        restoreInitialNeighborhood();
        addOneEmptyRow();
        setCreateMode();
        openFormSection();
    }

    function openCreate() {
        if (!formEl) { return; }
        if (isFirstPeriod) {
            var modalEl = document.getElementById('inlineMinRentalDaysModal');
            if (modalEl && window.bootstrap) {
                window.bootstrap.Modal.getOrCreateInstance(modalEl).show();
            }
            return;
        }
        doOpenCreate();
    }

    /** Called by the first-period modal confirm button. */
    function confirmMinRentalDays() {
        var input = document.getElementById('minRentalDaysModalInput');
        var val = Math.min(365, Math.max(1, parseInt(input ? input.value : '1', 10) || 1));
        var hidden = document.getElementById('minimumRentalDays');
        if (hidden) { hidden.value = val; }
        var modalEl = document.getElementById('inlineMinRentalDaysModal');
        if (modalEl && window.bootstrap) {
            var modal = window.bootstrap.Modal.getInstance(modalEl);
            if (modal) { modal.hide(); }
        }
        doOpenCreate();
    }

    function openEdit(card) {
        if (!formEl || !card) { return; }
        var id       = card.getAttribute('data-period-id')              || '';
        var from     = card.getAttribute('data-period-from')            || '';
        var to       = card.getAttribute('data-period-to')              || '';
        var price    = card.getAttribute('data-period-price')           || '';
        var nbId     = card.getAttribute('data-period-neighborhood-id') || '';
        var street   = card.getAttribute('data-period-street')          || '';
        var streetNum= card.getAttribute('data-period-street-number')   || '';
        var checkIn  = card.getAttribute('data-period-check-in')        || '';
        var checkOut = card.getAttribute('data-period-check-out')       || '';
        var reservedRangesRaw = card.getAttribute('data-period-reserved-ranges') || '[]';
        var reservedRanges = [];
        try {
            reservedRanges = JSON.parse(reservedRangesRaw) || [];
        } catch (e) {
            reservedRanges = [];
        }

        clearAvailabilityRows();
        if (formEl.reset) { formEl.reset(); }
        resetNeighborhoodPicker();
        setEditMode(id);

        var priceInput = document.getElementById('pricePerDay');
        if (priceInput) { priceInput.value = price; }

        var streetInput = document.getElementById('create_start_point_street');
        if (streetInput) { streetInput.value = street; }

        var streetNumInput = document.getElementById('create_start_point_number');
        if (streetNumInput) { streetNumInput.value = streetNum; }

        var checkInInput = document.getElementById('checkInTime');
        if (checkInInput) { checkInInput.value = checkIn ? checkIn.substring(0, 5) : ''; }

        var checkOutInput = document.getElementById('checkOutTime');
        if (checkOutInput) { checkOutInput.value = checkOut ? checkOut.substring(0, 5) : ''; }

        if (nbId) { selectNeighborhood(nbId); }

        addOneEmptyRow();
        setFirstRowDates(from, to);
        applyReservedDayMarkers(reservedRanges);

        openFormSection();
    }

    function close() {
        closeFormSection();
        clearAvailabilityRows();
        if (formEl && formEl.reset) { formEl.reset(); }
        restoreInitialNeighborhood();
        setCreateMode();
        clearInlineAvailError();
        currentReservedRanges = [];
    }

    function openOnLoad(mode) {
        if (!formEl) { return; }
        if (mode === 'create') {
            setCreateMode();
        } else {
            setEditMode(mode);
        }
        openFormSection();
    }

    function init() {
        formSection    = document.getElementById('inlinePeriodFormSection');
        formEl         = document.getElementById('publishCarFormEl');
        titleEl        = document.getElementById('inlinePeriodFormTitle');
        submitLabelEl  = document.getElementById('inlineFormSubmitLabel');
        addBtn         = document.getElementById('publish_avail_add');
        rowsRoot       = document.getElementById('publish_availability_rows');

        if (!formSection || !formEl) { return; }

        createUrl      = formSection.getAttribute('data-create-url')      || '';
        editUrlPrefix  = formSection.getAttribute('data-edit-url-prefix') || '';
        editUrlSuffix  = formSection.getAttribute('data-edit-url-suffix') || '';
        originalCbuAttr= formEl.getAttribute('data-ryden-user-has-cbu')   || 'false';
        titleCreate    = (titleEl && titleEl.textContent.trim()) || '';
        titleEdit      = formSection.getAttribute('data-title-edit')  || titleCreate;
        submitLabelCreate = (submitLabelEl && submitLabelEl.textContent.trim()) || '';
        submitLabelEdit   = formSection.getAttribute('data-submit-edit') || submitLabelCreate;
        isFirstPeriod  = formSection.getAttribute('data-first-period') === 'true';

        var nbHidden = document.getElementById('nb_hid_publish');
        if (nbHidden && nbHidden.value) {
            initialNbId = nbHidden.value;
        }
        var nbText = document.getElementById('nb_dd_text_publish');
        if (nbText) {
            initialNbText = nbText.textContent || '';
        }

        var addPeriodBtn = document.getElementById('inlinePeriodAddBtn');
        if (addPeriodBtn) {
            addPeriodBtn.addEventListener('click', function () { openCreate(); });
        }

        var cancelBtn = document.getElementById('inlinePeriodFormCancelBtn');
        if (cancelBtn) {
            cancelBtn.addEventListener('click', function () { close(); });
        }

        // Hard guard: block submit when the current selection would orphan a reservation,
        // or when the range is incomplete in edit-with-reservations mode. The picker's
        // onChange already reverts invalid selections, but this catches mid-selection
        // submits (length<2), races with rapid clicks, hook failures, and DevTools
        // tampering with the hidden inputs.
        if (formEl) {
            formEl.addEventListener('submit', function (e) {
                if (currentReservedRanges.length === 0) { return; }
                if (!rowsRoot) { return; }
                var row = rowsRoot.querySelector('[data-publish-avail-row]');
                if (!row) { return; }
                var fromH  = row.querySelector('.ryden-avail-from');
                var untilH = row.querySelector('.ryden-avail-until');
                var fromVal  = fromH  ? fromH.value  : '';
                var untilVal = untilH ? untilH.value : '';
                if (!fromVal || !untilVal) {
                    e.preventDefault();
                    e.stopPropagation();
                    showInlineAvailError(getReservedShrinkMsg());
                    return;
                }
                var ok = rangeCoversAllReservations(
                        parseYmdLocal(fromVal),
                        parseYmdLocal(untilVal),
                        currentReservedRanges);
                if (!ok) {
                    e.preventDefault();
                    e.stopPropagation();
                    showInlineAvailError(getReservedShrinkMsg());
                }
            });
        }

        document.addEventListener('click', function (e) {
            var editBtn = e.target.closest('.js-period-edit');
            if (!editBtn) { return; }
            var card = editBtn.closest('.manage-period-card');
            if (card) { openEdit(card); }
        });

        // Click outside the form = implicit cancel.
        // Exclusions: inside the form, edit buttons, add button, inline owner calendar,
        // the Flatpickr popup (which Flatpickr appends to <body>, so it's NOT a descendant
        // of formSection), and modals. Additionally: don't auto-close while a client-side
        // validation error is showing — force the user to either fix it or hit Cancel.
        document.addEventListener('click', function (e) {
            if (!isOpen()) { return; }
            if (formSection.contains(e.target))                { return; }
            if (e.target.closest('.js-period-edit'))           { return; }
            if (e.target.closest('#inlinePeriodAddBtn'))       { return; }
            if (e.target.closest('.owner-cal-container'))      { return; }
            if (e.target.closest('.flatpickr-calendar'))       { return; }
            if (e.target.closest('.modal'))                    { return; }
            if (isInlineAvailErrorVisible())                   { return; }
            close();
        });
    }

    function getReservedShrinkMsg() {
        var sec = document.getElementById('publishAvailabilitySection');
        return (sec && sec.getAttribute('data-publish-reserved-shrink-msg'))
                || 'No podés excluir días con reservas activas.';
    }

    function isInlineAvailErrorVisible() {
        var box = document.getElementById('publishClientAvailError');
        return !!(box && !box.classList.contains('d-none') && box.textContent && box.textContent.trim() !== '');
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

    window.RydenManagePeriods = {
        openCreate: openCreate,
        openEdit: openEdit,
        close: close,
        openOnLoad: openOnLoad,
        confirmMinRentalDays: confirmMinRentalDays,
        isOpen: isOpen
    };
})();
