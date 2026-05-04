<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<!DOCTYPE html>
<html lang="en">

    <head>
        <title><spring:message code="app.title"/> - <spring:message code="home.pageTitle"/></title>
        <%@include file="header.jsp" %>
    </head>

    <body>
        <ryden:navbar />
        <c:if test="${passwordResetSignedIn != null}">
            <div class="container pt-3">
                <div class="alert alert-success mb-0" role="alert">
                    <spring:message code="home.passwordResetSignedIn"/>
                </div>
            </div>
        </c:if>
        <div>
            <div class="hero-section w-100">
                <div class="hero-overlay pb-2">
                    <div class="hero-text text-center text-white mb-4">
                        <h1 class="fw-bold"><spring:message code="home.hero.title"/></h1>
                        <p class="lead"><spring:message code="home.hero.subtitle"/></p>
                        <p class="small mb-0 opacity-90"><spring:message code="home.hero.argentinaReservationsNote"/></p>
                    </div>
                </div>
            </div>


            <div class="sticky-top w-100 pt-2" style="top: 55px; z-index: 1020; background-color: #f9fbff;">
                <c:url var="homeSearchFiltersClearHref" value="/"/>
                <c:set var="hasActiveSearchFilters"
                       value="${not empty param.query or not empty param.from or not empty param.until or not empty searchSanitizedNeighborhoodIds or not empty paramValues.category or not empty paramValues.transmission or not empty paramValues.powertrain or not empty paramValues.price}"/>
                <ryden:searchWithFilters
                        formId="homeSearchForm"
                        formClass="search-menu w-100"
                        actionPath="/search"
                        showFilters="true"
                        autoSubmitOnFilterChange="false"
                        clearFiltersHref="${homeSearchFiltersClearHref}"
                        showClearFilters="${hasActiveSearchFilters}"
                        categoryFilterOptions="${categoryFilterOptions}"
                        transmissionFilterOptions="${transmissionFilterOptions}"
                        powertrainFilterOptions="${powertrainFilterOptions}"/>
            </div>


            <div class="container mt-5">
                <section class="carouselSection" id="cheapestVehiclesSection">
                    <spring:message code="home.cheapest.title" var="cheapestTitle"/>
                    <spring:message code="home.cheapest.subtitle" var="cheapestSubtitle"/>

                    <%-- Build cheapest carousel page nav hrefs --%>
                    <c:remove var="cheapestPrevHref"/>
                    <c:remove var="cheapestNextHref"/>
                    <c:if test="${cheapestCarsPage.hasPrevious}">
                        <c:url var="cheapestPrevHref" value="/">
                            <c:param name="cheapestPage" value="${cheapestCarsPage.currentPage - 1}"/>
                            <c:param name="recentPage"   value="${recentCarsPage.currentPage}"/>
                        </c:url>
                    </c:if>
                    <c:if test="${cheapestCarsPage.hasNext}">
                        <c:url var="cheapestNextHref" value="/">
                            <c:param name="cheapestPage" value="${cheapestCarsPage.currentPage + 1}"/>
                            <c:param name="recentPage"   value="${recentCarsPage.currentPage}"/>
                        </c:url>
                    </c:if>

                    <ryden:carouselSection cars="${cheapestCarsPage.content}" title="${cheapestTitle}"
                                         subtitle="${cheapestSubtitle}" id="cheapestVehiclesCarousel"
                                         prevPageHref="${cheapestPrevHref}" nextPageHref="${cheapestNextHref}"/>
                </section>

                <section class="carouselSection mt-5 pt-5 border-top border-secondary-subtle"
                            id="mostRecentVehiclesSection">
                    <spring:message code="home.recent.title" var="recentTitle"/>
                    <spring:message code="home.recent.subtitle" var="recentSubtitle"/>

                    <%-- Build recent carousel page nav hrefs --%>
                    <c:remove var="recentPrevHref"/>
                    <c:remove var="recentNextHref"/>
                    <c:if test="${recentCarsPage.hasPrevious}">
                        <c:url var="recentPrevHref" value="/">
                            <c:param name="cheapestPage" value="${cheapestCarsPage.currentPage}"/>
                            <c:param name="recentPage"   value="${recentCarsPage.currentPage - 1}"/>
                        </c:url>
                    </c:if>
                    <c:if test="${recentCarsPage.hasNext}">
                        <c:url var="recentNextHref" value="/">
                            <c:param name="cheapestPage" value="${cheapestCarsPage.currentPage}"/>
                            <c:param name="recentPage"   value="${recentCarsPage.currentPage + 1}"/>
                        </c:url>
                    </c:if>

                    <ryden:carouselSection cars="${recentCarsPage.content}" title="${recentTitle}"
                                subtitle="${recentSubtitle}" id="mostRecentVehiclesCarousel"
                                prevPageHref="${recentPrevHref}" nextPageHref="${recentNextHref}"/>
                </section>

                <section class="features-section mt-5 pt-5 pb-5 border-top border-secondary-subtle text-center" id="whyChooseUsSection">
                    <h2 class="mb-5 fw-semibold"><spring:message code="home.features.title"/></h2>
                    <div class="row g-4">
                        <div class="col-12 col-md-6 col-lg-3">
                            <div class="mb-3 d-inline-flex align-items-center justify-content-center rounded-circle" style="width: 72px; height: 72px; background-color: var(--color-primary-soft, #eef4ff); color: var(--color-primary, #3b7be0);">
                                <i class="bi bi-shield fs-2"></i>
                            </div>
                            <h5 class="fw-semibold fs-6"><spring:message code="home.features.safe.title"/></h5>
                            <p class="text-muted small px-3"><spring:message code="home.features.safe.desc"/></p>
                        </div>
                        <div class="col-12 col-md-6 col-lg-3">
                            <div class="mb-3 d-inline-flex align-items-center justify-content-center rounded-circle" style="width: 72px; height: 72px; background-color: var(--color-primary-soft, #eef4ff); color: var(--color-primary, #3b7be0);">
                                <i class="bi bi-cash-coin fs-2" aria-hidden="true"></i>
                            </div>
                            <h5 class="fw-semibold fs-6"><spring:message code="home.features.price.title"/></h5>
                            <p class="text-muted small px-3"><spring:message code="home.features.price.desc"/></p>
                        </div>
                        <div class="col-12 col-md-6 col-lg-3">
                            <div class="mb-3 d-inline-flex align-items-center justify-content-center rounded-circle" style="width: 72px; height: 72px; background-color: var(--color-primary-soft, #eef4ff); color: var(--color-primary, #3b7be0);">
                                <i class="bi bi-clock fs-2"></i>
                            </div>
                            <h5 class="fw-semibold fs-6"><spring:message code="home.features.hours.title"/></h5>
                            <p class="text-muted small px-3"><spring:message code="home.features.hours.desc"/></p>
                        </div>
                        <div class="col-12 col-md-6 col-lg-3">
                            <div class="mb-3 d-inline-flex align-items-center justify-content-center rounded-circle" style="width: 72px; height: 72px; background-color: var(--color-primary-soft, #eef4ff); color: var(--color-primary, #3b7be0);">
                                <i class="bi bi-star fs-2"></i>
                            </div>
                            <h5 class="fw-semibold fs-6"><spring:message code="home.features.premium.title"/></h5>
                            <p class="text-muted small px-3"><spring:message code="home.features.premium.desc"/></p>
                        </div>
                    </div>
                </section>
            </div>

            <section class="cta-banner text-center text-white py-5 w-100" style="background-color: var(--color-primary, #3b7be0);">
                <div class="container py-4 my-2">
                    <h2 class="fw-bold mb-3"><spring:message code="home.cta.title"/></h2>
                    <p class="lead mb-4" style="font-size: 1.1rem; opacity: 0.9;"><spring:message code="home.cta.desc"/></p>
                    <spring:message code="home.cta.button" var="ctaButtonText"/>
                    <ryden:button href="${pageContext.request.contextPath}/publish-car" text="${ctaButtonText}" size="lg" cssClass="btn-light text-primary fw-semibold px-4 rounded-3 shadow-sm" />
                </div>
            </section>
        </div>

        <%@ include file="footer.jsp" %>
    </body>

</html>
