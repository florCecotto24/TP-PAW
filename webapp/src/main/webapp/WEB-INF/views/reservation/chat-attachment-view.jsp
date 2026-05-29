<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><spring:message code="app.title"/> - <c:out value="${attachmentFileName}"/></title>
    <link rel="icon" type="image/svg+xml" href="${pageContext.request.contextPath}/assets/images/Ryden_logo.ico">
    <style>
        html, body {
            margin: 0;
            padding: 0;
            height: 100%;
            background: #f5f4f0;
        }

        .chat-attachment-view__frame {
            width: 100%;
            height: 100%;
            border: 0;
        }

        .chat-attachment-view__image {
            display: block;
            max-width: 100%;
            max-height: 100%;
            margin: 0 auto;
            object-fit: contain;
        }

        .chat-attachment-view__media-wrap {
            display: none;
            align-items: center;
            justify-content: center;
            height: 100%;
            padding: 1rem;
            box-sizing: border-box;
        }

        .chat-attachment-view__media-wrap--visible {
            display: flex;
        }

        .chat-attachment-view__frame--hidden {
            display: none;
        }
    </style>
</head>
<body>
<div id="chatAttachmentImageWrap" class="chat-attachment-view__media-wrap">
    <img id="chatAttachmentImage" class="chat-attachment-view__image" alt=""/>
</div>
<iframe
        id="chatAttachmentFrame"
        class="chat-attachment-view__frame"
        title="<c:out value='${attachmentFileName}'/>"></iframe>
<script>
    (function () {
        var downloadUrl = "<c:url value='/my-reservations/${reservationId}/messages/${messageId}/attachment/download'/>";
        var fallbackUrl = "<c:url value='/my-reservations/${reservationId}'/>";
        var frame = document.getElementById('chatAttachmentFrame');
        var imageWrap = document.getElementById('chatAttachmentImageWrap');
        var imageEl = document.getElementById('chatAttachmentImage');

        fetch(downloadUrl, { credentials: 'same-origin' })
            .then(function (response) {
                if (!response.ok) {
                    throw new Error('Attachment could not be loaded');
                }
                var contentType = (response.headers.get('Content-Type') || '').toLowerCase();
                return response.blob().then(function (blob) {
                    return { blob: blob, contentType: contentType };
                });
            })
            .then(function (result) {
                var objectUrl = URL.createObjectURL(result.blob);
                window.addEventListener('beforeunload', function () {
                    URL.revokeObjectURL(objectUrl);
                });
                if (result.contentType.indexOf('image/') === 0) {
                    frame.classList.add('chat-attachment-view__frame--hidden');
                    imageWrap.classList.add('chat-attachment-view__media-wrap--visible');
                    imageEl.src = objectUrl;
                    imageEl.alt = "<c:out value='${attachmentFileName}'/>";
                    return;
                }
                if (result.contentType.indexOf('video/') === 0) {
                    frame.classList.add('chat-attachment-view__frame--hidden');
                    imageWrap.classList.add('chat-attachment-view__media-wrap--visible');
                    var video = document.createElement('video');
                    video.className = 'chat-attachment-view__image';
                    video.controls = true;
                    video.src = objectUrl;
                    imageWrap.innerHTML = '';
                    imageWrap.appendChild(video);
                    return;
                }
                frame.src = objectUrl;
            })
            .catch(function () {
                window.location.href = fallbackUrl;
            });
    })();
</script>
</body>
</html>
