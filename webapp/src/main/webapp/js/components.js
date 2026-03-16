

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
