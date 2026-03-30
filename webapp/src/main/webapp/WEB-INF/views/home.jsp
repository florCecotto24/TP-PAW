<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
 
<!DOCTYPE html>
<html lang="en">
    <head>
        <%@include file="header.jsp"%>
    </head>
    <body>
        <paw:navbar/>
        <div>
            <div class="hero-section w-100 sticky-top">
                <div class="hero-overlay">
                    <div class="hero-text text-center text-white mb-4">
                        <h1 class="fw-bold">Skip the rental car counter</h1>
                        <p class="lead">Rent just about any car, just about anywhere</p>
                    </div>
                    <paw:searchBar/>
                </div>
            </div>
            <div class="container mt-5">
                <section class="carouselSection" id="featuredVehiclesSection">
                    <paw:carouselHeader title="Featured Vehicles" subtitle="Discover our top picks for you" id="featuredCarsCarousel"/>
                    <div id="featuredCarsCarousel" class="carousel slide" data-bs-ride="false">
                        <div class="carousel-inner">
                            <div class="carousel-item active">
                                <div class="row row-cols-1 row-cols-md-2 row-cols-lg-4 g-3">
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
                                                     image="https://malevamag.com/wp-content/uploads/2019/12/JeepRenegade.jpg"/>
                                    </div>
                                    <div class="col d-flex justify-content-center">
                                        <paw:carCard brand="Volvo" model="S90" stars="4.7" price="108" reviews="18"
                                                     href="${pageContext.request.contextPath}/car-detail"
                                                     image="https://www.univision.com/_next/image?url=https%3A%2F%2Fst1.uvnimg.com%2Fa8%2F2a%2F6ae6bd4146438b12f17c878e1701%2Fhonda-civic-2016.jpg&w=1280&q=75"/>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </section>

                <section class="carouselSection mt-5 pt-5 border-top border-secondary-subtle" id="mostSearchedVehiclesSection">
                    <paw:carouselHeader title="Most Searched Vehicles" subtitle="Check out our most popular options" id="mostSearchedVehiclesCarousel"/>
                    <div id="mostSearchedVehiclesCarousel" class="carousel slide" data-bs-ride="false">
                        <div class="carousel-inner">
                            <div class="carousel-item active">
                                <div class="row row-cols-1 row-cols-md-2 row-cols-lg-4 g-3">
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
                                                     image="https://malevamag.com/wp-content/uploads/2019/12/JeepRenegade.jpg"/>
                                    </div>
                                    <div class="col d-flex justify-content-center">
                                        <paw:carCard brand="Volvo" model="S90" stars="4.7" price="108" reviews="18"
                                                     href="${pageContext.request.contextPath}/car-detail"
                                                     image="https://www.univision.com/_next/image?url=https%3A%2F%2Fst1.uvnimg.com%2Fa8%2F2a%2F6ae6bd4146438b12f17c878e1701%2Fhonda-civic-2016.jpg&w=1280&q=75"/>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </section>
            </div>
        </div>

        <%@ include file="footer.jsp"%>
    </body>
</html>