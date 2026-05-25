(function () {
    'use strict';

    var WALL_TIMEZONE = 'America/Argentina/Buenos_Aires';

    var root = document.getElementById('reservationChatRoot');
    if (!root) {
        return;
    }

    var contextPath = root.getAttribute('data-context-path') || '';
    var reservationId = root.getAttribute('data-reservation-id');
    var viewerUserId = Number(root.getAttribute('data-viewer-user-id'));
    var maxLength = Number(root.getAttribute('data-max-length')) || 1000;
    var emptyLabel = root.getAttribute('data-empty-label') || '';
    var labelToday = root.getAttribute('data-label-today') || 'Today';
    var labelYesterday = root.getAttribute('data-label-yesterday') || 'Yesterday';
    var errorLoadLabel = root.getAttribute('data-error-load') || 'Could not load chat. Try again later.';
    var errorConnectionLabel = root.getAttribute('data-error-connection') || 'Connection lost. Refresh the page.';

    var messagesEl = document.getElementById('reservationChatMessages');
    var dayBarEl = document.getElementById('reservationChatDayBar');
    var dayLabelEl = document.getElementById('reservationChatDayLabel');
    var inputEl = document.getElementById('reservationChatInput');
    var sendBtn = document.getElementById('reservationChatSend');
    var errorEl = document.getElementById('reservationChatError');

    var stompClient = null;
    var historyLoaded = false;
    var connected = false;
    var dayGroups = Object.create(null);
    var daySeparators = Object.create(null);
    var dayObserver = null;
    var scrollFadeTimer = null;
    var lastStickyDayKey = null;

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

    function parseTimestamp(value) {
        if (value == null || value === '') {
            return null;
        }
        if (typeof value === 'number' && isFinite(value)) {
            return new Date(value < 1e12 ? value * 1000 : value);
        }
        if (typeof value === 'string') {
            var parsed = Date.parse(value);
            return isNaN(parsed) ? null : new Date(parsed);
        }
        return null;
    }

    function intlLocale() {
        var lang = document.documentElement.lang;
        return lang && lang.trim() ? lang.trim() : undefined;
    }

    function wallDayKeyFromDate(date) {
        if (!date || isNaN(date.getTime())) {
            return '';
        }
        return new Intl.DateTimeFormat('en-CA', {
            timeZone: WALL_TIMEZONE,
            year: 'numeric',
            month: '2-digit',
            day: '2-digit'
        }).format(date);
    }

    function wallDayKey(value) {
        return wallDayKeyFromDate(parseTimestamp(value));
    }

    function wallTodayKey() {
        return wallDayKeyFromDate(new Date());
    }

    function wallYesterdayKey() {
        var todayKey = wallTodayKey();
        var parts = todayKey.split('-');
        if (parts.length !== 3) {
            return '';
        }
        var prior = new Date(Date.UTC(Number(parts[0]), Number(parts[1]) - 1, Number(parts[2]) - 1));
        return wallDayKeyFromDate(prior);
    }

    function formatDayLabel(dayKey) {
        if (!dayKey) {
            return '';
        }
        if (dayKey === wallTodayKey()) {
            return labelToday;
        }
        if (dayKey === wallYesterdayKey()) {
            return labelYesterday;
        }
        try {
            return new Intl.DateTimeFormat(intlLocale(), {
                timeZone: WALL_TIMEZONE,
                dateStyle: 'medium'
            }).format(new Date(dayKey + 'T12:00:00'));
        } catch (e) {
            return dayKey;
        }
    }

    function formatMessageTime(value) {
        var date = parseTimestamp(value);
        if (!date || isNaN(date.getTime())) {
            return '';
        }
        try {
            return new Intl.DateTimeFormat(intlLocale(), {
                timeZone: WALL_TIMEZONE,
                timeStyle: 'short'
            }).format(date);
        } catch (e) {
            return '';
        }
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
        return node && node.id === 'reservationChatDayBar';
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

    function renderMessage(dto) {
        if (!dto) {
            return;
        }
        removeEmptyPlaceholder();
        var dayKey = wallDayKey(dto.createdAt);
        if (!dayKey) {
            dayKey = wallTodayKey();
        }
        var group = getOrCreateDayGroup(dayKey);
        var isMine = Number(dto.senderUserId) === viewerUserId;
        var item = document.createElement('div');
        item.className = 'reservation-chat__message mb-2' + (isMine ? ' text-end' : '');
        var bubble = document.createElement('div');
        bubble.className =
            'reservation-chat__bubble ' +
            (isMine ? 'reservation-chat__bubble--mine' : 'reservation-chat__bubble--theirs');
        var text = document.createElement('span');
        text.className = 'reservation-chat__bubble-text';
        text.textContent = dto.body == null ? '' : String(dto.body);
        var time = document.createElement('span');
        time.className = 'reservation-chat__time';
        time.textContent = formatMessageTime(dto.createdAt);
        bubble.appendChild(text);
        bubble.appendChild(time);
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

    function renderAll(messages) {
        clearMessageContent();
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
        afterMessagesChanged(true);
    }

    function appendMessage(dto) {
        renderMessage(dto);
        afterMessagesChanged(true);
    }

    function loadHistory() {
        if (historyLoaded) {
            return Promise.resolve();
        }
        return fetch(contextPath + '/my-reservations/' + reservationId + '/messages?page=0', {
            credentials: 'same-origin',
            headers: { Accept: 'application/json' }
        })
            .then(function (response) {
                if (!response.ok) {
                    throw new Error('history');
                }
                return response.json();
            })
            .then(function (messages) {
                renderAll(messages);
                historyLoaded = true;
            });
    }

    function markDisconnected() {
        connected = false;
    }

    function connectStomp() {
        if (connected && stompClient && stompClient.connected) {
            return Promise.resolve();
        }
        return new Promise(function (resolve, reject) {
            var socket = new SockJS(contextPath + '/ws');
            socket.onclose = markDisconnected;
            stompClient = Stomp.over(socket);
            stompClient.debug = null;
            stompClient.connect(
                {},
                function () {
                    connected = true;
                    stompClient.subscribe('/topic/reservations/' + reservationId, function (frame) {
                        try {
                            var dto = JSON.parse(frame.body);
                            appendMessage(dto);
                        } catch (e) {
                            showError(errorLoadLabel);
                        }
                    });
                    resolve();
                },
                function () {
                    markDisconnected();
                    reject(new Error('stomp'));
                }
            );
        });
    }

    function sendMessage() {
        showError('');
        var body = (inputEl.value || '').trim();
        if (!body) {
            return;
        }
        if (body.length > maxLength) {
            body = body.substring(0, maxLength);
        }
        if (!stompClient || !stompClient.connected) {
            showError(errorConnectionLabel);
            return;
        }
        try {
            stompClient.send(
                '/app/reservations/' + reservationId + '/messages',
                {},
                JSON.stringify({ body: body })
            );
            inputEl.value = '';
        } catch (e) {
            markDisconnected();
            showError(errorConnectionLabel);
        }
    }

    function initChat() {
        showError('');
        if (messagesEl) {
            messagesEl.addEventListener('scroll', function () {
                if (messagesEl.querySelector('.reservation-chat__empty')) {
                    return;
                }
                updateStickyFromScroll();
                showDayBarWhileScrolling();
            });
        }
        Promise.all([loadHistory(), connectStomp()]).catch(function () {
            showError(errorLoadLabel);
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

    initChat();
})();
