<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
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
                        <c:forEach var="car" items="${results}">
                            <div class="col d-flex justify-content-center">
                            <paw:carCard model="${car.model}" brand="${car.brand}" stars="4.5" price="2000000" image="https://www.buyatoyota.com/sharpr/bat/assets/img/vehicle-info/Corolla/2026/hero-image.png" reviews="5" href="${pageContext.request.contextPath}/car-detail"/>
                            </div>
                        </c:forEach>
                    </div>
                </div>
            </div>
        </div>
        <%@include file="footer.jsp"%>
    </body>
</html>
