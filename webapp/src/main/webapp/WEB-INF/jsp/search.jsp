<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<!DOCTYPE html>
<html>
    <head>
        <%@include file="header.jsp"%>
    </head>
    <body>
        <paw:navbar/>
        <div>
            <div class="search-menu sticky-top w-100">
                <paw:searchBar/>
                <nav class="navbar navbar-expand-lg d-flex justify-content-center border py-2">
                    <paw:filterButton label="Transmission" options="${categories}"/>
                    <paw:filterButton label="Prices" options="${prices}"/>
                    <paw:filterButton label="Ratings" options="${ratings}"/>
                </nav>
            </div>
            <div class="container mt-5">
                <%-- Header --%>
                <div class="mb-6 pt-5">
                    <h4 class="font-semibold mb-1">9 cars available</h4>
                </div>

                <%-- Results Grid --%>
                <div class="text-center">
                    <div class="row row-cols-1 row-cols-md-2 row-cols-lg-4 pt-4 g-3">
                        <div class="col d-flex justify-content-center">
                            <paw:carCard model="Yaris" brand="Toyota" stars="4.5" price="2000000" image="https://www.buyatoyota.com/sharpr/bat/assets/img/vehicle-info/Corolla/2026/hero-image.png" reviews="5"/>
                        </div>
                        <div class="col d-flex justify-content-center">
                            <paw:carCard model="Yaris" brand="Toyota" stars="4.5" price="2000000" image="https://www.bmw.com.mx/es/local/lista-de-precios-de-autos-bmw/_jcr_content/root/maincontent/contentblueprint_cop_245807772/contentblueprint_143/container/image.coreimg.png/1753708406694/bmw-3-series-ice-lci-modelfinder.png" reviews="5"/>
                        </div>
                        <div class="col d-flex justify-content-center">
                            <paw:carCard model="Yaris" brand="Toyota" stars="4.5" price="2000000" image="" reviews="5"/>
                        </div>
                        <div class="col d-flex justify-content-center">
                            <paw:carCard model="Yaris" brand="Toyota" stars="4.5" price="2000000" image="" reviews="5"/>
                        </div>
                        <div class="col d-flex justify-content-center">
                            <paw:carCard model="Yaris" brand="Toyota" stars="4.5" price="2000000" image="https://www.buyatoyota.com/sharpr/bat/assets/img/vehicle-info/Corolla/2026/hero-image.png" reviews="5"/>
                        </div>
                        <div class="col d-flex justify-content-center">
                            <paw:carCard model="Yaris" brand="Toyota" stars="4.5" price="2000000" image="https://www.bmw.com.mx/es/local/lista-de-precios-de-autos-bmw/_jcr_content/root/maincontent/contentblueprint_cop_245807772/contentblueprint_143/container/image.coreimg.png/1753708406694/bmw-3-series-ice-lci-modelfinder.png" reviews="5"/>
                        </div>
                        <div class="col d-flex justify-content-center">
                            <paw:carCard model="Yaris" brand="Toyota" stars="4.5" price="2000000" image="" reviews="5"/>
                        </div>
                        <div class="col d-flex justify-content-center">
                            <paw:carCard model="Yaris" brand="Toyota" stars="4.5" price="2000000" image="" reviews="5"/>
                        </div>
                        <div class="col d-flex justify-content-center">
                            <paw:carCard model="Yaris" brand="Toyota" stars="4.5" price="2000000" image="https://www.bmw.com.mx/es/local/lista-de-precios-de-autos-bmw/_jcr_content/root/maincontent/contentblueprint_cop_245807772/contentblueprint_143/container/image.coreimg.png/1753708406694/bmw-3-series-ice-lci-modelfinder.png" reviews="5"/>
                        </div>
                        <div class="col d-flex justify-content-center">
                            <paw:carCard model="Yaris" brand="Toyota" stars="4.5" price="2000000" image="" reviews="5"/>
                        </div>
                        <div class="col d-flex justify-content-center">
                            <paw:carCard model="Yaris" brand="Toyota" stars="4.5" price="2000000" image="" reviews="5"/>
                        </div>
                    </div>
                </div>
            </div>
        </div>
        <%@include file="footer.jsp"%>
    </body>
</html>
