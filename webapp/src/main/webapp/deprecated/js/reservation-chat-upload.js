(function (root, factory) {
    var api = factory();
    if (typeof module !== 'undefined' && module.exports) {
        module.exports = api;
    } else {
        root.ReservationChatUpload = api;
    }
}(typeof globalThis !== 'undefined' ? globalThis : this, function () {
    'use strict';

    var MIN_UPLOAD_TIMEOUT_MS = 120000;
    var MS_PER_MEGABYTE = 5000;

    function computeUploadTimeoutMs(maxAttachmentMb) {
        var mb = maxAttachmentMb != null && isFinite(maxAttachmentMb) ? maxAttachmentMb : 25;
        return Math.max(MIN_UPLOAD_TIMEOUT_MS, mb * MS_PER_MEGABYTE);
    }

    return {
        MIN_UPLOAD_TIMEOUT_MS: MIN_UPLOAD_TIMEOUT_MS,
        MS_PER_MEGABYTE: MS_PER_MEGABYTE,
        computeUploadTimeoutMs: computeUploadTimeoutMs
    };
}));
