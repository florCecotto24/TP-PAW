/* Character validation: phone (only digits and +), numeric codes, plate (alphanumeric). */
(function () {
    function maxLenAttr(el) {
        var m = parseInt(el.getAttribute("maxlength"), 10);
        if (!isNaN(m) && m > 0) {
            return m;
        }
        m = parseInt(el.getAttribute("data-max-len"), 10);
        return !isNaN(m) && m > 0 ? m : 999;
    }

    function bindPhone(el) {
        var cap = maxLenAttr(el);
        el.addEventListener("beforeinput", function (e) {
            if (e.data === null || e.data === "") {
                return;
            }
            if (!/^[0-9+]+$/.test(e.data)) {
                e.preventDefault();
            }
        });
        el.addEventListener("input", function () {
            el.value = el.value.replace(/[^0-9+]/g, "").slice(0, cap);
        });
        el.addEventListener("paste", function (e) {
            var raw = (e.clipboardData || window.clipboardData).getData("text");
            var cleaned = String(raw || "").replace(/[^0-9+]/g, "").slice(0, cap);
            e.preventDefault();
            var start = el.selectionStart;
            var end = el.selectionEnd;
            el.value = el.value.slice(0, start) + cleaned + el.value.slice(end);
            var pos = start + cleaned.length;
            el.setSelectionRange(pos, pos);
        });
    }

    function bindDigitsOnly(el) {
        var cap = maxLenAttr(el);
        el.addEventListener("beforeinput", function (e) {
            if (e.data === null || e.data === "") {
                return;
            }
            if (!/^\d$/.test(e.data)) {
                e.preventDefault();
            }
        });
        el.addEventListener("input", function () {
            el.value = el.value.replace(/\D/g, "").slice(0, cap);
        });
        el.addEventListener("paste", function (e) {
            var raw = (e.clipboardData || window.clipboardData).getData("text");
            var cleaned = String(raw || "").replace(/\D/g, "").slice(0, cap);
            e.preventDefault();
            var start = el.selectionStart;
            var end = el.selectionEnd;
            el.value = el.value.slice(0, start) + cleaned + el.value.slice(end);
            var pos = start + cleaned.length;
            el.setSelectionRange(pos, pos);
        });
    }

    function bindPlate(el) {
        var cap = maxLenAttr(el);
        el.addEventListener("beforeinput", function (e) {
            if (e.data === null || e.data === "") {
                return;
            }
            if (!/^[A-Za-z0-9]$/.test(e.data)) {
                e.preventDefault();
            }
        });
        el.addEventListener("input", function () {
            el.value = el.value.toUpperCase().replace(/[^A-Z0-9]/g, "").slice(0, cap);
        });
        el.addEventListener("paste", function (e) {
            var raw = (e.clipboardData || window.clipboardData).getData("text");
            var cleaned = String(raw || "").toUpperCase().replace(/[^A-Z0-9]/g, "").slice(0, cap);
            e.preventDefault();
            var start = el.selectionStart;
            var end = el.selectionEnd;
            el.value = el.value.slice(0, start) + cleaned + el.value.slice(end);
            var pos = start + cleaned.length;
            el.setSelectionRange(pos, pos);
        });
    }

    function bindNoPunctuation(el) {
        var cap = maxLenAttr(el);
        el.addEventListener("beforeinput", function (e) {
            if (e.isComposing) return;
            if (e.data === null || e.data === "") {
                return;
            }
            if (/[^\p{L}\p{M}\p{N}\s]/u.test(e.data)) {
                e.preventDefault();
            }
        });
        el.addEventListener("input", function (e) {
            if (e.isComposing) return;
            el.value = el.value.replace(/[^\p{L}\p{M}\p{N}\s]/gu, "").slice(0, cap);
        });
        el.addEventListener("compositionend", function () {
            el.value = el.value.replace(/[^\p{L}\p{M}\p{N}\s]/gu, "").slice(0, cap);
        });
        el.addEventListener("paste", function (e) {
            var raw = (e.clipboardData || window.clipboardData).getData("text");
            var cleaned = String(raw || "").replace(/[^\p{L}\p{M}\p{N}\s]/gu, "").slice(0, cap);
            e.preventDefault();
            var start = el.selectionStart;
            var end = el.selectionEnd;
            el.value = el.value.slice(0, start) + cleaned + el.value.slice(end);
            var pos = start + cleaned.length;
            el.setSelectionRange(pos, pos);
        });
    }

    document.addEventListener("DOMContentLoaded", function () {
        document.querySelectorAll("input[data-ryden-phone]").forEach(bindPhone);
        document.querySelectorAll("input[data-ryden-digits-only]").forEach(bindDigitsOnly);
        document.querySelectorAll("input[data-ryden-plate]").forEach(bindPlate);
        document.querySelectorAll("input[data-ryden-no-punctuation]").forEach(bindNoPunctuation);
    });
})();

/* Disable auth submit buttons after first submit to prevent double posting. */
(function () {
    function bindSingleSubmit(form) {
        if (!form || form.getAttribute("data-ryden-single-submit-bound") === "1") {
            return;
        }
        form.setAttribute("data-ryden-single-submit-bound", "1");

        form.addEventListener("submit", function (event) {
            if (form.getAttribute("data-ryden-submitting") === "1") {
                event.preventDefault();
                return;
            }

            window.setTimeout(function () {
                if (event.defaultPrevented) {
                    return;
                }
                if (typeof form.checkValidity === "function" && !form.checkValidity()) {
                    return;
                }

                var submitter = event.submitter;
                if (!submitter || submitter.form !== form) {
                    submitter = form.querySelector("button[type='submit'], input[type='submit']");
                }
                if (!submitter) {
                    return;
                }

                form.setAttribute("data-ryden-submitting", "1");
                submitter.disabled = true;
                submitter.classList.add("disabled");
                submitter.setAttribute("aria-disabled", "true");
                submitter.setAttribute("aria-busy", "true");
            }, 0);
        });
    }

    document.addEventListener("DOMContentLoaded", function () {
        document.querySelectorAll("form[data-ryden-disable-submit-once='true']").forEach(bindSingleSubmit);
    });
})();

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
        dispatchModalEvent(modal, 'ryden:modal-open');
    }

    function closeModal(modal, action) {
        if (!modal || !modal.classList.contains(OPEN_CLASS)) {
            return;
        }

        modal.classList.remove(OPEN_CLASS);
        modal.setAttribute('aria-hidden', 'true');
        updateBodyLock();
        dispatchModalEvent(modal, 'ryden:modal-close', action);

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

    window.RydenModal = window.RydenModal || {};
    window.RydenModal.open = function (id) {
        openModal(getModal(id));
    };
    window.RydenModal.close = function (id) {
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

/* Publish car form: early image validation (red message, no alert) + submission */
(function () {
    function rydenParseMaxImageBytes(inputEl) {
        if (!inputEl) {
            return NaN;
        }
        var raw = inputEl.getAttribute("data-upload-max-image-bytes");
        if (!raw) {
            return NaN;
        }
        var n = parseInt(raw, 10);
        return isNaN(n) ? NaN : n;
    }

    function rydenImageTooLargeMessage(inputEl) {
        if (!inputEl) {
            return "";
        }
        return inputEl.getAttribute("data-upload-image-too-large") || "";
    }

    function rydenNotImageMessage(inputEl) {
        if (!inputEl) {
            return "";
        }
        return inputEl.getAttribute("data-upload-not-image-msg") || "";
    }

    var input = document.getElementById("picturesInput");
    var preview = document.getElementById("picturesPreview");
    var form = document.getElementById("publishCarFormEl");
    var submitBtn = document.getElementById("publishCarSubmitBtn");
    var errEl = document.getElementById("publishPicturesClientError");
    var labelEl = document.getElementById("picturesChooseLabel");
    var requiredMsg = (input && input.getAttribute("data-publish-pictures-required")) || "";

    function hidePublishPicturesClientError() {
        if (errEl) {
            errEl.textContent = "";
            errEl.classList.add("d-none");
        }
        if (labelEl) {
            labelEl.classList.remove("is-invalid");
        }
    }

    function showPublishPicturesClientError(msg) {
        if (errEl) {
            errEl.textContent = msg || "";
            errEl.classList.toggle("d-none", !msg);
        }
        if (labelEl && msg) {
            labelEl.classList.add("is-invalid");
        }
    }

    function validatePublishPictureFiles(files) {
        if (!files || files.length === 0) {
            return requiredMsg || "";
        }
        var maxBytes = rydenParseMaxImageBytes(input);
        var tooLargeMsg = rydenImageTooLargeMessage(input);
        if (maxBytes > 0) {
            var i;
            for (i = 0; i < files.length; i++) {
                if (files[i].size > maxBytes) {
                    return tooLargeMsg || "";
                }
            }
        }
        var notImgMsg = rydenNotImageMessage(input);
        var j;
        for (j = 0; j < files.length; j++) {
            var fj = files[j];
            var t = (fj.type || "").toLowerCase();
            if (!t || t.indexOf("image/") !== 0) {
                return notImgMsg || "";
            }
        }
        return "";
    }

    var retainedBlock = document.getElementById("publishRetainedPictures");

    var selectedFiles = [];

    function renderPreview() {
        preview.innerHTML = "";
        if (retainedBlock) {
            if (selectedFiles.length > 0) {
                retainedBlock.classList.add("d-none");
            } else {
                retainedBlock.classList.remove("d-none");
            }
        }

        var dt = new DataTransfer();
        selectedFiles.forEach(function(f) { dt.items.add(f); });
        input.files = dt.files;

        if (selectedFiles.length === 0) {
            return;
        }

        var err = validatePublishPictureFiles(selectedFiles);
        if (err) {
            showPublishPicturesClientError(err);
            selectedFiles = [];
            input.value = "";
            renderPreview();
            return;
        }

        selectedFiles.forEach(function (file, index) {
            var col = document.createElement("div");
            col.className = "col-6 col-md-4";

            var card = document.createElement("div");
            card.className = "border rounded p-2 position-relative";

            var img = document.createElement("img");
            img.className = "img-fluid rounded";
            img.style.height = "130px";
            img.style.objectFit = "cover";
            img.style.width = "100%";
            img.alt = file.name;

            var name = document.createElement("small");
            name.className = "d-block text-truncate mt-1";
            name.textContent = file.name;

            var removeBtn = document.createElement("button");
            removeBtn.type = "button";
            removeBtn.className = "btn btn-sm btn-danger position-absolute top-0 end-0 m-1";
            removeBtn.setAttribute("aria-label", "Remove image");
            removeBtn.innerHTML = '<i class="bi bi-trash" aria-hidden="true"></i>';

            removeBtn.addEventListener("click", function() {
                selectedFiles.splice(index, 1);
                renderPreview();
            });

            card.appendChild(img);
            card.appendChild(name);
            card.appendChild(removeBtn);
            col.appendChild(card);
            preview.appendChild(col);

            var reader = new FileReader();
            reader.onload = function (e) {
                img.src = e.target.result;
            };
            reader.readAsDataURL(file);
        });
    }

    if (input && preview) {
        input.addEventListener("change", function (event) {
            hidePublishPicturesClientError();
            var newFiles = event.target.files;

            if (newFiles && newFiles.length > 0) {
                for (var i = 0; i < newFiles.length; i++) {
                    var isDuplicate = false;
                    for (var j = 0; j < selectedFiles.length; j++) {
                        if (selectedFiles[j].name === newFiles[i].name && selectedFiles[j].size === newFiles[i].size) {
                            isDuplicate = true;
                            break;
                        }
                    }
                    if (!isDuplicate) {
                        selectedFiles.push(newFiles[i]);
                    }
                }
            }
            renderPreview();
        });
    }

    if (retainedBlock) {
        var btnNodes = retainedBlock.querySelectorAll('.ryden-publish-remove-retained-btn');
        for (var i = 0; i < btnNodes.length; i++) {
            btnNodes[i].addEventListener('click', function(e) {
                e.preventDefault();
                var btn = e.currentTarget;
                var url = btn.getAttribute('data-remove-url');
                var col = btn.closest('[data-retained-picture-col]');

                var csrfInput = form ? form.querySelector('input[name="_csrf"]') : null;
                var formData = new URLSearchParams();
                if (csrfInput) {
                    formData.append('_csrf', csrfInput.value);
                }

                if (url && col) {
                    btn.disabled = true;
                    fetch(url, {
                        method: 'POST',
                        body: formData
                    }).then(function(res) {
                        if (res.ok) {
                            col.remove();
                            // If it's the last one, hide the container.
                            if (retainedBlock.querySelectorAll('[data-retained-picture-col]').length === 0) {
                                retainedBlock.classList.add("d-none");
                            }
                        } else {
                            btn.disabled = false;
                        }
                    }).catch(function(err) {
                        btn.disabled = false;
                    });
                }
            });
        }
    }

    function publishCarRetainedPictureDomCount() {
        if (!retainedBlock) {
            return 0;
        }
        return retainedBlock.querySelectorAll('[data-retained-picture-col]').length;
    }

    if (form && submitBtn) {
        form.addEventListener("submit", function (e) {
            if (submitBtn.disabled) {
                return;
            }
            if (input) {
                var retainedAttr = 0;
                if (form && form.getAttribute) {
                    retainedAttr = parseInt(form.getAttribute("data-publish-retained-count") || "0", 10) || 0;
                }
                var retainedDom = publishCarRetainedPictureDomCount();
                var retained = Math.max(retainedAttr, retainedDom);
                var hasFiles = input.files && input.files.length > 0;
                var err = "";
                if (!hasFiles && retained > 0) {
                    err = "";
                } else {
                    err = validatePublishPictureFiles(input.files);
                }
                if (err) {
                    e.preventDefault();
                    e.stopPropagation();
                    showPublishPicturesClientError(err);
                    return;
                }
            }
            submitBtn.disabled = true;
            submitBtn.classList.add("disabled");
            var def = submitBtn.querySelector(".publish-submit-default");
            var load = submitBtn.querySelector(".publish-submit-loading");
            if (def) {
                def.classList.add("d-none");
            }
            if (load) {
                load.classList.remove("d-none");
            }
            submitBtn.setAttribute("aria-busy", "true");
        });
    }
})();

(function () {
    function rydenParseMaxImageBytes(inputEl) {
        if (!inputEl) {
            return NaN;
        }
        var raw = inputEl.getAttribute("data-upload-max-image-bytes");
        if (!raw) {
            return NaN;
        }
        var n = parseInt(raw, 10);
        return isNaN(n) ? NaN : n;
    }

    function rydenImageTooLargeMessage(inputEl) {
        if (!inputEl) {
            return "";
        }
        return inputEl.getAttribute("data-upload-image-too-large") || "";
    }

    function rydenNotImageMessage(inputEl) {
        if (!inputEl) {
            return "";
        }
        return inputEl.getAttribute("data-upload-not-image-msg") || "";
    }

    var profileInput = document.getElementById("profilePictureInput");
    if (!profileInput) {
        return;
    }

    var profileClientErr = document.getElementById("profilePictureClientError");

    function rydenShowProfileClientError(msg) {
        if (!profileClientErr) {
            return;
        }
        profileClientErr.textContent = msg || "";
        profileClientErr.classList.toggle("d-none", !msg);
    }

    profileInput.addEventListener("change", function () {
        rydenShowProfileClientError("");
        var file = profileInput.files && profileInput.files[0];
        if (!file) {
            return;
        }
        var maxBytes = rydenParseMaxImageBytes(profileInput);
        var tooLargeMsg = rydenImageTooLargeMessage(profileInput);
        if (maxBytes > 0 && file.size > maxBytes) {
            if (tooLargeMsg) {
                rydenShowProfileClientError(tooLargeMsg);
            }
            profileInput.value = "";
            return;
        }
        var ft = (file.type || "").toLowerCase();
        if (!ft || ft.indexOf("image/") !== 0) {
            var nim = rydenNotImageMessage(profileInput);
            if (nim) {
                rydenShowProfileClientError(nim);
            }
            profileInput.value = "";
        }
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
        document.querySelectorAll('[data-ryden-dtpair-wrap]').forEach(function (wrap) {
            var hidId = wrap.getAttribute('data-ryden-hidden');
            var dId = wrap.getAttribute('data-ryden-date');
            var tId = wrap.getAttribute('data-ryden-time');
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

    window.RydenDateTimePair = {
        bindPair: bindPair,
        splitLocalDt: splitLocalDt,
        combine: combine
    };
})();

/* Flatpickr range helper (search, publish availability; car-detail reservation uses detailReservationForm.js) */
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
     * @param {number} [opts.showMonths] calendars side-by-side (default 2 for cross-month ranges)
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
        var showMonths = 2;
        if (typeof opts.showMonths === 'number' && opts.showMonths >= 1 && opts.showMonths <= 6) {
            showMonths = Math.floor(opts.showMonths);
        }
        var fpConfig = {
            mode: 'range',
            showMonths: showMonths,
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

    window.RydenFlatpickrRange = {
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
    if (typeof flatpickr === 'undefined') {
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
    function initPair(fromAnchor, fromHidden, untilAnchor, untilHidden) {
        var fromDef = parseYmdToLocalNoon(fromHidden.value);
        var untilDef = parseYmdToLocalNoon(untilHidden.value);

        function dayTs(d) {
            return new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime();
        }
        function clearHoverClasses() {
            untilFp.calendarContainer.querySelectorAll('.inRange, .endRange').forEach(function (el) {
                el.classList.remove('inRange', 'endRange');
            });
        }

        var untilFp = flatpickr(untilAnchor, {
            mode: 'single',
            enableTime: false,
            dateFormat: 'Y-m-d',
            showMonths: 2,
            minDate: fromDef || 'today',
            defaultDate: untilDef || undefined,
            onDayCreate: function (dObj, dStr, fp, dayElem) {
                if (!fromHidden.value || !dayElem.dateObj) { return; }
                var fromDate = parseYmdToLocalNoon(fromHidden.value);
                if (!fromDate) { return; }
                if (dayTs(dayElem.dateObj) === dayTs(fromDate)) {
                    dayElem.classList.add('startRange');
                }
            },
            onChange: function (selectedDates) {
                untilHidden.value = selectedDates.length && window.RydenFlatpickrRange
                    ? RydenFlatpickrRange.formatIsoLocalDate(selectedDates[0])
                    : '';
            }
        });

        untilFp.calendarContainer.addEventListener('mouseover', function (e) {
            var day = e.target.closest('.flatpickr-day');
            if (!day || !day.dateObj || !fromHidden.value) { return; }
            var fromDate = parseYmdToLocalNoon(fromHidden.value);
            if (!fromDate) { return; }
            var fromTs = dayTs(fromDate);
            var hovTs = dayTs(day.dateObj);
            clearHoverClasses();
            if (hovTs <= fromTs) { return; }
            untilFp.calendarContainer.querySelectorAll('.flatpickr-day').forEach(function (dayEl) {
                if (!dayEl.dateObj) { return; }
                var dTs = dayTs(dayEl.dateObj);
                if (dTs > fromTs && dTs < hovTs) {
                    dayEl.classList.add('inRange');
                }
                if (dTs === hovTs) {
                    dayEl.classList.add('endRange');
                }
            });
        });

        untilFp.calendarContainer.addEventListener('mouseleave', function () {
            clearHoverClasses();
        });

        flatpickr(fromAnchor, {
            mode: 'single',
            enableTime: false,
            dateFormat: 'Y-m-d',
            showMonths: 2,
            minDate: 'today',
            defaultDate: fromDef || undefined,
            onChange: function (selectedDates, dateStr, instance) {
                fromHidden.value = selectedDates.length && window.RydenFlatpickrRange
                    ? RydenFlatpickrRange.formatIsoLocalDate(selectedDates[0])
                    : '';
                if (selectedDates.length) {
                    instance.close();
                    untilFp.set('minDate', selectedDates[0]);
                    if (untilFp.selectedDates.length && untilFp.selectedDates[0] < selectedDates[0]) {
                        untilFp.clear();
                        untilHidden.value = '';
                    }
                    untilFp.open();
                }
            }
        });
    }
    document.querySelectorAll('[id^="search_from_picker_"]').forEach(function (fromPicker) {
        var suffix = fromPicker.id.replace(/^search_from_picker_/, '');
        var untilPicker = document.getElementById('search_until_picker_' + suffix);
        var fromHidden = document.getElementById('search_from_hidden_' + suffix);
        var untilHidden = document.getElementById('search_until_hidden_' + suffix);
        if (!untilPicker || !fromHidden || !untilHidden) {
            return;
        }
        initPair(fromPicker, fromHidden, untilPicker, untilHidden);
    });
    var fp0 = document.getElementById('search_from_picker');
    var up0 = document.getElementById('search_until_picker');
    var fh0 = document.getElementById('search_from_hidden');
    var uh0 = document.getElementById('search_until_hidden');
    if (fp0 && up0 && fh0 && uh0) {
        initPair(fp0, fh0, up0, uh0);
    }
})();

/* Perfil: fecha de nacimiento (Flatpickr single, Y-m-d, coherente con búsqueda/reservas) */
(function () {
    var birthEl = document.getElementById("profileBirthDateInput");
    if (!birthEl || typeof flatpickr === "undefined" || !window.RydenFlatpickrRange) {
        return;
    }
    var maxYmd = birthEl.getAttribute("data-max-ymd");
    var initial = (birthEl.value && birthEl.value.length >= 10)
        ? RydenFlatpickrRange.localNoonFromYmd(birthEl.value.substring(0, 10))
        : null;
    flatpickr(birthEl, {
        mode: "single",
        enableTime: false,
        dateFormat: "Y-m-d",
        minDate: "1900-01-01",
        maxDate: maxYmd || "today",
        defaultDate: initial || undefined,
        allowInput: false,
        onChange: function (selectedDates) {
            birthEl.value = selectedDates.length && window.RydenFlatpickrRange
                ? RydenFlatpickrRange.formatIsoLocalDate(selectedDates[0])
                : "";
        },
        onReady: function (selectedDates, dateStr, fp) {
            var clearLabel = birthEl.getAttribute("data-clear-label") || "Borrar selección";
            var footer = document.createElement("div");
            footer.className = "flatpickr-profile-clear-footer";
            var btn = document.createElement("button");
            btn.type = "button";
            btn.className = "flatpickr-profile-clear-btn";
            btn.textContent = clearLabel;
            btn.addEventListener("click", function () {
                fp.clear();
                birthEl.value = "";
            });
            footer.appendChild(btn);
            fp.calendarContainer.appendChild(footer);
        }
    });
})();

/* Publish: dynamic availability rows (date-only range) */
(function () {
    var root = document.getElementById('publish_availability_rows');
    var tpl = document.getElementById('publish_avail_row_template');
    var addBtn = document.getElementById('publish_avail_add');
    var section = document.getElementById('publishAvailabilitySection');
    if (!root || !tpl || !addBtn || !window.RydenFlatpickrRange) {
        return;
    }

    var MAX_ROWS = 10;

    function readPublishMinAvailYmd() {
        if (!section) {
            return '';
        }
        return (section.getAttribute('data-publish-min-avail-ymd') || '').trim();
    }

    function minDateForPublish() {
        var ymd = readPublishMinAvailYmd();
        if (ymd) {
            var d = RydenFlatpickrRange.localWallDayStartFromYmd(ymd);
            if (d) {
                return d;
            }
        }
        return 'today';
    }

    function setSectionMinYmd(ymd) {
        if (section && ymd) {
            section.setAttribute('data-publish-min-avail-ymd', ymd);
        }
    }

    function refreshAllPublishRowsMinDateFromServer() {
        var urlBase = window.rydenPublishAvailMinFromUrl;
        if (!urlBase) {
            return;
        }
        var timeEl = document.getElementById('checkInTime');
        var t = timeEl && timeEl.value ? String(timeEl.value) : '10:00';
        if (t.length >= 5) {
            t = t.substring(0, 5);
        }
        var sep = urlBase.indexOf('?') >= 0 ? '&' : '?';
        fetch(urlBase + sep + 'checkIn=' + encodeURIComponent(t), { credentials: 'same-origin' })
            .then(function (r) {
                if (!r.ok) {
                    return null;
                }
                return r.json();
            })
            .then(function (data) {
                if (!data || !data.minFrom) {
                    return;
                }
                setSectionMinYmd(data.minFrom);
                var minD = RydenFlatpickrRange.localWallDayStartFromYmd(data.minFrom);
                if (!minD) {
                    return;
                }
                root.querySelectorAll('[data-publish-avail-row]').forEach(function (rrow) {
                    if (rrow._rydenAvailFp && rrow._rydenAvailFp.fp) {
                        rrow._rydenAvailFp.fp.set('minDate', minD);
                    }
                });
            })
            .catch(function () { /* ignore */ });
    }

    function syncAddBtn() {
        var n = root.querySelectorAll('[data-publish-avail-row]').length;
        addBtn.classList.toggle('d-none', n >= MAX_ROWS);
    }

    function reindexRows() {
        var rows = root.querySelectorAll('[data-publish-avail-row]');
        rows.forEach(function (row, i) {
            row.querySelectorAll('.publish-avail-index').forEach(function (el) {
                el.textContent = String(i + 1);
            });
            var fromH = row.querySelector('.ryden-avail-from');
            var untilH = row.querySelector('.ryden-avail-until');
            if (fromH) {
                fromH.name = 'availabilityRows[' + i + '].from';
            }
            if (untilH) {
                untilH.name = 'availabilityRows[' + i + '].until';
            }
        });
    }

    function initRow(row) {
        var anchor = row.querySelector('.ryden-avail-range-input');
        var fromH = row.querySelector('.ryden-avail-from');
        var untilH = row.querySelector('.ryden-avail-until');
        if (!anchor || !fromH || !untilH) {
            return;
        }
        var defaults = [];
        if (fromH.value && untilH.value) {
            defaults.push(new Date(fromH.value + 'T00:00:00'));
            defaults.push(new Date(untilH.value + 'T00:00:00'));
        }
        var c = RydenFlatpickrRange.init({
            anchor: anchor,
            fromHidden: fromH,
            untilHidden: untilH,
            enableTime: false,
            dateFormat: 'Y-m-d',
            minDate: minDateForPublish(),
            defaultDate: defaults.length ? defaults : undefined
        });
        row._rydenAvailFp = c;
    }

    function destroyRowFp(row) {
        if (row._rydenAvailFp && row._rydenAvailFp.fp) {
            row._rydenAvailFp.destroy();
        }
        row._rydenAvailFp = null;
    }

    root.querySelectorAll('[data-publish-avail-row]').forEach(initRow);

    var checkInEl = document.getElementById('checkInTime');
    if (checkInEl) {
        checkInEl.addEventListener('change', refreshAllPublishRowsMinDateFromServer);
        checkInEl.addEventListener('input', refreshAllPublishRowsMinDateFromServer);
    }

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
        syncAddBtn();
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
            row.querySelectorAll('.ryden-avail-from, .ryden-avail-until').forEach(function (h) { h.value = ''; });
            var anchor = row.querySelector('.ryden-avail-range-input');
            if (anchor) {
                anchor.value = '';
            }
            initRow(row);
            syncAddBtn();
            return;
        }
        destroyRowFp(row);
        row.remove();
        reindexRows();
        root.querySelectorAll('[data-publish-avail-row]').forEach(function (r) {
            destroyRowFp(r);
        });
        root.querySelectorAll('[data-publish-avail-row]').forEach(initRow);
        syncAddBtn();
    });

    syncAddBtn();
})();

/* Mostrar / ocultar contraseña (botón .ryden-password-toggle dentro de .input-group) */
(function () {
    document.addEventListener("DOMContentLoaded", function () {
        document.querySelectorAll(".ryden-password-toggle").forEach(function (btn) {
            if (btn.getAttribute("data-ryden-bound") === "1") {
                return;
            }
            btn.setAttribute("data-ryden-bound", "1");
            btn.addEventListener("click", function () {
                var group = btn.closest(".input-group");
                if (!group) {
                    return;
                }
                var pwInput = group.querySelector("input");
                if (!pwInput) {
                    return;
                }
                var showLabel = btn.getAttribute("data-label-show") || "Show password";
                var hideLabel = btn.getAttribute("data-label-hide") || "Hide password";
                var icon = btn.querySelector(".bi");
                if (pwInput.type === "password") {
                    pwInput.type = "text";
                    btn.setAttribute("aria-pressed", "true");
                    btn.setAttribute("aria-label", hideLabel);
                    if (icon) {
                        icon.classList.remove("bi-eye");
                        icon.classList.add("bi-eye-slash");
                    }
                } else {
                    pwInput.type = "password";
                    btn.setAttribute("aria-pressed", "false");
                    btn.setAttribute("aria-label", showLabel);
                    if (icon) {
                        icon.classList.remove("bi-eye-slash");
                        icon.classList.add("bi-eye");
                    }
                }
            });
        });
    });
})();

/* Evita que la rueda del mouse cambie el valor de inputs type="number" al scrollear la página. */
(function () {
    function bindNoWheelStep(el) {
        if (el.getAttribute("data-ryden-no-wheel") === "1") {
            return;
        }
        el.setAttribute("data-ryden-no-wheel", "1");
        el.addEventListener(
            "wheel",
            function (e) {
                e.preventDefault();
            },
            { passive: false }
        );
    }

    function init() {
        document.querySelectorAll("input.js-no-number-wheel-step[type='number']").forEach(bindNoWheelStep);
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init);
    } else {
        init();
    }
})();

/* Evita que la rueda del mouse cambie el valor de inputs type="number" al scrollear la página. */
(function () {
    function bindNoWheelStep(el) {
        if (el.getAttribute("data-ryden-no-wheel") === "1") {
            return;
        }
        el.setAttribute("data-ryden-no-wheel", "1");
        el.addEventListener(
            "wheel",
            function (e) {
                e.preventDefault();
            },
            { passive: false }
        );
    }

    function init() {
        document.querySelectorAll("input.js-no-number-wheel-step[type='number']").forEach(bindNoWheelStep);
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init);
    } else {
        init();
    }
})();

