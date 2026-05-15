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

        document.querySelectorAll('[data-wizard-next]').forEach(function (btn) {
            btn.addEventListener('click', function () {
                if (currentStep < totalSteps) showStep(currentStep + 1);
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
