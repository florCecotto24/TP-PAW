(function () {
    'use strict';

    function initCounterpartyListingsLoadMore() {
        var btn = document.getElementById('counterpartyListingsLoadMoreBtn');
        if (!btn) {
            return;
        }
        var row = document.getElementById('counterpartyActiveListingsRow');
        var section = document.querySelector('[data-counterparty-listings-endpoint]');
        if (!row || !section) {
            return;
        }
        var endpoint = section.getAttribute('data-counterparty-listings-endpoint');
        var errEl = document.getElementById('counterpartyListingsLoadErr');
        var loadingEl = document.getElementById('counterpartyListingsLoadLoadingText');
        var defaultBtnLabel =
                btn.getAttribute('data-default-label') != null && btn.getAttribute('data-default-label') !== ''
                        ? btn.getAttribute('data-default-label')
                        : btn.textContent.trim();

        btn.addEventListener('click', function () {
            var ownerId = btn.getAttribute('data-owner-user-id');
            var nextPage = btn.getAttribute('data-next-page');
            var excludeRaw = btn.getAttribute('data-exclude-car-id');
            var url =
                    endpoint +
                    '?userId=' +
                    encodeURIComponent(ownerId) +
                    '&page=' +
                    encodeURIComponent(nextPage);
            if (excludeRaw != null && excludeRaw !== '') {
                url += '&carId=' + encodeURIComponent(excludeRaw);
            }

            btn.disabled = true;
            if (loadingEl && loadingEl.textContent.trim() !== '') {
                btn.textContent = loadingEl.textContent.trim();
            }

            fetch(url, { credentials: 'same-origin', headers: { Accept: 'text/html' } })
                    .then(function (res) {
                        if (!res.ok) {
                            throw new Error('request failed');
                        }
                        return res.text();
                    })
                    .then(function (html) {
                        row.insertAdjacentHTML('beforeend', html);
                        var metas = row.querySelectorAll('.js-counterparty-listings-chunk-meta');
                        var meta = metas.length ? metas[metas.length - 1] : null;
                        if (meta && meta.getAttribute('data-has-more') === 'true') {
                            var np = meta.getAttribute('data-next-page');
                            if (np != null && np !== '') {
                                btn.setAttribute('data-next-page', np);
                            }
                        } else {
                            btn.classList.add('d-none');
                        }
                        if (meta) {
                            meta.parentNode.removeChild(meta);
                        }
                    })
                    .catch(function () {
                        if (errEl && errEl.textContent.trim() !== '') {
                            window.alert(errEl.textContent.trim());
                        }
                    })
                    .finally(function () {
                        btn.disabled = false;
                        btn.textContent = defaultBtnLabel;
                    });
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initCounterpartyListingsLoadMore);
    } else {
        initCounterpartyListingsLoadMore();
    }
})();
