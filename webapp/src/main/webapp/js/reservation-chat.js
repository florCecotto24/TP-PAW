(function () {
    'use strict';

    var wallTime = window.ReservationChatWallTime;
    var sendGuardApi = window.ReservationChatSendGuard;
    var messageMerge = window.ReservationChatMessageMerge;
    var uploadHelpers = window.ReservationChatUpload;

    if (!wallTime || !sendGuardApi || !messageMerge || !uploadHelpers) {
        return;
    }

    var POLL_INTERVAL_MS = 5000;
    var UPLOAD_MAX_RETRIES = 1;

    var ALLOWED_EXTENSIONS = {
        jpg: true,
        jpeg: true,
        png: true,
        gif: true,
        webp: true,
        bmp: true,
        svg: true,
        pdf: true,
        doc: true,
        docx: true,
        mp4: true,
        webm: true,
        mov: true,
        txt: true,
        zip: true,
        xls: true,
        xlsx: true,
        ppt: true,
        pptx: true
    };

    var root = document.getElementById('reservationChatRoot');
    if (!root) {
        return;
    }

    var contextPath = root.getAttribute('data-context-path') || '';
    var reservationId = root.getAttribute('data-reservation-id');
    var viewerUserId = Number(root.getAttribute('data-viewer-user-id'));
    var maxLength = Number(root.getAttribute('data-max-length')) || 1000;
    var maxAttachmentMb = Number(root.getAttribute('data-max-attachment-mb')) || 25;
    var maxAttachmentBytes = maxAttachmentMb * 1024 * 1024;
    var emptyLabel = root.getAttribute('data-empty-label') || '';
    var labelToday = root.getAttribute('data-label-today') || 'Today';
    var labelYesterday = root.getAttribute('data-label-yesterday') || 'Yesterday';
    var errorLoadLabel = root.getAttribute('data-error-load') || 'Could not load chat. Try again later.';
    var errorSendLabel = root.getAttribute('data-error-send') || 'Could not send the message. Try again.';
    var errorUploadTimeoutLabel =
        root.getAttribute('data-error-upload-timeout') || 'Upload timed out. Try again.';
    var labelUploading = root.getAttribute('data-uploading-label') || 'Uploading…';
    var labelTooLarge = root.getAttribute('data-too-large-label') || 'File must be at most {0} MB.';
    var labelInvalidType = root.getAttribute('data-invalid-type-label') || 'This file type is not allowed.';
    var labelCancel = root.getAttribute('data-cancel-label') || 'Cancel';
    var historyPageSize = Number(root.getAttribute('data-history-page-size')) || 50;
    var labelLoadOlder = root.getAttribute('data-label-load-older') || 'Load older messages';
    var labelLoadNewer = root.getAttribute('data-label-load-newer') || 'Load newer messages';

    var messagesEl = document.getElementById('reservationChatMessages');
    var dayBarEl = document.getElementById('reservationChatDayBar');
    var dayLabelEl = document.getElementById('reservationChatDayLabel');
    var inputEl = document.getElementById('reservationChatInput');
    var sendBtn = document.getElementById('reservationChatSend');
    var attachBtn = document.getElementById('reservationChatAttach');
    var fileInputEl = document.getElementById('reservationChatFileInput');
    var errorEl = document.getElementById('reservationChatError');
    var dropZoneEl = document.getElementById('reservationChatDropZone');
    var dropOverlayEl = document.getElementById('reservationChatDropOverlay');
    var pendingEl = document.getElementById('reservationChatPending');
    var uploadProgressEl = document.getElementById('reservationChatUploadProgress');
    var uploadProgressBarEl = document.getElementById('reservationChatUploadProgressBar');
    var loadOlderBtn = document.getElementById('reservationChatLoadOlder');
    var loadNewerBtn = document.getElementById('reservationChatLoadNewer');

    var historyLoaded = false;
    var historyLoading = false;
    var historyPageIndex = 0;
    var historyTotalPages = 1;
    var historyHasPrevious = false;
    var historyHasNext = false;
    var dayGroups = Object.create(null);
    var daySeparators = Object.create(null);
    var dayObserver = null;
    var scrollFadeTimer = null;
    var lastStickyDayKey = null;
    var pendingFile = null;
    var uploading = false;
    var renderedMessageIds = Object.create(null);
    var pollTimer = null;
    var sendGuard = sendGuardApi.createSendGuard();

    function maxRenderedMessageId() {
        var maxId = 0;
        Object.keys(renderedMessageIds).forEach(function (key) {
            var id = Number(key);
            if (!isNaN(id) && id > maxId) {
                maxId = id;
            }
        });
        return maxId;
    }

    function wallTimeOptions() {
        return {
            labelToday: labelToday,
            labelYesterday: labelYesterday,
            intlLocale: intlLocale()
        };
    }

    function showError(msg) {
        if (!errorEl) {
            return;
        }
        if (!msg) {
            errorEl.textContent = '';
            errorEl.classList.add('d-none');
            return;
        }
        errorEl.textContent = msg;
        errorEl.classList.remove('d-none');
    }

    function setComposerEnabled(enabled) {
        if (inputEl) {
            inputEl.disabled = !enabled;
        }
        if (sendBtn) {
            sendBtn.disabled = !enabled;
        }
        if (attachBtn) {
            attachBtn.disabled = !enabled;
        }
    }

    function formatTemplate(template, arg0) {
        if (!template) {
            return '';
        }
        return String(template).replace('{0}', String(arg0));
    }

    function fileExtension(name) {
        if (!name) {
            return '';
        }
        var lower = String(name).toLowerCase();
        var dot = lower.lastIndexOf('.');
        return dot >= 0 ? lower.substring(dot + 1) : '';
    }

    function isAllowedFile(file) {
        if (!file) {
            return false;
        }
        var ext = fileExtension(file.name);
        if (ext && ALLOWED_EXTENSIONS[ext]) {
            return true;
        }
        var type = (file.type || '').toLowerCase();
        if (type.indexOf('image/') === 0) {
            return true;
        }
        if (type === 'application/pdf') {
            return true;
        }
        if (
            type === 'application/msword' ||
            type === 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
        ) {
            return true;
        }
        if (type === 'video/mp4' || type === 'video/webm' || type === 'video/quicktime') {
            return true;
        }
        if (
            type === 'text/plain' ||
            type === 'application/zip' ||
            type === 'application/x-zip-compressed'
        ) {
            return true;
        }
        return false;
    }

    function validateFileClient(file) {
        if (!file) {
            return labelInvalidType;
        }
        if (file.size > maxAttachmentBytes) {
            return formatTemplate(labelTooLarge, maxAttachmentMb);
        }
        if (!isAllowedFile(file)) {
            return labelInvalidType;
        }
        return null;
    }

    function formatFileSize(bytes) {
        if (bytes == null || !isFinite(bytes) || bytes < 0) {
            return '';
        }
        if (bytes < 1024) {
            return bytes + ' B';
        }
        if (bytes < 1024 * 1024) {
            return (bytes / 1024).toFixed(1) + ' KB';
        }
        return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
    }

    function attachmentFullUrl(relativeUrl) {
        if (!relativeUrl) {
            return '';
        }
        if (relativeUrl.indexOf('http://') === 0 || relativeUrl.indexOf('https://') === 0) {
            return relativeUrl;
        }
        return contextPath + relativeUrl;
    }

    function attachmentDownloadUrl(relativeUrl) {
        var full = attachmentFullUrl(relativeUrl);
        if (!full) {
            return '';
        }
        if (full.indexOf('/attachment/download') >= 0) {
            return full;
        }
        if (full.slice(-'/attachment'.length) === '/attachment') {
            return full + '/download';
        }
        return full;
    }

    function attachmentViewUrl(relativeUrl) {
        var download = attachmentDownloadUrl(relativeUrl);
        if (!download) {
            return '';
        }
        return download.replace(/\/attachment\/download$/, '/attachment/view');
    }

    function setUploadProgress(percent) {
        if (!uploadProgressEl || !uploadProgressBarEl) {
            return;
        }
        if (percent == null || percent < 0) {
            uploadProgressEl.classList.add('d-none');
            uploadProgressBarEl.style.width = '0%';
            uploadProgressEl.setAttribute('aria-valuenow', '0');
            return;
        }
        uploadProgressEl.classList.remove('d-none');
        var p = Math.min(100, Math.max(0, percent));
        uploadProgressBarEl.style.width = p + '%';
        uploadProgressEl.setAttribute('aria-valuenow', String(Math.round(p)));
    }

    function clearPendingFile() {
        pendingFile = null;
        if (fileInputEl) {
            fileInputEl.value = '';
        }
        if (pendingEl) {
            pendingEl.classList.add('d-none');
            pendingEl.textContent = '';
        }
    }

    function setPendingFile(file) {
        var err = validateFileClient(file);
        if (err) {
            showError(err);
            return;
        }
        showError('');
        pendingFile = file;
        if (!pendingEl) {
            return;
        }
        pendingEl.classList.remove('d-none');
        pendingEl.innerHTML = '';
        var nameSpan = document.createElement('span');
        nameSpan.className = 'reservation-chat__pending-name';
        nameSpan.textContent = file.name + ' (' + formatFileSize(file.size) + ')';
        pendingEl.appendChild(nameSpan);
        var cancelBtn = document.createElement('button');
        cancelBtn.type = 'button';
        cancelBtn.className = 'btn btn-sm btn-outline-secondary';
        cancelBtn.textContent = labelCancel;
        cancelBtn.addEventListener('click', clearPendingFile);
        pendingEl.appendChild(cancelBtn);
    }

    function showDropOverlay(visible) {
        if (!dropOverlayEl) {
            return;
        }
        if (visible) {
            dropOverlayEl.classList.remove('d-none');
            dropOverlayEl.setAttribute('aria-hidden', 'false');
        } else {
            dropOverlayEl.classList.add('d-none');
            dropOverlayEl.setAttribute('aria-hidden', 'true');
        }
    }

    function intlLocale() {
        var lang = document.documentElement.lang;
        return lang && lang.trim() ? lang.trim() : undefined;
    }

    function wallDayKey(value) {
        return wallTime.wallDayKey(value);
    }

    function wallTodayKey() {
        return wallTime.wallTodayKey();
    }

    function formatDayLabel(dayKey) {
        return wallTime.formatDayLabel(dayKey, wallTimeOptions());
    }

    function formatMessageTime(value) {
        return wallTime.formatMessageTime(value, { intlLocale: intlLocale() });
    }

    function setDayBarVisible(visible) {
        if (!dayBarEl) {
            return;
        }
        if (visible) {
            dayBarEl.classList.remove('d-none');
        } else {
            dayBarEl.classList.add('d-none');
            hideFloatingDayBar();
        }
    }

    function setStickyDayLabel(dayKey) {
        if (!dayLabelEl || !dayKey) {
            return;
        }
        if (dayKey === lastStickyDayKey) {
            return;
        }
        lastStickyDayKey = dayKey;
        dayLabelEl.textContent = formatDayLabel(dayKey);
    }

    function shouldShowFloatingDayBar() {
        if (!lastStickyDayKey || !daySeparators[lastStickyDayKey]) {
            return false;
        }
        var containerTop = messagesEl.getBoundingClientRect().top;
        return daySeparators[lastStickyDayKey].getBoundingClientRect().top < containerTop + 4;
    }

    function hideFloatingDayBar() {
        if (dayBarEl) {
            dayBarEl.classList.remove('reservation-chat-day-bar--scrolling');
        }
    }

    function showDayBarWhileScrolling() {
        if (!dayBarEl) {
            return;
        }
        if (!shouldShowFloatingDayBar()) {
            hideFloatingDayBar();
            return;
        }
        dayBarEl.classList.add('reservation-chat-day-bar--scrolling');
        if (scrollFadeTimer) {
            clearTimeout(scrollFadeTimer);
        }
        scrollFadeTimer = setTimeout(function () {
            hideFloatingDayBar();
        }, 1000);
    }

    function updateStickyFromScroll() {
        var separators = messagesEl.querySelectorAll('.reservation-chat__day-separator');
        if (!separators.length) {
            return;
        }
        var containerTop = messagesEl.getBoundingClientRect().top;
        var activeKey = null;
        for (var i = 0; i < separators.length; i++) {
            var sep = separators[i];
            var rect = sep.getBoundingClientRect();
            if (rect.top <= containerTop + 8) {
                activeKey = sep.getAttribute('data-day-key');
            } else if (!activeKey) {
                activeKey = sep.getAttribute('data-day-key');
                break;
            }
        }
        if (!activeKey) {
            activeKey = separators[0].getAttribute('data-day-key');
        }
        setStickyDayLabel(activeKey);
    }

    function observeDaySeparators() {
        if (dayObserver) {
            dayObserver.disconnect();
            dayObserver = null;
        }
        var separators = messagesEl.querySelectorAll('.reservation-chat__day-separator');
        if (!separators.length) {
            return;
        }
        if (typeof IntersectionObserver === 'undefined') {
            updateStickyFromScroll();
            return;
        }
        dayObserver = new IntersectionObserver(
            function (entries) {
                var containerTop = messagesEl.getBoundingClientRect().top;
                var bestKey = null;
                var bestTop = Infinity;
                entries.forEach(function (entry) {
                    if (!entry.isIntersecting) {
                        return;
                    }
                    var key = entry.target.getAttribute('data-day-key');
                    var top = entry.boundingClientRect.top;
                    if (top <= containerTop + 12 && top < bestTop) {
                        bestTop = top;
                        bestKey = key;
                    }
                });
                if (bestKey) {
                    setStickyDayLabel(bestKey);
                    showDayBarWhileScrolling();
                } else {
                    updateStickyFromScroll();
                }
            },
            {
                root: messagesEl,
                rootMargin: '-4px 0px -70% 0px',
                threshold: [0, 0.25, 0.5, 1]
            }
        );
        separators.forEach(function (sep) {
            dayObserver.observe(sep);
        });
    }

    function syncStickyToLastDay() {
        var separators = messagesEl.querySelectorAll('.reservation-chat__day-separator');
        if (separators.length) {
            setStickyDayLabel(separators[separators.length - 1].getAttribute('data-day-key'));
        }
    }

    function resetDayState() {
        dayGroups = Object.create(null);
        daySeparators = Object.create(null);
        lastStickyDayKey = null;
        if (dayObserver) {
            dayObserver.disconnect();
            dayObserver = null;
        }
    }

    function isDayBarNode(node) {
        return node && (node.id === 'reservationChatDayBar' || node.id === 'reservationChatDropOverlay');
    }

    function clearMessageContent() {
        resetDayState();
        var child = messagesEl.firstChild;
        while (child) {
            var next = child.nextSibling;
            if (!isDayBarNode(child)) {
                messagesEl.removeChild(child);
            }
            child = next;
        }
        setDayBarVisible(false);
    }

    function removeEmptyPlaceholder() {
        var empty = messagesEl.querySelector('.reservation-chat__empty');
        if (empty) {
            empty.remove();
        }
        setDayBarVisible(true);
    }

    function renderDaySeparator(dayKey) {
        var separator = document.createElement('div');
        separator.className = 'reservation-chat__day-separator';
        separator.setAttribute('data-day-key', dayKey);
        var pill = document.createElement('span');
        pill.className = 'reservation-chat__day-pill reservation-chat__day-separator-pill';
        pill.textContent = formatDayLabel(dayKey);
        separator.appendChild(pill);
        messagesEl.appendChild(separator);
        daySeparators[dayKey] = separator;
        return separator;
    }

    function getOrCreateDayGroup(dayKey) {
        if (dayGroups[dayKey]) {
            return dayGroups[dayKey];
        }
        renderDaySeparator(dayKey);
        var group = document.createElement('div');
        group.className = 'reservation-chat__day-group';
        group.setAttribute('data-day-key', dayKey);
        messagesEl.appendChild(group);
        dayGroups[dayKey] = group;
        return group;
    }

    function appendFileCard(bubble, att, iconClass, iconExtraClass) {
        var card = document.createElement('a');
        card.className = 'reservation-chat__file-card';
        card.href = attachmentViewUrl(att.url);
        card.target = '_blank';
        card.rel = 'noopener noreferrer';
        var icon = document.createElement('i');
        icon.className =
            'reservation-chat__file-card-icon bi ' + iconClass + (iconExtraClass ? ' ' + iconExtraClass : '');
        icon.setAttribute('aria-hidden', 'true');
        card.appendChild(icon);
        var body = document.createElement('span');
        body.className = 'reservation-chat__file-card-body';
        var nameEl = document.createElement('span');
        nameEl.className = 'reservation-chat__file-card-name';
        nameEl.textContent = att.fileName || '';
        var metaEl = document.createElement('span');
        metaEl.className = 'reservation-chat__file-card-meta';
        metaEl.textContent = formatFileSize(att.sizeBytes);
        body.appendChild(nameEl);
        body.appendChild(metaEl);
        card.appendChild(body);
        bubble.appendChild(card);
    }

    function appendVisualMedia(bubble, mediaEl) {
        var wrap = document.createElement('div');
        wrap.className = 'reservation-chat__attachment-media';
        wrap.appendChild(mediaEl);
        bubble.appendChild(wrap);
    }

    function renderAttachment(bubble, att) {
        if (!att) {
            return;
        }
        var kind = (att.kind || 'GENERIC').toUpperCase();
        var downloadUrl = attachmentDownloadUrl(att.url);
        var viewUrl = attachmentViewUrl(att.url);
        if (kind === 'IMAGE') {
            var img = document.createElement('img');
            img.className = 'reservation-chat__attachment-image';
            img.src = downloadUrl;
            img.alt = att.fileName || '';
            img.loading = 'lazy';
            img.addEventListener('click', function () {
                window.open(viewUrl, '_blank', 'noopener,noreferrer');
            });
            appendVisualMedia(bubble, img);
            return;
        }
        if (kind === 'VIDEO') {
            var video = document.createElement('video');
            video.className = 'reservation-chat__attachment-video';
            video.controls = true;
            video.preload = 'metadata';
            video.src = downloadUrl;
            appendVisualMedia(bubble, video);
            return;
        }
        if (kind === 'PDF') {
            appendFileCard(bubble, att, 'bi-file-pdf', 'reservation-chat__file-card-icon--pdf');
            return;
        }
        if (kind === 'DOCUMENT') {
            appendFileCard(bubble, att, 'bi-file-word', 'reservation-chat__file-card-icon--document');
            return;
        }
        appendFileCard(bubble, att, 'bi-file-earmark');
    }

    function buildMessageMeta(dto, isMine) {
        var meta = document.createElement('span');
        meta.className = 'reservation-chat__meta';
        var time = document.createElement('span');
        time.className = 'reservation-chat__time';
        time.textContent = formatMessageTime(dto.createdAt);
        meta.appendChild(time);
        if (isMine) {
            var receipt = document.createElement('span');
            receipt.className = 'reservation-chat__receipt';
            receipt.setAttribute('aria-hidden', 'true');
            receipt.textContent = '\u2713\u2713';
            if (dto.seen === true) {
                receipt.classList.add('reservation-chat__receipt--seen');
            }
            meta.appendChild(receipt);
        }
        return meta;
    }

    function syncReadReceipts(messages) {
        if (!messages || !messages.length || !messagesEl) {
            return;
        }
        for (var i = 0; i < messages.length; i++) {
            var dto = messages[i];
            if (!dto || dto.id == null || Number(dto.senderUserId) !== viewerUserId) {
                continue;
            }
            var item = messagesEl.querySelector('[data-message-id="' + String(dto.id) + '"]');
            if (!item) {
                continue;
            }
            var receipt = item.querySelector('.reservation-chat__receipt');
            if (!receipt) {
                continue;
            }
            if (dto.seen === true) {
                receipt.classList.add('reservation-chat__receipt--seen');
            } else {
                receipt.classList.remove('reservation-chat__receipt--seen');
            }
        }
    }

    function refreshReadReceiptsOnCurrentPage() {
        return fetchHistoryPage(historyPageIndex).then(function (page) {
            syncReadReceipts(page.content || []);
        });
    }

    function renderMessage(dto) {
        if (!dto) {
            return;
        }
        if (dto.id != null) {
            var key = String(dto.id);
            if (renderedMessageIds[key]) {
                return;
            }
            renderedMessageIds[key] = true;
        }
        removeEmptyPlaceholder();
        var dayKey = wallDayKey(dto.createdAt);
        if (!dayKey) {
            dayKey = wallTodayKey();
        }
        var group = getOrCreateDayGroup(dayKey);
        var isMine = Number(dto.senderUserId) === viewerUserId;
        var item = document.createElement('div');
        item.className =
            'reservation-chat__message mb-2' + (isMine ? ' reservation-chat__message--mine' : '');
        if (dto.id != null) {
            item.setAttribute('data-message-id', String(dto.id));
        }
        var bubble = document.createElement('div');
        bubble.className =
            'reservation-chat__bubble ' +
            (isMine ? 'reservation-chat__bubble--mine' : 'reservation-chat__bubble--theirs');
        var bodyText = dto.body == null ? '' : String(dto.body).trim();
        var attachmentKind = dto.attachment ? String(dto.attachment.kind || 'GENERIC').toUpperCase() : '';
        if (dto.attachment) {
            bubble.classList.add('reservation-chat__bubble--has-attachment');
            if (!bodyText) {
                bubble.classList.add('reservation-chat__bubble--media-only');
                if (attachmentKind === 'IMAGE' || attachmentKind === 'VIDEO') {
                    bubble.classList.add('reservation-chat__bubble--visual-only');
                }
            }
            renderAttachment(bubble, dto.attachment);
        }
        var meta = buildMessageMeta(dto, isMine);
        if (bodyText) {
            var text = document.createElement('span');
            text.className =
                'reservation-chat__bubble-text' + (dto.attachment ? ' reservation-chat__attachment-caption' : '');
            text.textContent = bodyText;
            if (dto.attachment) {
                var footer = document.createElement('div');
                footer.className = 'reservation-chat__bubble-footer';
                footer.appendChild(text);
                footer.appendChild(meta);
                bubble.appendChild(footer);
            } else {
                bubble.appendChild(text);
                bubble.appendChild(meta);
            }
        } else {
            bubble.appendChild(meta);
        }
        item.appendChild(bubble);
        group.appendChild(item);
    }

    function scrollToBottom() {
        messagesEl.scrollTop = messagesEl.scrollHeight;
    }

    function afterMessagesChanged(scrollBottom) {
        observeDaySeparators();
        if (scrollBottom) {
            scrollToBottom();
            syncStickyToLastDay();
        } else {
            updateStickyFromScroll();
        }
    }

    function scrollToTop() {
        if (messagesEl) {
            messagesEl.scrollTop = 0;
        }
    }

    function renderAll(messages, scrollToBottomAfter) {
        clearMessageContent();
        renderedMessageIds = Object.create(null);
        if (!messages || messages.length === 0) {
            var p = document.createElement('p');
            p.className = 'text-muted small mb-0 reservation-chat__empty';
            messagesEl.appendChild(p);
            p.textContent = emptyLabel;
            setDayBarVisible(false);
            return;
        }
        setDayBarVisible(true);
        messages.forEach(renderMessage);
        afterMessagesChanged(scrollToBottomAfter !== false);
        if (scrollToBottomAfter === false) {
            scrollToTop();
        }
    }

    function appendMessage(dto) {
        renderMessage(dto);
        afterMessagesChanged(true);
    }

    function appendMergedMessages(messages) {
        var merged = messageMerge.mergeIncomingMessages(renderedMessageIds, messages);
        if (!merged.length) {
            return;
        }
        merged.forEach(function (dto) {
            renderMessage(dto);
        });
        afterMessagesChanged(true);
    }

    function buildHistoryUrl(pageParam) {
        var url =
            contextPath +
            '/my-reservations/' +
            reservationId +
            '/messages?size=' +
            encodeURIComponent(String(historyPageSize));
        if (pageParam !== undefined && pageParam !== null) {
            url += '&page=' + encodeURIComponent(String(pageParam));
        }
        return url;
    }

    function fetchHistoryPage(pageParam) {
        return fetch(buildHistoryUrl(pageParam), {
            credentials: 'same-origin',
            headers: { Accept: 'application/json' }
        }).then(function (response) {
            if (!response.ok) {
                throw new Error('history');
            }
            return response.json();
        });
    }

    function isOnLastHistoryPage() {
        return historyTotalPages <= 1 || historyPageIndex >= historyTotalPages - 1;
    }

    function updatePaginationControls() {
        var showNav = historyTotalPages > 1;
        if (loadOlderBtn) {
            if (showNav && historyHasPrevious) {
                loadOlderBtn.classList.remove('d-none');
                loadOlderBtn.disabled = historyLoading;
            } else {
                loadOlderBtn.classList.add('d-none');
                loadOlderBtn.disabled = true;
            }
        }
        if (loadNewerBtn) {
            if (showNav && historyHasNext) {
                loadNewerBtn.classList.remove('d-none');
                loadNewerBtn.disabled = historyLoading;
            } else {
                loadNewerBtn.classList.add('d-none');
                loadNewerBtn.disabled = true;
            }
        }
    }

    function applyHistoryPage(page, scrollToBottomAfter) {
        historyPageIndex = page.currentPage != null ? page.currentPage : 0;
        historyPageSize = page.pageSize != null ? page.pageSize : historyPageSize;
        historyTotalPages = page.totalPages != null ? page.totalPages : 1;
        historyHasPrevious = !!page.hasPrevious;
        historyHasNext = !!page.hasNext;
        renderAll(page.content || [], scrollToBottomAfter);
        updatePaginationControls();
        syncMessagePolling();
    }

    function stopMessagePolling() {
        if (pollTimer) {
            clearInterval(pollTimer);
            pollTimer = null;
        }
    }

    function syncMessagePolling() {
        if (isOnLastHistoryPage()) {
            startMessagePolling();
        } else {
            stopMessagePolling();
        }
    }

    function goToHistoryPage(pageParam, scrollToBottomAfter) {
        historyLoading = true;
        updatePaginationControls();
        return fetchHistoryPage(pageParam)
            .then(function (page) {
                applyHistoryPage(page, scrollToBottomAfter);
                historyLoaded = true;
                showError('');
            })
            .catch(function () {
                showError(errorLoadLabel);
                throw new Error('history');
            })
            .finally(function () {
                historyLoading = false;
                updatePaginationControls();
            });
    }

    function goToLastHistoryPageAfterSend() {
        if (isOnLastHistoryPage()) {
            return Promise.resolve();
        }
        return goToHistoryPage(null);
    }

    function fetchPollMessages() {
        var afterId = maxRenderedMessageId();
        return fetch(
            contextPath +
                '/my-reservations/' +
                reservationId +
                '/messages/poll?afterId=' +
                encodeURIComponent(String(afterId)),
            {
                credentials: 'same-origin',
                headers: { Accept: 'application/json' }
            }
        ).then(function (response) {
            if (!response.ok) {
                throw new Error('poll');
            }
            return response.json();
        });
    }

    function loadHistory() {
        if (historyLoaded) {
            return Promise.resolve();
        }
        return goToHistoryPage(null);
    }

    function pollMessages() {
        if (!historyLoaded || !isOnLastHistoryPage()) {
            return;
        }
        fetchPollMessages()
            .then(function (messages) {
                appendMergedMessages(messages);
                return refreshReadReceiptsOnCurrentPage();
            })
            .catch(function () {
                /* polling is best-effort */
            });
    }

    function startMessagePolling() {
        if (pollTimer || !isOnLastHistoryPage()) {
            return;
        }
        pollTimer = setInterval(pollMessages, POLL_INTERVAL_MS);
    }

    function handleSendSuccess(xhr) {
        if (isOnLastHistoryPage()) {
            tryAppendDtoFromResponse(xhr);
            return Promise.resolve();
        }
        return goToLastHistoryPageAfterSend();
    }

    function parseErrorResponse(xhr) {
        try {
            var data = JSON.parse(xhr.responseText);
            if (data && data.message) {
                return data.message;
            }
        } catch (e) {
            /* ignore */
        }
        return errorSendLabel;
    }

    function tryAppendDtoFromResponse(xhr) {
        try {
            var dto = JSON.parse(xhr.responseText);
            if (dto && dto.id != null) {
                appendMessage(dto);
            }
        } catch (e) {
            /* ignore */
        }
    }

    function uploadMessageWithFileOnce(file, body) {
        return new Promise(function (resolve, reject) {
            var formData = new FormData();
            if (body) {
                formData.append('body', body);
            }
            formData.append('file', file);
            var xhr = new XMLHttpRequest();
            xhr.open('POST', contextPath + '/my-reservations/' + reservationId + '/messages', true);
            xhr.setRequestHeader('Accept', 'application/json');
            xhr.timeout = uploadHelpers.computeUploadTimeoutMs(maxAttachmentMb);
            xhr.upload.onprogress = function (ev) {
                if (ev.lengthComputable) {
                    setUploadProgress((ev.loaded / ev.total) * 100);
                }
            };
            xhr.onload = function () {
                setUploadProgress(null);
                if (xhr.status >= 200 && xhr.status < 300) {
                    handleSendSuccess(xhr).then(resolve).catch(reject);
                    return;
                }
                showError(parseErrorResponse(xhr));
                reject(new Error('upload'));
            };
            xhr.onerror = function () {
                setUploadProgress(null);
                showError(errorSendLabel);
                reject(new Error('network'));
            };
            xhr.ontimeout = function () {
                setUploadProgress(null);
                showError(errorUploadTimeoutLabel);
                reject(new Error('timeout'));
            };
            setUploadProgress(0);
            xhr.send(formData);
        });
    }

    function uploadMessageWithFile(file, body, retriesLeft) {
        if (retriesLeft == null) {
            retriesLeft = UPLOAD_MAX_RETRIES;
        }
        uploading = true;
        showError(labelUploading);
        return uploadMessageWithFileOnce(file, body).catch(function (err) {
            if (retriesLeft > 0 && (err.message === 'network' || err.message === 'timeout')) {
                return uploadMessageWithFile(file, body, retriesLeft - 1);
            }
            throw err;
        }).finally(function () {
            uploading = false;
        });
    }

    function sendTextOverHttp(body) {
        return new Promise(function (resolve, reject) {
            var formData = new FormData();
            formData.append('body', body);
            var xhr = new XMLHttpRequest();
            xhr.open('POST', contextPath + '/my-reservations/' + reservationId + '/messages', true);
            xhr.setRequestHeader('Accept', 'application/json');
            xhr.onload = function () {
                if (xhr.status >= 200 && xhr.status < 300) {
                    handleSendSuccess(xhr).then(resolve).catch(reject);
                    return;
                }
                showError(parseErrorResponse(xhr));
                reject(new Error('send'));
            };
            xhr.onerror = function () {
                showError(errorSendLabel);
                reject(new Error('network'));
            };
            xhr.send(formData);
        });
    }

    function releaseSendLock() {
        sendGuard.release();
        if (!uploading) {
            setComposerEnabled(true);
        }
    }

    function sendMessage() {
        if (uploading || !sendGuard.tryAcquire()) {
            return;
        }
        setComposerEnabled(false);
        showError('');
        var body = (inputEl.value || '').trim();
        if (pendingFile) {
            var fileErr = validateFileClient(pendingFile);
            if (fileErr) {
                showError(fileErr);
                releaseSendLock();
                return;
            }
            var fileToSend = pendingFile;
            var caption = body;
            clearPendingFile();
            inputEl.value = '';
            uploadMessageWithFile(fileToSend, caption)
                .then(function () {
                    showError('');
                })
                .catch(function () {
                    /* error already shown */
                })
                .finally(function () {
                    releaseSendLock();
                });
            return;
        }
        if (!body) {
            releaseSendLock();
            return;
        }
        if (body.length > maxLength) {
            body = body.substring(0, maxLength);
        }
        inputEl.value = '';
        sendTextOverHttp(body)
            .catch(function () {
                inputEl.value = body;
            })
            .finally(function () {
                releaseSendLock();
            });
    }

    function handleFilesFromDrop(fileList) {
        if (!fileList || !fileList.length) {
            return;
        }
        setPendingFile(fileList[0]);
    }

    function initDragDrop() {
        if (!dropZoneEl) {
            return;
        }
        function isInsideDropZone(node) {
            return node && dropZoneEl.contains(node);
        }
        function onDragEnter(e) {
            e.preventDefault();
            e.stopPropagation();
            if (isInsideDropZone(e.relatedTarget)) {
                return;
            }
            showDropOverlay(true);
        }
        function onDragLeave(e) {
            e.preventDefault();
            e.stopPropagation();
            if (isInsideDropZone(e.relatedTarget)) {
                return;
            }
            showDropOverlay(false);
        }
        function onDragOver(e) {
            e.preventDefault();
            e.stopPropagation();
            if (e.dataTransfer) {
                e.dataTransfer.dropEffect = 'copy';
            }
        }
        function onDrop(e) {
            e.preventDefault();
            e.stopPropagation();
            showDropOverlay(false);
            if (e.dataTransfer && e.dataTransfer.files) {
                handleFilesFromDrop(e.dataTransfer.files);
            }
        }
        dropZoneEl.addEventListener('dragenter', onDragEnter);
        dropZoneEl.addEventListener('dragleave', onDragLeave);
        dropZoneEl.addEventListener('dragover', onDragOver);
        dropZoneEl.addEventListener('drop', onDrop);
    }

    function initChat() {
        showError('');
        initDragDrop();
        if (messagesEl) {
            messagesEl.addEventListener('scroll', function () {
                if (messagesEl.querySelector('.reservation-chat__empty')) {
                    return;
                }
                updateStickyFromScroll();
                showDayBarWhileScrolling();
            });
        }
        document.addEventListener('visibilitychange', function () {
            if (document.visibilityState === 'visible' && historyLoaded && isOnLastHistoryPage()) {
                pollMessages();
            }
        });
        if (loadOlderBtn) {
            loadOlderBtn.addEventListener('click', function () {
                if (historyLoading || !historyHasPrevious) {
                    return;
                }
                goToHistoryPage(historyPageIndex - 1, false);
            });
        }
        if (loadNewerBtn) {
            loadNewerBtn.addEventListener('click', function () {
                if (historyLoading || !historyHasNext) {
                    return;
                }
                goToHistoryPage(historyPageIndex + 1, true);
            });
        }
        loadHistory().catch(function () {
            /* error shown in goToHistoryPage */
        });
    }

    if (sendBtn) {
        sendBtn.addEventListener('click', sendMessage);
    }
    if (inputEl) {
        inputEl.addEventListener('keydown', function (e) {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                sendMessage();
            }
        });
    }
    if (attachBtn && fileInputEl) {
        attachBtn.addEventListener('click', function () {
            fileInputEl.click();
        });
        fileInputEl.addEventListener('change', function () {
            if (fileInputEl.files && fileInputEl.files.length) {
                setPendingFile(fileInputEl.files[0]);
            }
        });
    }

    initChat();
})();
