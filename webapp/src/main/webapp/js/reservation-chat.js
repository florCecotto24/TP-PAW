(function () {
    'use strict';

    var root = document.getElementById('reservationChatRoot');
    if (!root) {
        return;
    }

    var contextPath = root.getAttribute('data-context-path') || '';
    var reservationId = root.getAttribute('data-reservation-id');
    var viewerUserId = Number(root.getAttribute('data-viewer-user-id'));
    var maxLength = Number(root.getAttribute('data-max-length')) || 1000;
    var emptyLabel = root.getAttribute('data-empty-label') || '';
    var errorLoadLabel = root.getAttribute('data-error-load') || 'Could not load chat. Try again later.';
    var errorConnectionLabel = root.getAttribute('data-error-connection') || 'Connection lost. Refresh the page.';

    var messagesEl = document.getElementById('reservationChatMessages');
    var inputEl = document.getElementById('reservationChatInput');
    var sendBtn = document.getElementById('reservationChatSend');
    var errorEl = document.getElementById('reservationChatError');

    var stompClient = null;
    var historyLoaded = false;
    var connected = false;

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

    function escapeText(text) {
        var span = document.createElement('span');
        span.textContent = text == null ? '' : String(text);
        return span.textContent;
    }

    function parseTimestamp(value) {
        if (value == null || value === '') {
            return null;
        }
        if (typeof value === 'number' && isFinite(value)) {
            // Jackson may emit epoch seconds; JavaScript Date expects milliseconds.
            return new Date(value < 1e12 ? value * 1000 : value);
        }
        if (typeof value === 'string') {
            var parsed = Date.parse(value);
            return isNaN(parsed) ? null : new Date(parsed);
        }
        return null;
    }

    function formatTimestamp(value) {
        var date = parseTimestamp(value);
        if (!date || isNaN(date.getTime())) {
            return '';
        }
        try {
            return new Intl.DateTimeFormat(undefined, {
                dateStyle: 'short',
                timeStyle: 'short'
            }).format(date);
        } catch (e) {
            return '';
        }
    }

    function clearMessages() {
        while (messagesEl.firstChild) {
            messagesEl.removeChild(messagesEl.firstChild);
        }
    }

    function appendMessage(dto) {
        if (!dto) {
            return;
        }
        var empty = messagesEl.querySelector('.reservation-chat__empty');
        if (empty) {
            empty.remove();
        }
        var isMine = Number(dto.senderUserId) === viewerUserId;
        var item = document.createElement('div');
        item.className = 'reservation-chat__message mb-2' + (isMine ? ' text-end' : '');
        var meta = document.createElement('div');
        meta.className = 'small text-muted mb-1';
        meta.textContent = escapeText(dto.senderDisplayName) + ' · ' + formatTimestamp(dto.createdAt);
        var body = document.createElement('div');
        body.className = 'reservation-chat__bubble ' + (isMine ? 'reservation-chat__bubble--mine' : 'reservation-chat__bubble--theirs');
        body.textContent = dto.body;
        item.appendChild(meta);
        item.appendChild(body);
        messagesEl.appendChild(item);
        messagesEl.scrollTop = messagesEl.scrollHeight;
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
                clearMessages();
                if (!messages || messages.length === 0) {
                    var p = document.createElement('p');
                    p.className = 'text-muted small mb-0 reservation-chat__empty';
                    p.textContent = emptyLabel;
                    messagesEl.appendChild(p);
                } else {
                    messages.forEach(appendMessage);
                }
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
