/* Price market insight card: animated slider + sync with price-per-day input */
(function () {
    var DEFAULT_CURRENCY = "ARS";
    var moneyFormatterCache = {};

    function getMoneyFormatter(currency) {
        var key = currency || DEFAULT_CURRENCY;
        if (!moneyFormatterCache[key]) {
            try {
                moneyFormatterCache[key] = new Intl.NumberFormat(undefined, {
                    style: "currency",
                    currency: key,
                    minimumFractionDigits: 0,
                    maximumFractionDigits: 0
                });
            } catch (ignore) {
                moneyFormatterCache[key] = new Intl.NumberFormat(undefined, {
                    style: "currency",
                    currency: DEFAULT_CURRENCY,
                    minimumFractionDigits: 0,
                    maximumFractionDigits: 0
                });
            }
        }
        return moneyFormatterCache[key];
    }

    function getCardCurrency(card) {
        var attr = card.getAttribute("data-currency");
        return attr && attr.trim() !== "" ? attr.trim() : DEFAULT_CURRENCY;
    }

    var nextCardId = 0;

    function parseMoney(raw) {
        if (raw === null || raw === undefined || String(raw).trim() === "") {
            return NaN;
        }
        var cleaned = String(raw).trim().replace(/\s/g, "");
        if (cleaned.indexOf(",") >= 0 && cleaned.indexOf(".") >= 0) {
            cleaned = cleaned.replace(/\./g, "").replace(",", ".");
        } else {
            cleaned = cleaned.replace(",", ".");
        }
        var n = parseFloat(cleaned);
        return isNaN(n) ? NaN : n;
    }

    function parseAttrNumber(raw) {
        return parseMoney(raw);
    }

    function barRange(min, max) {
        if (isFinite(min) && isFinite(max) && max > min) {
            return { min: min, max: max };
        }
        var center = isFinite(min) ? min : isFinite(max) ? max : 0;
        var pad = Math.max(center * 0.15, 500);
        return { min: Math.max(0, center - pad), max: center + pad };
    }

    function pctOnBar(value, min, max) {
        var range = barRange(min, max);
        var p = ((value - range.min) / (range.max - range.min)) * 100;
        return Math.min(100, Math.max(0, p));
    }

    function valueFromPct(pct, min, max) {
        var range = barRange(min, max);
        var price = range.min + (range.max - range.min) * (pct / 100);
        return clampPrice(price);
    }

    function clampPrice(price) {
        var maxAllowed = 99999999.99;
        var clamped = Math.max(0, Math.min(maxAllowed, price));
        return Math.round(clamped * 100) / 100;
    }

    function formatInputPrice(price) {
        return String(Math.round(clampPrice(price)));
    }

    function getBarZoneLayout(min, max, avg) {
        var avgPct = pctOnBar(avg, min, max);
        var fadeWidth = Math.min(22, Math.max(10, avgPct * 0.2));
        var greenHalf = Math.max(4, fadeWidth * 0.35);
        return {
            avgPct: avgPct,
            fadeWidth: fadeWidth,
            redOuterLeft: avgPct - fadeWidth,
            redOuterRight: avgPct + fadeWidth,
            greenLeft: avgPct - greenHalf,
            greenRight: avgPct + greenHalf,
            minMarkerPct: pctOnBar(min, min, max),
            maxMarkerPct: pctOnBar(max, min, max)
        };
    }

    function zoneForUserPct(userPct, layout) {
        if (userPct <= layout.redOuterLeft || userPct >= layout.redOuterRight) {
            return "red";
        }
        if (userPct >= layout.greenLeft && userPct <= layout.greenRight) {
            return "green";
        }
        return "yellow";
    }

    function applyZone(card, zone) {
        card.classList.remove(
            "ryden-price-insight--zone-red",
            "ryden-price-insight--zone-yellow",
            "ryden-price-insight--zone-green"
        );
        if (zone === "red") {
            card.classList.add("ryden-price-insight--zone-red");
        } else if (zone === "yellow") {
            card.classList.add("ryden-price-insight--zone-yellow");
        } else if (zone === "green") {
            card.classList.add("ryden-price-insight--zone-green");
        }
    }

    function resolvePriceInput(card) {
        if (card._rydenPriceInput && document.body.contains(card._rydenPriceInput)) {
            return card._rydenPriceInput;
        }
        var inCard = card.querySelector("input.js-listing-price-decimal[type='number']");
        if (inCard) {
            card._rydenPriceInput = inCard;
            return inCard;
        }
        var inputId = card.getAttribute("data-price-input-id");
        if (inputId) {
            var byId = document.getElementById(inputId);
            if (byId) {
                card._rydenPriceInput = byId;
                return byId;
            }
        }
        return null;
    }

    function getMarketBounds(card) {
        return {
            min: parseAttrNumber(card.getAttribute("data-min")),
            max: parseAttrNumber(card.getAttribute("data-max")),
            avg: parseAttrNumber(card.getAttribute("data-avg"))
        };
    }

    function getTrack(card) {
        return card.querySelector(".ryden-price-insight__bar-track");
    }

    function pctFromPointer(track, clientX) {
        var rect = track.getBoundingClientRect();
        if (rect.width <= 0) {
            return 0;
        }
        var x = clientX - rect.left;
        return Math.min(100, Math.max(0, (x / rect.width) * 100));
    }

    function setUserMarkerPosition(userMarker, pct, animate) {
        if (!userMarker) {
            return;
        }
        userMarker.classList.toggle("ryden-price-insight__marker--animate", animate !== false);
        userMarker.style.setProperty("left", pct + "%");
        userMarker.setAttribute("data-user-pct", String(pct));
    }

    function syncInputFromPrice(card, price) {
        var input = resolvePriceInput(card);
        if (!input) {
            return;
        }
        var formatted = formatInputPrice(price);
        if (input.value !== formatted) {
            input.value = formatted;
            input.dispatchEvent(new CustomEvent("ryden-listing-price-input", { bubbles: true }));
        }
    }

    function updateBarGradient(card, min, max, avg) {
        var track = getTrack(card);
        if (!track) {
            return null;
        }
        var layout = getBarZoneLayout(min, max, avg);
        track.style.setProperty("--avg-pct", layout.avgPct + "%");
        track.style.setProperty("--fade-width", layout.fadeWidth + "%");
        card._rydenZoneLayout = layout;
        return layout;
    }

    function updateCard(card, options) {
        options = options || {};
        var bounds = getMarketBounds(card);
        var min = bounds.min;
        var max = bounds.max;
        var avg = bounds.avg;
        var input = resolvePriceInput(card);
        var user = options.userPrice;
        if (user === undefined) {
            user = input ? parseMoney(input.value) : parseMoney(card.getAttribute("data-initial-user-price"));
        }

        var layout = updateBarGradient(card, min, max, avg);

        var minMarker = card.querySelector(".ryden-price-insight__marker--min");
        var avgMarker = card.querySelector(".ryden-price-insight__marker--avg");
        var maxMarker = card.querySelector(".ryden-price-insight__marker--max");
        var userMarker = card.querySelector(".ryden-price-insight__marker--user");
        var userLabel = userMarker ? userMarker.querySelector(".ryden-price-insight__marker-label") : null;
        var track = getTrack(card);

        if (layout && minMarker) {
            minMarker.style.left = layout.minMarkerPct + "%";
        }
        if (layout && maxMarker) {
            maxMarker.style.left = layout.maxMarkerPct + "%";
        }
        if (layout && avgMarker) {
            avgMarker.style.left = layout.avgPct + "%";
        }

        if (!userMarker || !track || !layout) {
            return;
        }

        if (isNaN(user) || user <= 0) {
            userMarker.classList.add("d-none");
            userMarker.classList.remove("ryden-price-insight__marker--dragging");
            applyZone(card, null);
            return;
        }

        var userPct = pctOnBar(user, min, max);
        var formattedUser = getMoneyFormatter(getCardCurrency(card)).format(user);
        var animate = options.animate !== false && !card._rydenSliderDragging;

        userMarker.classList.remove("d-none");
        setUserMarkerPosition(userMarker, userPct, animate);
        userMarker.setAttribute("title", formattedUser);
        userMarker.setAttribute("aria-label", formattedUser);
        userMarker.setAttribute("aria-valuenow", String(user));
        userMarker.setAttribute("aria-valuemin", String(barRange(min, max).min));
        userMarker.setAttribute("aria-valuemax", String(barRange(min, max).max));

        if (userLabel) {
            userLabel.textContent = formattedUser;
        }
        applyZone(card, zoneForUserPct(userPct, layout));
    }

    function applyPriceFromSlider(card, clientX, animateInputSync) {
        var track = getTrack(card);
        if (!track) {
            return;
        }
        var bounds = getMarketBounds(card);
        var pct = pctFromPointer(track, clientX);
        var price = valueFromPct(pct, bounds.min, bounds.max);
        updateCard(card, { userPrice: price, animate: false });
        if (animateInputSync !== false) {
            syncInputFromPrice(card, price);
        }
    }

    function endDrag(card) {
        if (!card._rydenSliderDragging) {
            return;
        }
        card._rydenSliderDragging = false;
        var userMarker = card.querySelector(".ryden-price-insight__marker--user");
        if (userMarker) {
            userMarker.classList.remove("ryden-price-insight__marker--dragging");
        }
        document.body.classList.remove("ryden-price-insight--drag-active");
    }

    function applyPriceFromMarketBound(card, price) {
        var bounds = getMarketBounds(card);
        var range = barRange(bounds.min, bounds.max);
        var clamped = clampPrice(Math.max(range.min, Math.min(range.max, price)));
        updateCard(card, { userPrice: clamped, animate: true });
        syncInputFromPrice(card, clamped);
    }

    function bindMinMaxMarkers(card) {
        if (card.getAttribute("data-ryden-bounds-bound") === "1") {
            return;
        }
        card.setAttribute("data-ryden-bounds-bound", "1");

        var minMarker = card.querySelector(".ryden-price-insight__marker--min");
        var maxMarker = card.querySelector(".ryden-price-insight__marker--max");
        if (minMarker) {
            minMarker.addEventListener("click", function (e) {
                e.stopPropagation();
                applyPriceFromMarketBound(card, getMarketBounds(card).min);
            });
            minMarker.addEventListener("keydown", function (e) {
                if (e.key === "Enter" || e.key === " ") {
                    e.preventDefault();
                    applyPriceFromMarketBound(card, getMarketBounds(card).min);
                }
            });
        }
        if (maxMarker) {
            maxMarker.addEventListener("click", function (e) {
                e.stopPropagation();
                applyPriceFromMarketBound(card, getMarketBounds(card).max);
            });
            maxMarker.addEventListener("keydown", function (e) {
                if (e.key === "Enter" || e.key === " ") {
                    e.preventDefault();
                    applyPriceFromMarketBound(card, getMarketBounds(card).max);
                }
            });
        }
    }

    function bindSlider(card) {
        if (card.getAttribute("data-ryden-slider-bound") === "1") {
            return;
        }
        card.setAttribute("data-ryden-slider-bound", "1");

        var track = getTrack(card);
        var userMarker = card.querySelector(".ryden-price-insight__marker--user");
        var hit = card.querySelector(".ryden-price-insight__bar-hit");
        if (!track || !userMarker) {
            return;
        }

        function startDrag(clientX, target) {
            card._rydenSliderDragging = true;
            userMarker.classList.add("ryden-price-insight__marker--dragging");
            document.body.classList.add("ryden-price-insight--drag-active");
            applyPriceFromSlider(card, clientX, true);

            function onMove(e) {
                if (!card._rydenSliderDragging) {
                    return;
                }
                e.preventDefault();
                applyPriceFromSlider(card, e.clientX, true);
            }

            function onEnd() {
                endDrag(card);
                window.removeEventListener("pointermove", onMove);
                window.removeEventListener("pointerup", onEnd);
                window.removeEventListener("pointercancel", onEnd);
                if (target && typeof target.releasePointerCapture === "function") {
                    try {
                        target.releasePointerCapture(card._rydenActivePointerId);
                    } catch (ignore) {
                        /* already released */
                    }
                }
            }

            window.addEventListener("pointermove", onMove);
            window.addEventListener("pointerup", onEnd);
            window.addEventListener("pointercancel", onEnd);
        }

        function onPointerDown(e) {
            if (e.button !== undefined && e.button !== 0) {
                return;
            }
            card._rydenActivePointerId = e.pointerId;
            if (typeof e.target.setPointerCapture === "function") {
                try {
                    e.target.setPointerCapture(e.pointerId);
                } catch (ignore) {
                    /* ignore */
                }
            }
            e.preventDefault();
            startDrag(e.clientX, e.target);
        }

        userMarker.addEventListener("pointerdown", onPointerDown);
        if (hit) {
            hit.addEventListener("pointerdown", onPointerDown);
        }
    }

    function scheduleUpdate(card, animate) {
        requestAnimationFrame(function () {
            updateCard(card, { animate: animate !== false });
        });
    }

    function bindCard(card) {
        if (!card.getAttribute("data-insight-id")) {
            nextCardId += 1;
            card.setAttribute("data-insight-id", "ryden-pi-" + nextCardId);
        }
        bindSlider(card);
        bindMinMaxMarkers(card);
        var input = resolvePriceInput(card);
        if (input) {
            input.setAttribute("data-ryden-price-insight-target", card.getAttribute("data-insight-id"));
            if (input.getAttribute("data-ryden-price-insight-bound") !== "1") {
                input.setAttribute("data-ryden-price-insight-bound", "1");
                input.addEventListener("input", function () {
                    if (!card._rydenSliderDragging) {
                        scheduleUpdate(card, true);
                    }
                });
                input.addEventListener("change", function () {
                    scheduleUpdate(card, true);
                });
            }
        }
        scheduleUpdate(card, false);
    }

    function findCardForInput(input) {
        var cardId = input.getAttribute("data-ryden-price-insight-target");
        if (cardId) {
            return document.querySelector('[data-ryden-price-insight][data-insight-id="' + cardId + '"]');
        }
        return input.closest("[data-ryden-price-insight]");
    }

    function init() {
        document.querySelectorAll("[data-ryden-price-insight]").forEach(bindCard);
    }

    document.addEventListener(
        "ryden-listing-price-input",
        function (e) {
            var input = e.target;
            if (!(input instanceof HTMLInputElement) || input.closest("[data-ryden-price-insight]") === null) {
                return;
            }
            var card = findCardForInput(input);
            if (card && !card._rydenSliderDragging) {
                scheduleUpdate(card, true);
            }
        },
        false
    );

    document.addEventListener(
        "input",
        function (e) {
            var input = e.target;
            if (!(input instanceof HTMLInputElement) || !input.classList.contains("js-listing-price-decimal")) {
                return;
            }
            var card = findCardForInput(input);
            if (card && !card._rydenSliderDragging) {
                scheduleUpdate(card, true);
            }
        },
        false
    );

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init);
    } else {
        init();
    }
})();
