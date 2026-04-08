<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<!DOCTYPE html>
<html lang="en">

    <head>
        <title>Ryden - Home</title>
        <%@include file="header.jsp" %>
    </head>

    <body>
        <paw:navbar />
        <div>
            <div class="hero-section w-100">
                <div class="hero-overlay pb-2">
                    <div class="hero-text text-center text-white mb-4">
                        <h1 class="fw-bold"><spring:message code="home.hero.title"/></h1>
                        <p class="lead"><spring:message code="home.hero.subtitle"/></p>
                    </div>
                </div>
            </div>


            <div class="sticky-top w-100 pt-4 home-search-shell">
                <form method="get" action="${pageContext.request.contextPath}/search">
                    <paw:searchBar />
                </form>
            </div>


            <div class="container mt-5">
                <section class="carouselSection" id="cheapestVehiclesSection">
                    <spring:message code="home.cheapest.title" var="cheapestTitle"/>
                    <spring:message code="home.cheapest.subtitle" var="cheapestSubtitle"/>
                    <paw:carouselSection cars="${cheapestCars}" title="${cheapestTitle}"
                                         subtitle="${cheapestSubtitle}" id="cheapestVehiclesCarousel" />
                </section>

                <section class="carouselSection mt-5 pt-5 border-top border-secondary-subtle"
                            id="mostRecentVehiclesSection">
                    <spring:message code="home.recent.title" var="recentTitle"/>
                    <spring:message code="home.recent.subtitle" var="recentSubtitle"/>
                    <paw:carouselSection cars="${mostRecentCars}" title="${recentTitle}"
                                subtitle="${recentSubtitle}" id="mostRecentVehiclesCarousel" />
                </section>

                <section class="features-section mt-5 pt-5 pb-5 border-top border-secondary-subtle text-center" id="whyChooseUsSection">
                    <h2 class="mb-5 fw-semibold"><spring:message code="home.features.title"/></h2>
                    <div class="row g-4">
                        <div class="col-12 col-md-6 col-lg-3">
                            <div class="mb-3 d-inline-flex align-items-center justify-content-center rounded-circle home-feature-icon">
                                <i class="bi bi-shield fs-2"></i>
                            </div>
                            <h5 class="fw-semibold fs-6"><spring:message code="home.features.safe.title"/></h5>
                            <p class="text-muted small px-3"><spring:message code="home.features.safe.desc"/></p>
                        </div>
                        <div class="col-12 col-md-6 col-lg-3">
                            <div class="mb-3 d-inline-flex align-items-center justify-content-center rounded-circle home-feature-icon">
                                <i class="bi bi-currency-dollar fs-2"></i>
                            </div>
                            <h5 class="fw-semibold fs-6"><spring:message code="home.features.price.title"/></h5>
                            <p class="text-muted small px-3"><spring:message code="home.features.price.desc"/></p>
                        </div>
                        <div class="col-12 col-md-6 col-lg-3">
                            <div class="mb-3 d-inline-flex align-items-center justify-content-center rounded-circle home-feature-icon">
                                <i class="bi bi-clock fs-2"></i>
                            </div>
                            <h5 class="fw-semibold fs-6"><spring:message code="home.features.hours.title"/></h5>
                            <p class="text-muted small px-3"><spring:message code="home.features.hours.desc"/></p>
                        </div>
                        <div class="col-12 col-md-6 col-lg-3">
                            <div class="mb-3 d-inline-flex align-items-center justify-content-center rounded-circle home-feature-icon">
                                <i class="bi bi-star fs-2"></i>
                            </div>
                            <h5 class="fw-semibold fs-6"><spring:message code="home.features.premium.title"/></h5>
                            <p class="text-muted small px-3"><spring:message code="home.features.premium.desc"/></p>
                        </div>
                    </div>
                </section>
            </div>

            <section class="cta-banner home-cta-banner text-center text-white py-5 w-100">
                <div class="container py-4 my-2">
                    <h2 class="fw-bold mb-3"><spring:message code="home.cta.title"/></h2>
                    <p class="lead home-cta-copy"><spring:message code="home.cta.desc"/></p>
                    <spring:message code="home.cta.button" var="ctaButtonText"/>
                    <paw:button href="${pageContext.request.contextPath}/publish-car" text="${ctaButtonText}" type="light" size="lg" cssClass="text-primary shadow-sm home-cta-button" />
                </div>
            </section>
        </div>

        <%@ include file="footer.jsp" %>
    </body>

</html>
