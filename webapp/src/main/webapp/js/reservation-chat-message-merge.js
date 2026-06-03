(function (root, factory) {
    var api = factory();
    if (typeof module !== 'undefined' && module.exports) {
        module.exports = api;
    } else {
        root.ReservationChatMessageMerge = api;
    }
}(typeof globalThis !== 'undefined' ? globalThis : this, function () {
    'use strict';

    /**
     * Returns DTOs from {@code incoming} whose {@code id} is not yet in {@code renderedIds}.
     * Does not mutate {@code renderedIds}; callers mark ids when rendering.
     */
    function mergeIncomingMessages(renderedIds, incoming) {
        if (!incoming || !incoming.length) {
            return [];
        }
        var merged = [];
        for (var i = 0; i < incoming.length; i++) {
            var dto = incoming[i];
            if (!dto || dto.id == null) {
                continue;
            }
            var key = String(dto.id);
            if (renderedIds[key]) {
                continue;
            }
            merged.push(dto);
        }
        return merged;
    }

    return {
        mergeIncomingMessages: mergeIncomingMessages
    };
}));
