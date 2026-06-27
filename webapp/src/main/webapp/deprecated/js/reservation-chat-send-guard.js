(function (root, factory) {
    var api = factory();
    if (typeof module !== 'undefined' && module.exports) {
        module.exports = api;
    } else {
        root.ReservationChatSendGuard = api;
    }
}(typeof globalThis !== 'undefined' ? globalThis : this, function () {
    'use strict';

    function createSendGuard() {
        var locked = false;

        return {
            tryAcquire: function () {
                if (locked) {
                    return false;
                }
                locked = true;
                return true;
            },
            release: function () {
                locked = false;
            },
            isLocked: function () {
                return locked;
            }
        };
    }

    return {
        createSendGuard: createSendGuard
    };
}));
