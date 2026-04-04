

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
        if (!hidden || !dateEl || !timeEl) {
            return null;
        }
        function syncToHidden() {
            hidden.value = combine(dateEl.value, timeEl.value);
        }
        function syncFromHidden() {
            var s = splitLocalDt(hidden.value);
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
        timeEl.addEventListener('change', syncToHidden);
        dateEl.addEventListener('input', syncToHidden);
        timeEl.addEventListener('input', syncToHidden);
        syncFromHidden();
        return { syncToHidden: syncToHidden, syncFromHidden: syncFromHidden, setDateBounds: setDateBounds };
    }

    function initWrappers() {
        document.querySelectorAll('[data-paw-dtpair-wrap]').forEach(function (wrap) {
            var hidId = wrap.getAttribute('data-paw-hidden');
            var dId = wrap.getAttribute('data-paw-date');
            var tId = wrap.getAttribute('data-paw-time');
            bindPair(
                document.getElementById(hidId),
                document.getElementById(dId),
                document.getElementById(tId)
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

/* Funciones de CarDetail */

(function () {
    const daterangeInput = document.getElementById('detail_daterange');
    const fromHidden = document.getElementById('detail_from_hidden');
    const untilHidden = document.getElementById('detail_until_hidden');
    const form = document.getElementById('detailReservationForm');
    const periodSelect = document.getElementById('detail_reservation_period');

    let minDate = null;
    let maxDate = null;

    // Función para parsear la fecha y hora ISO a objeto Date
    function parseDateTime(isoString) {
        if (!isoString) return null;
        return new Date(isoString);
    }

    // Función para formatear Date a ISO String (YYYY-MM-DDTHH:mm:ss)
    function formatDateTime(date) {
        if (!date) return '';
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        const hours = String(date.getHours()).padStart(2, '0');
        const minutes = String(date.getMinutes()).padStart(2, '0');
        const seconds = '00';
        return `${year}-${month}-${day}T${hours}:${minutes}:${seconds}`;
    }

    // Inicializar flatpickr en modo range
    const fp = flatpickr(daterangeInput, {
        mode: 'range',
        dateFormat: 'Y-m-d H:i',
        minDate: minDate,
        maxDate: maxDate,
        defaultDate: [
            fromHidden.value ? parseDateTime(fromHidden.value) : null,
            untilHidden.value ? parseDateTime(untilHidden.value) : null
        ],
        onChange: function(selectedDates) {
            if (selectedDates.length === 2) {
                fromHidden.value = formatDateTime(selectedDates[0]);
                untilHidden.value = formatDateTime(selectedDates[1]);
            } else if (selectedDates.length === 1) {
                fromHidden.value = formatDateTime(selectedDates[0]);
                untilHidden.value = '';
            } else {
                fromHidden.value = '';
                untilHidden.value = '';
            }
        }
    });

    // Actualizar restricciones de fechas cuando cambia el período
    function updateDateBounds() {
        if (periodSelect) {
            const selectedOption = periodSelect.options[periodSelect.selectedIndex];
            const minStr = selectedOption.getAttribute('data-min');
            const maxStr = selectedOption.getAttribute('data-max');

            if (minStr && maxStr) {
                minDate = parseDateTime(minStr);
                maxDate = parseDateTime(maxStr);
                fp.set('minDate', minDate);
                fp.set('maxDate', maxDate);
                // Limpiar selección cuando cambia el período
                fp.clear();
                fromHidden.value = '';
                untilHidden.value = '';
            }
        }
    }

    if (periodSelect) {
        periodSelect.addEventListener('change', updateDateBounds);
        // Aplicar restricciones iniciales si hay un solo período
        updateDateBounds();
    }

    // Sincronizar valores al submit del formulario
    if (form) {
        form.addEventListener('submit', function(e) {
            if (!fromHidden.value || !untilHidden.value) {
                e.preventDefault();
                alert('Please select both pickup and return dates');
                return false;
            }
        });
    }
})();

