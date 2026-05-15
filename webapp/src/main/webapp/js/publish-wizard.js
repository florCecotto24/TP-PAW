/* publish-wizard.js — multi-step publish form */
(function () {
    document.addEventListener('DOMContentLoaded', function () {
        var steps = Array.from(document.querySelectorAll('.publish-wizard-step'));
        if (!steps.length) return;

        var totalSteps   = steps.length;
        var currentStep  = 1;
        var progressFill = document.getElementById('wizardProgressFill');
        var stepTitle    = document.getElementById('wizardStepTitle');
        var stepCounter  = document.getElementById('wizardStepCounter');
        var labelEls     = Array.from(document.querySelectorAll('.wizard-label[data-step]'));

        function showStep(n) {
            currentStep = n;
            steps.forEach(function (step, i) {
                step.style.display = (i + 1 === n) ? '' : 'none';
            });

            var pct = Math.round((n / totalSteps) * 100);
            if (progressFill) progressFill.style.width = pct + '%';
            if (stepCounter)  stepCounter.textContent  = n + ' / ' + totalSteps;

            var activeLbl = labelEls.find(function (el) {
                return parseInt(el.dataset.step, 10) === n;
            });
            if (stepTitle && activeLbl) stepTitle.textContent = activeLbl.dataset.title || '';

            labelEls.forEach(function (lbl) {
                var s = parseInt(lbl.dataset.step, 10);
                lbl.classList.toggle('wizard-label--active', s === n);
                lbl.classList.toggle('wizard-label--done',   s < n);
            });

            var bar = document.getElementById('publishWizardProgress');
            if (bar) bar.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
        }

        function validateStep(n) {
            var stepEl = steps[n - 1];
            var valid = true;

            /* Standard required inputs and selects */
            stepEl.querySelectorAll('input[required], select[required]').forEach(function (field) {
                var empty = !field.value || !field.value.trim();
                field.classList.toggle('is-invalid', empty);
                if (empty) valid = false;
            });

            /* Step 2: neighborhood — uses the picker's built-in error element */
            if (n === 2) {
                var nbHid   = document.getElementById('nb_hid_publish');
                var nbErrEl = document.getElementById('nb_err_publish');
                var nbDdBtn = document.getElementById('nb_dd_btn_publish');
                var noNb    = !nbHid || !nbHid.value;
                if (noNb) {
                    if (nbErrEl) {
                        var form = document.getElementById('publishCarFormEl');
                        nbErrEl.textContent = (form && form.getAttribute('data-ryden-nb-required')) || '';
                        nbErrEl.classList.remove('d-none');
                    }
                    if (nbDdBtn) nbDdBtn.classList.add('is-invalid');
                    valid = false;
                } else {
                    if (nbErrEl) nbErrEl.classList.add('d-none');
                    if (nbDdBtn) nbDdBtn.classList.remove('is-invalid');
                }
            }

            /* Step 3: at least one availability row with both dates filled */
            if (n === 3) {
                var availSection = document.getElementById('publishAvailabilitySection');
                var rows = stepEl.querySelectorAll('[data-publish-avail-row]');
                var hasValidRow = false;
                rows.forEach(function (row) {
                    var from  = row.querySelector('.ryden-avail-from');
                    var until = row.querySelector('.ryden-avail-until');
                    if (from && from.value && until && until.value) hasValidRow = true;
                });
                var availErrEl = document.getElementById('publishClientAvailError');
                if (!hasValidRow) {
                    if (availErrEl) {
                        availErrEl.textContent = (availSection && availSection.getAttribute('data-publish-avail-required')) || '';
                        availErrEl.classList.remove('d-none');
                    }
                    valid = false;
                } else if (availErrEl) {
                    availErrEl.classList.add('d-none');
                }
            }

            /* Step 4: at least one picture (new upload, retained, or already previewed) */
            if (n === 4) {
                var picturesInput = document.getElementById('picturesInput');
                var retained  = document.querySelectorAll('[data-retained-picture-col]').length;
                var preview   = document.getElementById('picturesPreview');
                var previewed = preview ? preview.children.length : 0;
                var hasPics   = retained > 0 || previewed > 0
                             || (picturesInput && picturesInput.files && picturesInput.files.length > 0);
                var picErrEl  = document.getElementById('publishPicturesClientError');
                if (!hasPics) {
                    if (picErrEl) {
                        picErrEl.textContent = (picturesInput && picturesInput.getAttribute('data-publish-pictures-required')) || '';
                        picErrEl.classList.remove('d-none');
                    }
                    valid = false;
                } else if (picErrEl) {
                    picErrEl.classList.add('d-none');
                }
            }

            /* Scroll to first visible error */
            if (!valid) {
                var firstBad = stepEl.querySelector('.is-invalid')
                    || stepEl.querySelector('.text-danger:not(.d-none)');
                if (firstBad) firstBad.scrollIntoView({ behavior: 'smooth', block: 'center' });
            }

            return valid;
        }

        document.querySelectorAll('[data-wizard-next]').forEach(function (btn) {
            btn.addEventListener('click', function () {
                if (currentStep < totalSteps && validateStep(currentStep)) {
                    showStep(currentStep + 1);
                }
            });
        });

        document.querySelectorAll('[data-wizard-prev]').forEach(function (btn) {
            btn.addEventListener('click', function () {
                if (currentStep > 1) showStep(currentStep - 1);
            });
        });

        /* On reload after server validation, navigate to the first step with errors */
        var firstErrorStep = null;
        steps.forEach(function (step, i) {
            if (firstErrorStep !== null) return;
            var hasInvalid = step.querySelector('.is-invalid');
            var hasMsg = Array.from(step.querySelectorAll('.text-danger, .alert-danger')).some(function (el) {
                return el.textContent.trim().length > 0;
            });
            if (hasInvalid || hasMsg) firstErrorStep = i + 1;
        });

        showStep(firstErrorStep || 1);
    });
})();
