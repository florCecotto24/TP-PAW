<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>

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
                        <h1 class="fw-bold">Skip the rental car counter</h1>
                        <p class="lead">Rent just about any car, just about anywhere</p>
                    </div>
                </div>
            </div>


            <div class="sticky-top w-100 pt-4" style="top: 55px; z-index: 1020; background-color: #f9fbff;">
                <form method="get" action="${pageContext.request.contextPath}/search">
                    <paw:searchBar />
                </form>
            </div>


            <div class="container mt-5">
                <section class="carouselSection" id="cheapestVehiclesSection">
                    <paw:carouselSection cars="${cheapestCars}" title="Cheapest Vehicles"
                                         subtitle="Discover our cheapest options" id="cheapestVehiclesCarousel" />
                </section>

                <section class="carouselSection mt-5 pt-5 border-top border-secondary-subtle"
                            id="mostRecentVehiclesSection">
                    <paw:carouselSection cars="${mostRecentCars}" title="Most Recent Vehicles"
                                subtitle="Discover our most recent options" id="mostRecentVehiclesCarousel" />
                </section>

                <section class="features-section mt-5 pt-5 pb-5 border-top border-secondary-subtle text-center" id="whyChooseUsSection">
                    <h2 class="mb-5 fw-semibold">¿Por qué elegir Ryden?</h2>
                    <div class="row g-4">
                        <div class="col-12 col-md-6 col-lg-3">
                            <div class="mb-3 d-inline-flex align-items-center justify-content-center rounded-circle" style="width: 72px; height: 72px; background-color: var(--color-primary-soft, #eef4ff); color: var(--color-primary, #3b7be0);">
                                <i class="bi bi-shield fs-2"></i>
                            </div>
                            <h5 class="fw-semibold fs-6">Seguro y confiable</h5>
                            <p class="text-muted small px-3">Verificamos todos los vehículos y conductores</p>
                        </div>
                        <div class="col-12 col-md-6 col-lg-3">
                            <div class="mb-3 d-inline-flex align-items-center justify-content-center rounded-circle" style="width: 72px; height: 72px; background-color: var(--color-primary-soft, #eef4ff); color: var(--color-primary, #3b7be0);">
                                <i class="bi bi-currency-dollar fs-2"></i>
                            </div>
                            <h5 class="fw-semibold fs-6">Precios justos</h5>
                            <p class="text-muted small px-3">Sin comisiones ocultas, tarifas transparentes</p>
                        </div>
                        <div class="col-12 col-md-6 col-lg-3">
                            <div class="mb-3 d-inline-flex align-items-center justify-content-center rounded-circle" style="width: 72px; height: 72px; background-color: var(--color-primary-soft, #eef4ff); color: var(--color-primary, #3b7be0);">
                                <i class="bi bi-clock fs-2"></i>
                            </div>
                            <h5 class="fw-semibold fs-6">Disponibilidad 24/7</h5>
                            <p class="text-muted small px-3">Reservá cuando quieras, donde quieras</p>
                        </div>
                        <div class="col-12 col-md-6 col-lg-3">
                            <div class="mb-3 d-inline-flex align-items-center justify-content-center rounded-circle" style="width: 72px; height: 72px; background-color: var(--color-primary-soft, #eef4ff); color: var(--color-primary, #3b7be0);">
                                <i class="bi bi-star fs-2"></i>
                            </div>
                            <h5 class="fw-semibold fs-6">Experiencia premium</h5>
                            <p class="text-muted small px-3">Atención personalizada en cada paso</p>
                        </div>
                    </div>
                </section>
            </div>

            <section class="cta-banner text-center text-white py-5 w-100" style="background-color: var(--color-primary, #3b7be0);">
                <div class="container py-4 my-2">
                    <h2 class="fw-bold mb-3">¿Tenés un auto que no usás?</h2>
                    <p class="lead mb-4" style="font-size: 1.1rem; opacity: 0.9;">Convertilo en una fuente de ingresos. Publicá tu vehículo y empezá a ganar hoy.</p>
                    <paw:button href="${pageContext.request.contextPath}/publish-car" text="Publicar mi auto" size="lg" cssClass="btn-light text-primary fw-semibold px-4 rounded-3 shadow-sm" />
                </div>
            </section>
        </div>

        <%@ include file="footer.jsp" %>
    </body>

</html>