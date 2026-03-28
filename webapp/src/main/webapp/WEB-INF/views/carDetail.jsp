<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <%@include file="header.jsp"%>
</head>
<body class="bg-light has-fixed-navbar">
<paw:navbar/>

<main class="car-detail-page container pb-4">
    <paw:breadcrumbTrail currentLabel="Mercedes-Benz E-Class 300"/>

    <div class="d-flex flex-column flex-lg-row justify-content-between align-items-start gap-3 mb-4">
        <div class="flex-grow-1 min-w-0">
            <h1 class="h2 fw-bold mb-0">Mercedes-Benz E-Class 300</h1>
            <paw:detailListingMeta rating="4.9" reviewCount="18" location="Córdoba, AR"/>
        </div>
        <paw:detailToolbarActions/>
    </div>

    <div class="row g-4 align-items-start">
        <div class="col-lg-8 order-1">
            <paw:carDetailGalleryGrid
                    modalId="carDetailGalleryModal"
                    mainImage="${pageContext.request.contextPath}/assets/images/mercedes-exterior.png"
                    topImage="${pageContext.request.contextPath}/assets/images/mercedes-interior.png"
                    bottomImage="${pageContext.request.contextPath}/assets/images/mercedes-rear-view.png"
                    mainAlt="Mercedes-Benz E-Class exterior"
                    topAlt="Interior dashboard"
                    bottomAlt="Rear view"/>

            <paw:hostProfileBar
                    hostName="Julian S."
                    avatarUrl="https://github.com/mdo.png"
                    responseHint="Responds in under 1 hour"/>

            <section class="mt-5">
                <h2 class="h5 fw-bold mb-3">Specifications</h2>
                <div class="row row-cols-2 row-cols-md-4 g-3">
                    <div class="col">
                        <paw:specCard icon="people-fill" label="5 seats"/>
                    </div>
                    <div class="col">
                        <paw:specCard icon="gear-wide-connected" label="Automatic"/>
                    </div>
                    <div class="col">
                        <paw:specCard icon="fuel-pump" label="Premium gasoline"/>
                    </div>
                    <div class="col">
                        <paw:specCard icon="snow" label="Central A/C"/>
                    </div>
                </div>
            </section>

            <paw:pickupLocationBlock
                    address="Av. Corrientes 2000"
                    mapImageSrc="${pageContext.request.contextPath}/assets/images/map-placeholder.svg"/>

            <section class="listingDescriptionSection mt-5" id="descriptionSection">
                <h2 class="h5 fw-bold mb-3">Description</h2>
                <p class="listingDescriptionSection__intro text-secondary mb-4">
                    Experience the luxury and power of the Mercedes-Benz E-Class 300. Ideal for business trips or special
                    weekend getaways in Córdoba. This vehicle combines cutting-edge technology with exceptional
                    comfort and an imposing presence on the road.
                </p>
                <div class="d-flex flex-column gap-3">
                    <paw:descriptionFeatureItem
                            title="Home delivery"
                            subtitle="The vehicle can be delivered at the airport or at your hotel."/>
                    <paw:descriptionFeatureItem
                            title="Premium insurance"
                            subtitle="Full coverage included in the reservation price."/>
                </div>
            </section>

            <section class="ratingsSection mt-5" id="ratingsSection">
                <h2 class="h6 fw-bold text-uppercase detailRatingsHeading mb-4 letter-spacing-tight">Ratings and reviews</h2>
                <div class="row g-3">
                    <div class="col-md-6">
                        <paw:reviewCard
                                avatarUrl="https://i.pravatar.cc/128?img=32"
                                userName="Sol Garcia"
                                fullStars="4"
                                halfStar="true"
                                quoteText="Excellent car and very good service. Everything was impeccable during the trip."/>
                    </div>
                    <div class="col-md-6">
                        <paw:reviewCard
                                avatarUrl="https://i.pravatar.cc/128?img=45"
                                userName="Julia Maiol"
                                fullStars="4"
                                halfStar="true"
                                quoteText="Very punctual and the car consumes very little. I recommend it without a doubt."/>
                    </div>
                </div>
            </section>
        </div>

        <div class="col-lg-4 order-2">
            <div class="detail-booking-sticky">
                <paw:detailBookingPanel
                        pricePerDay="120"
                        days="3"
                        subtotal="360"
                        serviceFee="24"
                        total="384"
                        deliveryLocation="Córdoba Airport"/>
            </div>
        </div>
    </div>

    <section class="similarVehiclesSection mt-5 pt-5 border-top border-secondary-subtle" id="similarVehiclesSection">
        <paw:similarVehiclesHeader/>
        <div class="row row-cols-1 row-cols-md-3 g-4">
            <div class="col d-flex justify-content-center">
                <paw:carCard brand="BMW" model="Series 5" stars="4.8" price="115" reviews="24"
                        href="${pageContext.request.contextPath}/car-detail"
                        image="https://www.bmw.com.mx/es/local/lista-de-precios-de-autos-bmw/_jcr_content/root/maincontent/contentblueprint_cop_245807772/contentblueprint_143/container/image.coreimg.png/1753708406694/bmw-3-series-ice-lci-modelfinder.png"/>
            </div>
            <div class="col d-flex justify-content-center">
                <paw:carCard brand="Audi" model="A6 S-Line" stars="5.0" price="125" reviews="30"
                        href="${pageContext.request.contextPath}/car-detail"
                        image="https://images.unsplash.com/photo-1606664515524-ed2f786a0bd6?auto=format&amp;fit=crop&amp;w=800&amp;q=80"/>
            </div>
            <div class="col d-flex justify-content-center">
                <paw:carCard brand="Volvo" model="S90" stars="4.7" price="108" reviews="18"
                        href="${pageContext.request.contextPath}/car-detail"
                        image="https://images.unsplash.com/photo-1619682817487-e45dc39de5d6?auto=format&amp;fit=crop&amp;w=800&amp;q=80"/>
            </div>
        </div>
    </section>
</main>

<paw:carDetailGalleryModal
        modalId="carDetailGalleryModal"
        carouselId="carDetailCarousel"
        mainImage="${pageContext.request.contextPath}/assets/images/mercedes-exterior.png"
        topImage="${pageContext.request.contextPath}/assets/images/mercedes-interior.png"
        bottomImage="${pageContext.request.contextPath}/assets/images/mercedes-rear-view.png"
        mainAlt="Mercedes-Benz E-Class exterior"
        topAlt="Interior dashboard"
        bottomAlt="Rear view">
</paw:carDetailGalleryModal>

<%@include file="footer.jsp"%>
<script src="<c:url value="/js/components.js"/>"></script>
</body>
</html>
