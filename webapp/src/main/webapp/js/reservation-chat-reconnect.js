(function (root, factory) {
    var api = factory();
    if (typeof module !== 'undefined' && module.exports) {
        module.exports = api;
    } else {
        root.ReservationChatReconnect = api;
    }
}(typeof globalThis !== 'undefined' ? globalThis : this, function () {
    'use strict';

    /**
     * Exponential backoff reconnect scheduler without a terminal failure state.
     * {@code onReconnect} must return a Promise; rejection schedules another attempt.
     */
    function createReconnectScheduler(options) {
        options = options || {};
        var onReconnect = options.onReconnect;
        var maxDelayMs = options.maxDelayMs != null ? options.maxDelayMs : 30000;
        var baseDelayMs = options.baseDelayMs != null ? options.baseDelayMs : 1000;
        var setTimer = options.setTimer || setTimeout;
        var clearTimer = options.clearTimer || clearTimeout;

        var attemptCount = 0;
        var timerId = null;
        var suspended = false;

        function computeDelay() {
            return Math.min(maxDelayMs, baseDelayMs * Math.pow(2, attemptCount));
        }

        function clearPendingTimer() {
            if (timerId != null) {
                clearTimer(timerId);
                timerId = null;
            }
        }

        function schedule() {
            if (suspended || timerId != null || typeof onReconnect !== 'function') {
                return;
            }
            var delay = computeDelay();
            attemptCount++;
            timerId = setTimer(function () {
                timerId = null;
                var result;
                try {
                    result = onReconnect();
                } catch (e) {
                    schedule();
                    return;
                }
                if (result && typeof result.then === 'function') {
                    result.then(
                        function () {
                            attemptCount = 0;
                        },
                        function () {
                            schedule();
                        }
                    );
                }
            }, delay);
        }

        return {
            schedule: schedule,
            suspend: function () {
                suspended = true;
                clearPendingTimer();
            },
            resume: function () {
                suspended = false;
                schedule();
            },
            reset: function () {
                attemptCount = 0;
                clearPendingTimer();
            },
            getAttemptCount: function () {
                return attemptCount;
            },
            isSuspended: function () {
                return suspended;
            },
            hasPendingTimer: function () {
                return timerId != null;
            },
            getNextDelayMs: function () {
                return computeDelay();
            }
        };
    }

    return {
        createReconnectScheduler: createReconnectScheduler
    };
}));
