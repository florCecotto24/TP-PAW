<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<nav class="navbar navbar-expand shadow-sm fixed-top mb-0">
    <div class="container-fluid">
        <a class="navbar-brand ms-3 fw-semibold" href="${pageContext.request.contextPath}/">
            <img src="${pageContext.request.contextPath}/assets/images/Ryden_logo.ico" alt="Logo" width="30" height="24" class="d-inline-block align-text-top">
            <spring:message code="app.title"/></a>
        <div class="d-flex flex-row justify-content-end align-items-center">
            <ul class="navbar-nav nav-pills align-items-center mb-0">
                <li class="nav-item my-nav-item">
                    <a class="nav-link d-flex align-items-center ${activeTab == 'home' ? 'active' : ''}" href="${pageContext.request.contextPath}/"><spring:message code="navbar.home"/></a>
                </li>
                <li class="nav-item my-nav-item">
                    <a class="nav-link d-flex align-items-center ${activeTab == 'search' ? 'active' : ''}" aria-current="page" href="${pageContext.request.contextPath}/search"><spring:message code="navbar.explore"/></a>
                </li>
                <li class="nav-item px-1">
                    <a class="nav-link d-flex align-items-center ${activeTab == 'publish-car' ? 'active' : ''}" href="${pageContext.request.contextPath}/publish-car"><spring:message code="navbar.publish"/></a>
                </li>
                <sec:authorize access="isAuthenticated()">
                    <li class="nav-item px-1">
                        <a class="nav-link d-flex align-items-center ${activeTab == 'my-reservations' ? 'active' : ''}"
                           href="${pageContext.request.contextPath}/my-reservations"><spring:message code="navbar.myReservations"/></a>
                    </li>
                </sec:authorize>
                <%-- Language switch: visible to both anonymous and authenticated users (including auth screens).
                     Uses the SAME locale source as <spring:message> (LocaleContextHolder, populated once per
                     request by the DispatcherServlet) so the toggle's active button is always consistent with
                     the rendered copy. Calling RequestContextUtils.getLocale(request) would re-invoke the
                     resolver here and could drift from the cached value if the request mutated the source. --%>
                <spring:eval expression="T(org.springframework.context.i18n.LocaleContextHolder).getLocale().getLanguage()"
                             var="currentLocaleLanguage"/>
                <spring:message code="navbar.language.ariaLabel" var="languageToggleAria"/>
                <spring:message code="navbar.language.es" var="languageEsLabel"/>
                <spring:message code="navbar.language.en" var="languageEnLabel"/>
                <li class="nav-item px-1">
                    <form action="${pageContext.request.contextPath}/i18n/locale" method="post"
                          class="ryden-language-toggle" role="group"
                          aria-label="<c:out value='${languageToggleAria}'/>">
                        <button type="submit" name="lang" value="es"
                                class="ryden-language-toggle__btn ${currentLocaleLanguage == 'es' ? 'is-active' : ''}"
                                aria-pressed="${currentLocaleLanguage == 'es'}"
                                title="<c:out value='${languageEsLabel}'/>">ES</button>
                        <button type="submit" name="lang" value="en"
                                class="ryden-language-toggle__btn ${currentLocaleLanguage == 'en' ? 'is-active' : ''}"
                                aria-pressed="${currentLocaleLanguage == 'en'}"
                                title="<c:out value='${languageEnLabel}'/>">EN</button>
                    </form>
                </li>
                <sec:authorize access="isAnonymous()">
                    <li class="nav-item px-1">
                        <a class="nav-link d-flex align-items-center ${activeTab == 'login' ? 'active' : ''}" href="${pageContext.request.contextPath}/login"><spring:message code="navbar.login"/></a>
                    </li>
                </sec:authorize>
                <sec:authorize access="isAuthenticated()">
                    <li class="nav-item px-1 dropdown">
                        <spring:message code="navbar.userMenu.ariaLabel" var="userMenuAria"/>
                        <button type="button"
                                class="navbar-user-menu-toggle rounded-circle p-0 d-inline-flex align-items-center justify-content-center flex-shrink-0"
                                style="width:40px;height:40px;"
                                id="navbarUserMenuToggle"
                                data-bs-toggle="dropdown"
                                data-bs-popper="static"
                                aria-expanded="false"
                                aria-haspopup="true"
                                aria-label="<c:out value='${userMenuAria}'/>">
                            <c:choose>
                                <c:when test="${not empty navProfilePictureImageId}">
                                    <c:url var="navAvatarImgUrl" value="/image/${navProfilePictureImageId}"/>
                                    <spring:message code="navbar.avatar.alt" var="navAvatarAlt"/>
                                    <img src="<c:out value='${navAvatarImgUrl}'/>" alt="<c:out value='${navAvatarAlt}'/>" width="40" height="40" class="navbar-user-menu-toggle__img">
                                </c:when>
                                <c:otherwise>
                                    <c:set var="navInitialA" value="${not empty navUserForename and fn:length(navUserForename) > 0 ? fn:toUpperCase(fn:substring(navUserForename, 0, 1)) : ''}"/>
                                    <c:set var="navInitialB" value="${not empty navUserSurname and fn:length(navUserSurname) > 0 ? fn:toUpperCase(fn:substring(navUserSurname, 0, 1)) : ''}"/>
                                    <c:set var="navInitials" value="${navInitialA}${navInitialB}"/>
                                    <c:if test="${empty navInitials}">
                                        <c:set var="navInitials" value="?"/>
                                    </c:if>
                                    <span class="fw-semibold text-primary small user-select-none" aria-hidden="true"><c:out value="${navInitials}"/></span>
                                </c:otherwise>
                            </c:choose>
                        </button>
                        <ul class="dropdown-menu dropdown-menu-end shadow border-0 rounded-4 mt-2 py-2 px-1" aria-labelledby="navbarUserMenuToggle">
                            <sec:authorize access="hasRole('ADMIN')">
                                <li>
                                    <a class="dropdown-item rounded-3 py-2"
                                       href="${pageContext.request.contextPath}/admin/panel">
                                        <spring:message code="navbar.adminPanel"/>
                                    </a>
                                </li>
                                <li><hr class="dropdown-divider my-1"/></li>
                            </sec:authorize>
                            <li>
                                <a class="dropdown-item rounded-3 py-2 ${activeTab == 'profile' ? 'active' : ''}"
                                   href="${pageContext.request.contextPath}/profile"><spring:message code="navbar.profile"/></a>
                            </li>
                            <li><hr class="dropdown-divider my-2"></li>
                            <li>
                                <a class="dropdown-item rounded-3 py-2 ${activeTab == 'my-cars' ? 'active' : ''}"
                                   href="${pageContext.request.contextPath}/my-cars"><spring:message code="navbar.myCars"/></a>
                            </li>
                            <li>
                                <a class="dropdown-item rounded-3 py-2 ${activeTab == 'owner-reservations' ? 'active' : ''}"
                                   href="${pageContext.request.contextPath}/my-cars/reservations"><spring:message code="navbar.ownerReservations"/></a>
                            </li>
                            <li>
                                <a class="dropdown-item rounded-3 py-2 ${activeTab == 'my-favorites' ? 'active' : ''}"
                                   href="${pageContext.request.contextPath}/my-favorites"><spring:message code="navbar.myFavorites"/></a>
                            </li>
                            <li><hr class="dropdown-divider my-2"></li>
                            <li>
                                <button type="button"
                                        class="dropdown-item rounded-3 py-2 text-danger"
                                        data-bs-toggle="modal"
                                        data-bs-target="#navbarLogoutModal">
                                    <spring:message code="navbar.logout"/>
                                </button>
                            </li>
                        </ul>
                    </li>
                </sec:authorize>
            </ul>
        </div>
    </div>
</nav>

<%-- Global "your account is blocked" banner. Always rendered when the signed-in user is blocked
     (computed by NavModelAdvice on every request, so a freshly applied block by the overdue-refund-proof
     sweep takes effect on the next page load without re-login). The CTA points at the single overdue
     reservation when exactly one is pending; otherwise it falls back to the owner reservations list. --%>
<sec:authorize access="isAuthenticated()">
    <c:if test="${navUserBlocked}">
        <%-- The .ryden-blocked-banner class adds margin-top equal to --navbar-height by default so the
             banner clears the fixed navbar on pages that do NOT use body.has-fixed-navbar (e.g. home,
             where the hero section is intentionally placed flush against the top of the viewport).
             On pages that DO use body.has-fixed-navbar, the same CSS neutralises that margin because
             the body padding already reserves the navbar's slice. --%>
        <div class="alert alert-danger border-0 rounded-0 mb-0 d-flex flex-wrap align-items-center gap-2 px-3 py-2 ryden-blocked-banner"
             role="alert"
             aria-live="polite">
            <i class="bi bi-shield-exclamation fs-5 me-1" aria-hidden="true"></i>
            <div class="flex-grow-1">
                <strong class="d-block"><spring:message code="navbar.blockedBanner.title"/></strong>
                <span class="small"><spring:message code="navbar.blockedBanner.body"/></span>
            </div>
            <%-- Deep-link CTA: when exactly one reservation has an overdue refund proof, go straight to
                 its detail page (where the upload button lives). Otherwise (0 — race with auto-unblock —
                 or many) point at the owner reservations list so the owner can pick which to upload. --%>
            <c:choose>
                <c:when test="${not empty navBlockedSingleReservationId}">
                    <a class="btn btn-light btn-sm fw-semibold border"
                       href="${pageContext.request.contextPath}/my-reservations/${navBlockedSingleReservationId}?role=OWNER">
                        <i class="bi bi-upload me-1" aria-hidden="true"></i>
                        <spring:message code="navbar.blockedBanner.cta.uploadSingle"/>
                    </a>
                </c:when>
                <c:otherwise>
                    <a class="btn btn-light btn-sm fw-semibold border"
                       href="${pageContext.request.contextPath}/my-cars/reservations">
                        <i class="bi bi-receipt me-1" aria-hidden="true"></i>
                        <spring:message code="navbar.blockedBanner.cta"/>
                    </a>
                </c:otherwise>
            </c:choose>
        </div>
    </c:if>
</sec:authorize>

<sec:authorize access="isAuthenticated()">
    <div class="modal fade" id="navbarLogoutModal" tabindex="-1" aria-hidden="true" aria-labelledby="navbarLogoutModalLabel">
        <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content rounded-4 border-0 shadow">
                <div class="modal-header border-0 pb-0">
                    <h2 class="modal-title h5 fw-semibold" id="navbarLogoutModalLabel"><spring:message code="navbar.logoutConfirm.title"/></h2>
                    <spring:message code="common.close" var="logoutModalCloseAria"/>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="<c:out value='${logoutModalCloseAria}'/>"></button>
                </div>
                <div class="modal-body pt-2">
                    <p class="text-secondary mb-0"><spring:message code="navbar.logoutConfirm.message"/></p>
                </div>
                <div class="modal-footer border-0 pt-0">
                    <button type="button" class="btn btn-light border rounded-3" data-bs-dismiss="modal"><spring:message code="common.cancel"/></button>
                    <form action="${pageContext.request.contextPath}/logout" method="post" class="d-inline">
                        <input type="hidden" name="<c:out value='${_csrf.parameterName}'/>" value="<c:out value='${_csrf.token}'/>"/>
                        <button type="submit" class="btn btn-primary rounded-3"><spring:message code="navbar.logoutConfirm.submit"/></button>
                    </form>
                </div>
            </div>
        </div>
    </div>
</sec:authorize>
