

/* Funciones modal */
(function () {

    var OPEN_CLASS = 'is-open';
    var BODY_LOCK_CLASS = 'modal-open';
    var focusableSelector = 'a[href], area[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), iframe, [tabindex]:not([tabindex="-1"])';
    var modalState = new Map();

    function getModal(id) {
        return id ? document.getElementById(id) : null;
    }

    function getOpenModals() {
        return Array.prototype.slice.call(document.querySelectorAll('[data-modal].' + OPEN_CLASS));
    }

    function dispatchModalEvent(modal, eventName, action) {
        if (!modal) {
            return;
        }

        modal.dispatchEvent(new CustomEvent(eventName, {
            bubbles: true,
            detail: {
                id: modal.id,
                action: action || null
            }
        }));
    }

    function updateBodyLock() {
        if (getOpenModals().length > 0) {
            document.body.classList.add(BODY_LOCK_CLASS);
            return;
        }

        document.body.classList.remove(BODY_LOCK_CLASS);
    }

    function focusFirstElement(modal) {
        var dialog = modal.querySelector('.modal__dialog');
        var focusableElements = dialog ? dialog.querySelectorAll(focusableSelector) : [];

        if (focusableElements.length > 0) {
            focusableElements[0].focus();
            return;
        }

        if (dialog) {
            dialog.focus();
        }
    }

    function openModal(modal) {
        if (!modal || modal.classList.contains(OPEN_CLASS)) {
            return;
        }

        modalState.set(modal, {
            previousActiveElement: document.activeElement
        });

        modal.classList.add(OPEN_CLASS);
        modal.setAttribute('aria-hidden', 'false');
        updateBodyLock();
        focusFirstElement(modal);
        dispatchModalEvent(modal, 'paw:modal-open');
    }

    function closeModal(modal, action) {
        if (!modal || !modal.classList.contains(OPEN_CLASS)) {
            return;
        }

        modal.classList.remove(OPEN_CLASS);
        modal.setAttribute('aria-hidden', 'true');
        updateBodyLock();
        dispatchModalEvent(modal, 'paw:modal-close', action);

        var state = modalState.get(modal);
        if (state && state.previousActiveElement && typeof state.previousActiveElement.focus === 'function') {
            state.previousActiveElement.focus();
        }

        modalState.delete(modal);
    }

    function trapFocus(event, modal) {
        var dialog = modal.querySelector('.modal__dialog');
        if (!dialog) {
            return;
        }

        var focusableElements = Array.prototype.slice.call(dialog.querySelectorAll(focusableSelector));
        if (focusableElements.length === 0) {
            event.preventDefault();
            dialog.focus();
            return;
        }

        var firstElement = focusableElements[0];
        var lastElement = focusableElements[focusableElements.length - 1];

        if (event.shiftKey && document.activeElement === firstElement) {
            event.preventDefault();
            lastElement.focus();
            return;
        }

        if (!event.shiftKey && document.activeElement === lastElement) {
            event.preventDefault();
            firstElement.focus();
        }
    }

    document.addEventListener('click', function (event) {
        var openTrigger = event.target.closest('[data-modal-open]');
        if (openTrigger) {
            openModal(getModal(openTrigger.getAttribute('data-modal-open')));
            return;
        }

        var closeTrigger = event.target.closest('[data-modal-close]');
        if (!closeTrigger) {
            return;
        }

        var modal = getModal(closeTrigger.getAttribute('data-modal-close')) || closeTrigger.closest('[data-modal]');
        var action = closeTrigger.getAttribute('data-modal-action');
        closeModal(modal, action);
    });

    document.addEventListener('keydown', function (event) {
        var openModals = getOpenModals();
        var activeModal = openModals.length > 0 ? openModals[openModals.length - 1] : null;

        if (!activeModal) {
            return;
        }

        if (event.key === 'Escape') {
            closeModal(activeModal, 'escape');
            return;
        }

        if (event.key === 'Tab') {
            trapFocus(event, activeModal);
        }
    });

    document.addEventListener('DOMContentLoaded', function () {
        Array.prototype.forEach.call(document.querySelectorAll('[data-modal].' + OPEN_CLASS), function (modal) {
            modal.setAttribute('aria-hidden', 'false');
        });
        updateBodyLock();
    });

    window.PawModal = window.PawModal || {};
    window.PawModal.open = function (id) {
        openModal(getModal(id));
    };
    window.PawModal.close = function (id) {
        closeModal(getModal(id));
    };
})();

/* Bootstrap gallery modal: open carousel at clicked slide */
(function () {
    function initCarDetailGalleryModal() {
        var modalEl = document.getElementById('carDetailGalleryModal');
        if (!modalEl || typeof window.bootstrap === 'undefined') {
            return;
        }
        modalEl.addEventListener('show.bs.modal', function (event) {
            var trigger = event.relatedTarget;
            var idx = trigger ? parseInt(trigger.getAttribute('data-carousel-index') || '0', 10) : 0;
            var carouselEl = modalEl.querySelector('.carousel');
            if (!carouselEl) {
                return;
            }
            var items = carouselEl.querySelectorAll('.carousel-item');
            if (items.length > 0) {
                idx = Math.max(0, Math.min(idx, items.length - 1));
            }
            var instance = window.bootstrap.Carousel.getOrCreateInstance(carouselEl);
            instance.to(idx);
        });
    }

    document.addEventListener('DOMContentLoaded', initCarDetailGalleryModal);
})();

/* Funciones de PublishForm */
(function () {
    var input = document.getElementById("picturesInput");
    var preview = document.getElementById("picturesPreview");

    if (!input || !preview) {
        return;
    }

    input.addEventListener("change", function (event) {
        var files = event.target.files;
        preview.innerHTML = "";

        if (!files || files.length === 0) {
            return;
        }

        Array.prototype.forEach.call(files, function (file) {
            if (!file.type || file.type.indexOf("image/") !== 0) {
                return;
            }

            var col = document.createElement("div");
            col.className = "col-6 col-md-4";

            var card = document.createElement("div");
            card.className = "border rounded p-2";

            var img = document.createElement("img");
            img.className = "img-fluid rounded";
            img.style.height = "130px";
            img.style.objectFit = "cover";
            img.alt = file.name;

            var name = document.createElement("small");
            name.className = "d-block text-truncate mt-1";
            name.textContent = file.name;

            card.appendChild(img);
            card.appendChild(name);
            col.appendChild(card);
            preview.appendChild(col);

            var reader = new FileReader();
            reader.onload = function (e) {
                img.src = e.target.result;
            };
            reader.readAsDataURL(file);
        });
    });
})();


(function () {
    function splitLocalDt(iso) {
        if (!iso || typeof iso !== 'string') {
            return { d: '', t: '00:00' };
        }
        var parts = iso.trim().split('T');
        var d = parts[0] || '';
        var t = '00:00';
        if (parts.length > 1 && parts[1]) {
            t = parts[1].substring(0, 5);
        }
        return { d: d, t: t };
    }

    function combine(dVal, tVal) {
        if (!dVal) {
            return '';
        }
        return dVal + 'T' + (tVal || '00:00');
    }

    function bindPair(hidden, dateEl, timeEl) {
        if (!hidden || !dateEl) {
            return null;
        }
        function syncToHidden() {
            if (!timeEl) {
                hidden.value = dateEl.value || '';
                return;
            }
            hidden.value = combine(dateEl.value, timeEl.value);
        }
        function syncFromHidden() {
            var v = hidden.value || '';
            if (!timeEl) {
                dateEl.value = v.length >= 10 ? v.substring(0, 10) : '';
                return;
            }
            var s = splitLocalDt(v);
            dateEl.value = s.d;
            timeEl.value = s.t;
        }
        function setDateBounds(minFull, maxFull) {
            dateEl.removeAttribute('min');
            dateEl.removeAttribute('max');
            if (minFull && minFull.length >= 10) {
                dateEl.min = minFull.substring(0, 10);
            }
            if (maxFull && maxFull.length >= 10) {
                dateEl.max = maxFull.substring(0, 10);
            }
        }
        dateEl.addEventListener('change', syncToHidden);
        dateEl.addEventListener('input', syncToHidden);
        if (timeEl) {
            timeEl.addEventListener('change', syncToHidden);
            timeEl.addEventListener('input', syncToHidden);
        }
        syncFromHidden();
        return { syncToHidden: syncToHidden, syncFromHidden: syncFromHidden, setDateBounds: setDateBounds };
    }

    function initWrappers() {
        document.querySelectorAll('[data-paw-dtpair-wrap]').forEach(function (wrap) {
            var hidId = wrap.getAttribute('data-paw-hidden');
            var dId = wrap.getAttribute('data-paw-date');
            var tId = wrap.getAttribute('data-paw-time');
            var timeEl = (tId && tId.length) ? document.getElementById(tId) : null;
            bindPair(
                document.getElementById(hidId),
                document.getElementById(dId),
                timeEl
            );
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initWrappers);
    } else {
        initWrappers();
    }

    window.PawDateTimePair = {
        bindPair: bindPair,
        splitLocalDt: splitLocalDt,
        combine: combine
    };
})();

/* Flatpickr range helper (search, car detail, publish availability) */
(function () {
    function pad2(n) {
        return String(n).padStart(2, '0');
    }

    function parseIsoLocalDateTime(isoString) {
        if (!isoString) {
            return null;
        }
        return new Date(isoString);
    }

    function formatIsoLocalDateTime(date) {
        if (!date) {
            return '';
        }
        return date.getFullYear() + '-' + pad2(date.getMonth() + 1) + '-' + pad2(date.getDate()) + 'T' +
            pad2(date.getHours()) + ':' + pad2(date.getMinutes()) + ':00';
    }

    function formatIsoLocalDate(date) {
        if (!date) {
            return '';
        }
        return date.getFullYear() + '-' + pad2(date.getMonth() + 1) + '-' + pad2(date.getDate());
    }

    function normalizeWallHm(s) {
        if (!s) {
            return '00:00';
        }
        var t = String(s).trim();
        return t.length >= 5 ? t.substring(0, 5) : t;
    }

    function localNoonFromYmd(ymd) {
        if (!ymd || ymd.length < 10) {
            return null;
        }
        var p = ymd.substring(0, 10).split('-');
        if (p.length !== 3) {
            return null;
        }
        return new Date(parseInt(p[0], 10), parseInt(p[1], 10) - 1, parseInt(p[2], 10), 12, 0, 0);
    }

    function localWallDayStartFromYmd(ymd) {
        if (!ymd || ymd.length < 10) {
            return null;
        }
        var p = ymd.substring(0, 10).split('-');
        if (p.length !== 3) {
            return null;
        }
        return new Date(parseInt(p[0], 10), parseInt(p[1], 10) - 1, parseInt(p[2], 10), 0, 0, 0, 0);
    }

    function localWallDayEndFromYmd(ymd) {
        if (!ymd || ymd.length < 10) {
            return null;
        }
        var p = ymd.substring(0, 10).split('-');
        if (p.length !== 3) {
            return null;
        }
        return new Date(parseInt(p[0], 10), parseInt(p[1], 10) - 1, parseInt(p[2], 10), 23, 59, 59, 999);
    }

    /**
     * @param {object} opts
     * @param {object} [opts.combineWallTimes] if set, date-only range; hiddens get yyyy-MM-ddTHH:mm using these wall times
     * @param {Array<{from: Date, to: Date}>} [opts.enable] Flatpickr inclusive ranges; use local start/end-of-day, not noon
     * @param {boolean} [opts.disableAllDates] if true, no day is selectable
     * @returns {{ fp: object, setBounds: Function, clear: Function, destroy: Function } | null}
     */
    function initRange(opts) {
        if (typeof flatpickr === 'undefined') {
            return null;
        }
        var anchor = typeof opts.anchor === 'string' ? document.getElementById(opts.anchor) : opts.anchor;
        var fromHidden = typeof opts.fromHidden === 'string' ? document.getElementById(opts.fromHidden) : opts.fromHidden;
        var untilHidden = typeof opts.untilHidden === 'string' ? document.getElementById(opts.untilHidden) : opts.untilHidden;
        if (!anchor || !fromHidden || !untilHidden) {
            return null;
        }
        var combine = opts.combineWallTimes;
        var pickupHm = combine ? normalizeWallHm(combine.pickup) : '';
        var returnHm = combine ? normalizeWallHm(combine.returnDeadline) : '';
        var enableTime = combine ? false : !!opts.enableTime;
        var dateFormat = opts.dateFormat || (enableTime ? 'Y-m-d H:i' : 'Y-m-d');
        var fmtOut = enableTime ? formatIsoLocalDateTime : formatIsoLocalDate;
        var fpConfig = {
            mode: 'range',
            enableTime: enableTime,
            time_24hr: true,
            dateFormat: dateFormat,
            minDate: ('minDate' in opts) ? opts.minDate : 'today',
            maxDate: opts.maxDate != null ? opts.maxDate : undefined,
            defaultDate: opts.defaultDate,
            onChange: function (selectedDates) {
                if (combine) {
                    if (selectedDates.length === 2) {
                        fromHidden.value = formatIsoLocalDate(selectedDates[0]) + 'T' + pickupHm;
                        untilHidden.value = formatIsoLocalDate(selectedDates[1]) + 'T' + returnHm;
                    } else if (selectedDates.length === 1) {
                        fromHidden.value = formatIsoLocalDate(selectedDates[0]) + 'T' + pickupHm;
                        untilHidden.value = '';
                    } else {
                        fromHidden.value = '';
                        untilHidden.value = '';
                    }
                    return;
                }
                if (selectedDates.length === 2) {
                    fromHidden.value = fmtOut(selectedDates[0]);
                    untilHidden.value = fmtOut(selectedDates[1]);
                } else if (selectedDates.length === 1) {
                    fromHidden.value = fmtOut(selectedDates[0]);
                    untilHidden.value = '';
                } else {
                    fromHidden.value = '';
                    untilHidden.value = '';
                }
            }
        };
        if (opts.disableAllDates) {
            fpConfig.disable = [function () { return true; }];
        } else if (opts.enable && opts.enable.length > 0) {
            fpConfig.enable = opts.enable;
            fpConfig.disable = [];
        } else {
            fpConfig.disable = opts.disable || [];
        }
        var fp = flatpickr(anchor, fpConfig);
        return {
            fp: fp,
            setBounds: function (minD, maxD) {
                fp.set('minDate', minD != null ? minD : null);
                fp.set('maxDate', maxD != null ? maxD : null);
            },
            clear: function () {
                fp.clear();
                fromHidden.value = '';
                untilHidden.value = '';
            },
            destroy: function () {
                fp.destroy();
            }
        };
    }

    window.PawFlatpickrRange = {
        init: initRange,
        parseIsoLocalDateTime: parseIsoLocalDateTime,
        formatIsoLocalDateTime: formatIsoLocalDateTime,
        formatIsoLocalDate: formatIsoLocalDate,
        localNoonFromYmd: localNoonFromYmd,
        localWallDayStartFromYmd: localWallDayStartFromYmd,
        localWallDayEndFromYmd: localWallDayEndFromYmd
    };
})();

(function () {
    var fromPicker = document.getElementById('search_from_picker');
    var untilPicker = document.getElementById('search_until_picker');
    var fromHidden = document.getElementById('search_from_hidden');
    var untilHidden = document.getElementById('search_until_hidden');
    if (!fromPicker || !untilPicker || !fromHidden || !untilHidden || typeof flatpickr === 'undefined') {
        return;
    }
    function parseYmdToLocalNoon(ymd) {
        if (!ymd || ymd.length < 10) {
            return null;
        }
        var d = ymd.substring(0, 10).split('-');
        if (d.length !== 3) {
            return null;
        }
        return new Date(parseInt(d[0], 10), parseInt(d[1], 10) - 1, parseInt(d[2], 10), 12, 0, 0);
    }
    function initOne(anchor, hidden) {
        var def = parseYmdToLocalNoon(hidden.value);
        flatpickr(anchor, {
            mode: 'single',
            enableTime: false,
            dateFormat: 'Y-m-d',
            minDate: 'today',
            defaultDate: def || undefined,
            onChange: function (selectedDates) {
                hidden.value = selectedDates.length && window.PawFlatpickrRange
                    ? PawFlatpickrRange.formatIsoLocalDate(selectedDates[0])
                    : '';
            }
        });
    }
    initOne(fromPicker, fromHidden);
    initOne(untilPicker, untilHidden);
})();

/* Car detail: only days inside merged listing_availability segments are selectable (Flatpickr enable) */
(function () {
    var daterangeInput = document.getElementById('detail_daterange');
    var fromHidden = document.getElementById('detail_from_hidden');
    var untilHidden = document.getElementById('detail_until_hidden');
    var form = document.getElementById('detailReservationForm');
    if (!daterangeInput || !fromHidden || !untilHidden || !form || !window.PawFlatpickrRange) {
        return;
    }

    function parseBookableRangesJson(raw) {
        if (!raw || typeof raw !== 'string') {
            return [];
        }
        try {
            var parsed = JSON.parse(raw.trim());
            if (!Array.isArray(parsed)) {
                return [];
            }
            var out = [];
            for (var i = 0; i < parsed.length; i++) {
                var seg = parsed[i];
                if (!seg || typeof seg.from !== 'string' || typeof seg.to !== 'string') {
                    continue;
                }
                var fromD = PawFlatpickrRange.localWallDayStartFromYmd(seg.from);
                var toD = PawFlatpickrRange.localWallDayEndFromYmd(seg.to);
                if (fromD && toD) {
                    out.push({ from: fromD, to: toD });
                }
            }
            return out;
        } catch (e) {
            return [];
        }
    }

    var pickAttr = form.getAttribute('data-pickup-time') || '10:00';
    var retAttr = form.getAttribute('data-return-time') || '18:00';
    var segments = parseBookableRangesJson(form.getAttribute('data-bookable-ranges'));
    var enable = segments.map(function (s) {
        return { from: s.from, to: s.to };
    });

    var dd = [];
    if (fromHidden.value && fromHidden.value.length >= 10) {
        var d1 = PawFlatpickrRange.localWallDayStartFromYmd(fromHidden.value);
        if (d1) {
            dd.push(d1);
        }
    }
    if (untilHidden.value && untilHidden.value.length >= 10) {
        var d2 = PawFlatpickrRange.localWallDayStartFromYmd(untilHidden.value);
        if (d2) {
            dd.push(d2);
        }
    }

    var initOpts = {
        anchor: daterangeInput,
        fromHidden: fromHidden,
        untilHidden: untilHidden,
        combineWallTimes: { pickup: pickAttr, returnDeadline: retAttr },
        dateFormat: 'Y-m-d',
        defaultDate: dd.length ? dd : undefined
    };
    if (enable.length > 0) {
        initOpts.enable = enable;
    } else {
        initOpts.disableAllDates = true;
    }

    var ctrl = PawFlatpickrRange.init(initOpts);
    if (!ctrl) {
        return;
    }

    form.addEventListener('submit', function (e) {
        if (!fromHidden.value || !untilHidden.value) {
            e.preventDefault();
            alert('Please select both pickup and return dates');
            return false;
        }
    });
})();

/* Publish: dynamic availability rows (date-only range) */
(function () {
    var root = document.getElementById('publish_availability_rows');
    var tpl = document.getElementById('publish_avail_row_template');
    var addBtn = document.getElementById('publish_avail_add');
    if (!root || !tpl || !addBtn || !window.PawFlatpickrRange) {
        return;
    }

    var MAX_ROWS = 10;

    function reindexRows() {
        var rows = root.querySelectorAll('[data-publish-avail-row]');
        rows.forEach(function (row, i) {
            row.querySelectorAll('.publish-avail-index').forEach(function (el) {
                el.textContent = String(i + 1);
            });
            var fromH = row.querySelector('.paw-avail-from');
            var untilH = row.querySelector('.paw-avail-until');
            if (fromH) {
                fromH.name = 'availabilityRows[' + i + '].from';
            }
            if (untilH) {
                untilH.name = 'availabilityRows[' + i + '].until';
            }
        });
    }

    function initRow(row) {
        var anchor = row.querySelector('.paw-avail-range-input');
        var fromH = row.querySelector('.paw-avail-from');
        var untilH = row.querySelector('.paw-avail-until');
        if (!anchor || !fromH || !untilH) {
            return;
        }
        var defaults = [];
        if (fromH.value && untilH.value) {
            defaults.push(new Date(fromH.value + 'T00:00:00'));
            defaults.push(new Date(untilH.value + 'T00:00:00'));
        }
        var c = PawFlatpickrRange.init({
            anchor: anchor,
            fromHidden: fromH,
            untilHidden: untilH,
            enableTime: false,
            dateFormat: 'Y-m-d',
            defaultDate: defaults.length ? defaults : undefined
        });
        row._pawAvailFp = c;
    }

    function destroyRowFp(row) {
        if (row._pawAvailFp && row._pawAvailFp.fp) {
            row._pawAvailFp.destroy();
        }
        row._pawAvailFp = null;
    }

    root.querySelectorAll('[data-publish-avail-row]').forEach(initRow);

    addBtn.addEventListener('click', function () {
        var n = root.querySelectorAll('[data-publish-avail-row]').length;
        if (n >= MAX_ROWS) {
            return;
        }
        var html = tpl.innerHTML.replace(/__IDX__/g, '0');
        var wrap = document.createElement('div');
        wrap.innerHTML = html.trim();
        var row = wrap.firstElementChild;
        root.appendChild(row);
        reindexRows();
        initRow(row);
    });

    root.addEventListener('click', function (e) {
        var btn = e.target.closest('.publish-avail-remove');
        if (!btn) {
            return;
        }
        var row = btn.closest('[data-publish-avail-row]');
        if (!row || !root.contains(row)) {
            return;
        }
        var rows = root.querySelectorAll('[data-publish-avail-row]');
        if (rows.length <= 1) {
            destroyRowFp(row);
            row.querySelectorAll('.paw-avail-from, .paw-avail-until').forEach(function (h) { h.value = ''; });
            var anchor = row.querySelector('.paw-avail-range-input');
            if (anchor) {
                anchor.value = '';
            }
            initRow(row);
            return;
        }
        destroyRowFp(row);
        row.remove();
        reindexRows();
        root.querySelectorAll('[data-publish-avail-row]').forEach(function (r) {
            destroyRowFp(r);
        });
        root.querySelectorAll('[data-publish-avail-row]').forEach(initRow);
    });
})();

