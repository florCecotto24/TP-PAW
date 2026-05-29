<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <title><spring:message code="reservationChat.pageTitle"/> — <spring:message code="app.title"/></title>
    <%@include file="../header.jsp"%>
</head>
<body class="has-fixed-navbar bg-light reservation-chat-body">
<ryden:navbar/>

<main class="container-xl reservation-chat-main pt-3 pb-0">
    <spring:message code="reservationChat.pageTitle" var="chatPageTitle"/>
    <spring:message code="myReservationDetail.chat.send" var="chatSendLabel"/>
    <spring:message code="myReservationDetail.chat.placeholder" var="chatPlaceholder"/>
    <spring:message code="myReservationDetail.chat.empty" var="chatEmptyLabel"/>
    <spring:message code="reservationChat.error.load" var="chatErrorLoad"/>
    <spring:message code="reservationChat.error.connection" var="chatErrorConnection"/>
    <spring:message code="reservationChat.error.send" var="chatErrorSend"/>
    <spring:message code="reservationChat.date.today" var="chatDateToday"/>
    <spring:message code="reservationChat.date.yesterday" var="chatDateYesterday"/>
    <spring:message code="reservationChat.attach.button" var="chatAttachLabel"/>
    <spring:message code="reservationChat.attach.dropHint" var="chatDropHint"/>
    <spring:message code="reservationChat.attach.uploading" var="chatUploadingLabel"/>
    <spring:message code="reservationChat.attach.tooLarge" var="chatTooLargeLabel"/>
    <spring:message code="reservationChat.attach.invalidType" var="chatInvalidTypeLabel"/>
    <spring:message code="reservationChat.attach.cancel" var="chatCancelLabel"/>
    <spring:message code="reservationChat.attach.sendFile" var="chatSendFileLabel"/>
    <spring:message code="myReservationDetail.counterparty.viewFullProfile" var="counterpartyProfileLinkAria"/>

    <c:url var="detailUrl" value="/my-reservations/${reservationId}">
        <c:param name="role" value="${reservationRole}"/>
        <c:if test="${not empty fromCar}">
            <c:param name="fromCar" value="${fromCar}"/>
        </c:if>
    </c:url>

    <c:url var="counterpartyProfileUrl" value="/my-reservations/${reservationId}/counterparty-profile">
        <c:param name="role" value="${reservationRole}"/>
        <c:if test="${not empty reservationDetailOwnerCarHubId}">
            <c:param name="fromCar" value="${reservationDetailOwnerCarHubId}"/>
        </c:if>
        <c:if test="${empty reservationDetailOwnerCarHubId and not empty fromCar}">
            <c:param name="fromCar" value="${fromCar}"/>
        </c:if>
    </c:url>

    <c:choose>
        <c:when test="${not empty reservationDetailOwnerCarHubId}">
            <spring:message code="myListings.tab.reservations" var="bcHomeLabel"/>
            <c:url var="bcHomeHref" value="/my-cars">
                <c:param name="tab" value="reservations"/>
            </c:url>
            <ryden:breadcrumbTrail
                    homeLabel="${bcHomeLabel}"
                    homeHref="${bcHomeHref}"
                    midLabel="${vehicleLabel}"
                    midHref="${detailUrl}"
                    currentLabel="${chatPageTitle}"/>
        </c:when>
        <c:otherwise>
            <spring:message code="navbar.myReservations" var="myReservationsLabel"/>
            <ryden:breadcrumbTrail
                    homeLabel="${myReservationsLabel}"
                    homeHref="${pageContext.request.contextPath}/my-reservations"
                    midLabel="${vehicleLabel}"
                    midHref="${detailUrl}"
                    currentLabel="${chatPageTitle}"/>
        </c:otherwise>
    </c:choose>

    <div class="card border-0 shadow-sm rounded-4 reservation-chat-card flex-grow-1 min-h-0 mt-2">
        <div class="card-body p-0 d-flex flex-column min-h-0 h-100">
            <div id="reservationChatRoot"
                 class="reservation-chat reservation-chat-page reservation-chat-shell"
                 data-context-path="<c:out value='${pageContext.request.contextPath}'/>"
                 data-reservation-id="<c:out value='${reservationId}'/>"
                 data-viewer-user-id="<c:out value='${chatViewerUserId}'/>"
                 data-max-length="<c:out value='${chatMessageMaxLength}'/>"
                 data-max-attachment-mb="<c:out value='${chatMaxAttachmentMegabytes}'/>"
                 data-attach-label="<c:out value='${chatAttachLabel}'/>"
                 data-drop-hint="<c:out value='${chatDropHint}'/>"
                 data-uploading-label="<c:out value='${chatUploadingLabel}'/>"
                 data-too-large-label="<c:out value='${chatTooLargeLabel}'/>"
                 data-invalid-type-label="<c:out value='${chatInvalidTypeLabel}'/>"
                 data-cancel-label="<c:out value='${chatCancelLabel}'/>"
                 data-send-file-label="<c:out value='${chatSendFileLabel}'/>"
                 data-empty-label="<c:out value='${chatEmptyLabel}'/>"
                 data-error-load="<c:out value='${chatErrorLoad}'/>"
                 data-error-connection="<c:out value='${chatErrorConnection}'/>"
                 data-error-send="<c:out value='${chatErrorSend}'/>"
                 data-label-today="<c:out value='${chatDateToday}'/>"
                 data-label-yesterday="<c:out value='${chatDateYesterday}'/>">
                <div id="reservationChatDropZone" class="reservation-chat__drop-zone d-flex flex-column flex-grow-1 min-h-0">
                    <div id="reservationChatDropOverlay" class="reservation-chat__drop-overlay d-none" aria-hidden="true">
                        <span class="reservation-chat__drop-overlay-text"><c:out value="${chatDropHint}"/></span>
                    </div>
                <header class="reservation-chat-header">
                    <a href="<c:out value='${counterpartyProfileUrl}'/>"
                       class="reservation-chat-header__link text-decoration-none text-reset d-flex align-items-center gap-2 min-w-0"
                       aria-label="<c:out value='${counterpartyProfileLinkAria}'/>">
                        <c:choose>
                            <c:when test="${counterpartyProfileImageId != null}">
                                <c:url var="counterpartyChatAvatarUrl" value="/image/${counterpartyProfileImageId}"/>
                                <img src="${counterpartyChatAvatarUrl}"
                                     alt=""
                                     class="reservation-chat-header__avatar rounded-circle flex-shrink-0"/>
                            </c:when>
                            <c:otherwise>
                                <span class="reservation-chat-header__avatar reservation-chat-header__avatar--placeholder rounded-circle d-inline-flex align-items-center justify-content-center flex-shrink-0"
                                      aria-hidden="true">
                                    <i class="bi bi-person-fill"></i>
                                </span>
                            </c:otherwise>
                        </c:choose>
                        <span class="reservation-chat-header__name text-break">
                            <c:out value="${counterpartyDisplayName}"/>
                        </span>
                    </a>
                </header>
                <div id="reservationChatMessages"
                     class="reservation-chat__messages reservation-chat-page__messages"
                     role="log"
                     aria-live="polite">
                    <div id="reservationChatDayBar" class="reservation-chat-day-bar d-none" aria-live="polite">
                        <span id="reservationChatDayLabel" class="reservation-chat__day-pill reservation-chat-day-bar__label"></span>
                    </div>
                    <p class="text-muted small mb-0 reservation-chat__empty"><c:out value="${chatEmptyLabel}"/></p>
                </div>
                </div>
                <div id="reservationChatPending" class="reservation-chat__pending d-none" aria-live="polite"></div>
                <div id="reservationChatUploadProgress" class="reservation-chat__upload-progress d-none" role="progressbar"
                     aria-valuemin="0" aria-valuemax="100" aria-valuenow="0">
                    <div id="reservationChatUploadProgressBar" class="reservation-chat__upload-progress-bar"></div>
                </div>
                <div class="reservation-chat__composer reservation-chat-page__composer d-flex gap-2 align-items-end">
                    <input type="file" id="reservationChatFileInput" class="visually-hidden"
                           accept="image/*,application/pdf,.doc,.docx,video/mp4,video/webm,video/quicktime,.txt,.zip,.xls,.xlsx,.ppt,.pptx"/>
                    <button type="button" id="reservationChatAttach" class="btn btn-outline-secondary flex-shrink-0"
                            title="<c:out value='${chatAttachLabel}'/>" aria-label="<c:out value='${chatAttachLabel}'/>">
                        <i class="bi bi-paperclip" aria-hidden="true"></i>
                    </button>
                    <label class="visually-hidden" for="reservationChatInput"><c:out value="${chatPlaceholder}"/></label>
                    <textarea id="reservationChatInput" class="form-control reservation-chat-page__input" rows="1" maxlength="${chatMessageMaxLength}"
                              placeholder="<c:out value='${chatPlaceholder}'/>"></textarea>
                    <button type="button" id="reservationChatSend" class="btn btn-primary flex-shrink-0">
                        <c:out value="${chatSendLabel}"/>
                    </button>
                </div>
                <div id="reservationChatError" class="reservation-chat-page__error text-danger small d-none" role="alert"></div>
            </div>
        </div>
    </div>
</main>

<script src="https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js" crossorigin="anonymous"></script>
<script src="https://cdn.jsdelivr.net/npm/stompjs@2.3.3/lib/stomp.min.js" crossorigin="anonymous"></script>
<script src="${pageContext.request.contextPath}/js/reservation-chat.js"></script>
<%@ include file="../includes/footerScripts.jspf" %>
</body>
</html>
