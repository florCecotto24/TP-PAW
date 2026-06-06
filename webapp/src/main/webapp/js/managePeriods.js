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
            fp.setDate([
                new Date(from + 'T00:00:00'),
                new Date(to   + 'T00:00:00')
            ], true);
        } else {
            var fromH = row.querySelector('.ryden-avail-from');
            var untilH = row.querySelector('.ryden-avail-until');
            var display = row.querySelector('.ryden-avail-range-input');
            if (fromH)   { fromH.value   = from; }
            if (untilH)  { untilH.value  = to; }
            if (display) { display.value = from + ' – ' + to; }
        }
    }

    function resetNeighborhoodPicker() {
        var hidden = document.getElementById('nb_hid_publish');
        if (hidden) { hidden.value = ''; }
        document.querySelectorAll('[id^="nb_rb_publish_"]:checked').forEach(function (rb) {
            rb.checked = false;
        });
        var text = document.getElementById('nb_dd_text_publish');
        if (text) {
            var placeholder = text.getAttribute('data-placeholder') || '';
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

        openFormSection();
    }

    function close() {
        closeFormSection();
        clearAvailabilityRows();
        if (formEl && formEl.reset) { formEl.reset(); }
        restoreInitialNeighborhood();
        setCreateMode();
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

        document.addEventListener('click', function (e) {
            var editBtn = e.target.closest('.js-period-edit');
            if (!editBtn) { return; }
            var card = editBtn.closest('.manage-period-card');
            if (card) { openEdit(card); }
        });

        // Click outside the form = implicit cancel.
        // Exclusions: inside the form, edit buttons, add button, calendar, and modals.
        document.addEventListener('click', function (e) {
            if (!isOpen()) { return; }
            if (formSection.contains(e.target))                { return; }
            if (e.target.closest('.js-period-edit'))           { return; }
            if (e.target.closest('#inlinePeriodAddBtn'))       { return; }
            if (e.target.closest('.owner-cal-container'))      { return; }
            if (e.target.closest('.modal'))                    { return; }
            close();
        });
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
