<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <title><spring:message code="reservationChat.pageTitle"/> — <spring:message code="app.title"/></title>
    <%@include file="header.jsp"%>
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
    <spring:message code="reservationChat.date.today" var="chatDateToday"/>
    <spring:message code="reservationChat.date.yesterday" var="chatDateYesterday"/>
    <spring:message code="myReservationDetail.counterparty.viewFullProfile" var="counterpartyProfileLinkAria"/>

    <c:url var="detailUrl" value="/my-reservations/${reservationId}">
        <c:param name="role" value="${reservationRole}"/>
        <c:if test="${not empty fromListing}">
            <c:param name="fromListing" value="${fromListing}"/>
        </c:if>
    </c:url>

    <c:url var="counterpartyProfileUrl" value="/my-reservations/${reservationId}/counterparty-profile">
        <c:param name="role" value="${reservationRole}"/>
        <c:if test="${not empty reservationDetailOwnerListingHubId}">
            <c:param name="fromListing" value="${reservationDetailOwnerListingHubId}"/>
        </c:if>
        <c:if test="${empty reservationDetailOwnerListingHubId and not empty fromListing}">
            <c:param name="fromListing" value="${fromListing}"/>
        </c:if>
    </c:url>

    <c:choose>
        <c:when test="${not empty reservationDetailOwnerListingHubId}">
            <spring:message code="myListings.tab.reservations" var="bcHomeLabel"/>
            <c:url var="bcHomeHref" value="/my-listings">
                <c:param name="tab" value="reservations"/>
            </c:url>
            <ryden:breadcrumbTrail
                    homeLabel="${bcHomeLabel}"
                    homeHref="${bcHomeHref}"
                    midLabel="${listingTitle}"
                    midHref="${detailUrl}"
                    currentLabel="${chatPageTitle}"/>
        </c:when>
        <c:otherwise>
            <spring:message code="navbar.myReservations" var="myReservationsLabel"/>
            <ryden:breadcrumbTrail
                    homeLabel="${myReservationsLabel}"
                    homeHref="${pageContext.request.contextPath}/my-reservations"
                    midLabel="${listingTitle}"
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
                 data-empty-label="<c:out value='${chatEmptyLabel}'/>"
                 data-error-load="<c:out value='${chatErrorLoad}'/>"
                 data-error-connection="<c:out value='${chatErrorConnection}'/>"
                 data-label-today="<c:out value='${chatDateToday}'/>"
                 data-label-yesterday="<c:out value='${chatDateYesterday}'/>">
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
                <div class="reservation-chat__composer reservation-chat-page__composer d-flex gap-2 align-items-end">
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
<%@ include file="includes/footerScripts.jspf" %>
</body>
</html>
