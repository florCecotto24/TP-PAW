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
<body class="has-fixed-navbar bg-light">
<ryden:navbar/>

<main class="container pt-5 pb-4">
    <spring:message code="reservationChat.pageTitle" var="chatPageTitle"/>
    <spring:message code="reservationChat.withCounterparty" arguments="${counterpartyDisplayName}" var="chatSubtitle"/>
    <spring:message code="myReservationDetail.chat.send" var="chatSendLabel"/>
    <spring:message code="myReservationDetail.chat.placeholder" var="chatPlaceholder"/>
    <spring:message code="myReservationDetail.chat.empty" var="chatEmptyLabel"/>
    <spring:message code="reservationChat.error.load" var="chatErrorLoad"/>
    <spring:message code="reservationChat.error.connection" var="chatErrorConnection"/>

    <c:url var="detailUrl" value="/my-reservations/${reservationId}">
        <c:param name="role" value="${reservationRole}"/>
        <c:if test="${not empty fromListing}">
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

    <header class="reservation-management-header mb-4">
        <h1 class="h3 fw-bold mb-1"><c:out value="${chatPageTitle}"/></h1>
        <p class="text-muted mb-0"><c:out value="${chatSubtitle}"/></p>
    </header>

    <div class="card border-0 shadow-sm rounded-4">
        <div class="card-body p-4">
            <div id="reservationChatRoot"
                 class="reservation-chat reservation-chat-page"
                 data-context-path="<c:out value='${pageContext.request.contextPath}'/>"
                 data-reservation-id="<c:out value='${reservationId}'/>"
                 data-viewer-user-id="<c:out value='${chatViewerUserId}'/>"
                 data-max-length="<c:out value='${chatMessageMaxLength}'/>"
                 data-empty-label="<c:out value='${chatEmptyLabel}'/>"
                 data-error-load="<c:out value='${chatErrorLoad}'/>"
                 data-error-connection="<c:out value='${chatErrorConnection}'/>">
                <div id="reservationChatMessages"
                     class="reservation-chat__messages reservation-chat-page__messages border rounded-3 p-3 mb-3 bg-light"
                     role="log"
                     aria-live="polite">
                    <p class="text-muted small mb-0 reservation-chat__empty"><c:out value="${chatEmptyLabel}"/></p>
                </div>
                <div class="reservation-chat__composer reservation-chat-page__composer d-flex gap-2 align-items-end">
                    <label class="visually-hidden" for="reservationChatInput"><c:out value="${chatPlaceholder}"/></label>
                    <textarea id="reservationChatInput" class="form-control" rows="3" maxlength="${chatMessageMaxLength}"
                              placeholder="<c:out value='${chatPlaceholder}'/>"></textarea>
                    <button type="button" id="reservationChatSend" class="btn btn-primary flex-shrink-0">
                        <c:out value="${chatSendLabel}"/>
                    </button>
                </div>
                <div id="reservationChatError" class="text-danger small mt-2 d-none" role="alert"></div>
            </div>
        </div>
    </div>
</main>

<script src="https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js" crossorigin="anonymous"></script>
<script src="https://cdn.jsdelivr.net/npm/stompjs@2.3.3/lib/stomp.min.js" crossorigin="anonymous"></script>
<script src="${pageContext.request.contextPath}/js/reservation-chat.js"></script>

<%@include file="footer.jsp"%>
</body>
</html>
