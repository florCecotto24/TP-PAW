<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html>
<html lang="en">
    <head>
        <title>Ryden - Explore</title>
        <%@include file="header.jsp"%>
    </head>
    <body class="has-fixed-navbar">
        <paw:navbar/>
        <div>
            <form id="exploreSearchForm" class="search-menu sticky-top w-100" method="get" action="${pageContext.request.contextPath}/search">
                <paw:searchBar/>
                <nav class="navbar navbar-expand-lg d-flex justify-content-center align-items-center border py-2 flex-wrap gap-1"
                     aria-label="Search filters">
                    <paw:exploreFilterDropdown
                            filterLabel="Category"
                            helperText="Body style from the listing (sedan, hatchback, SUV, etc.). Several choices are combined with OR."
                            paramName="category"
                            ariaGroup="category"
                            options="${categoryFilterOptions}"/>
                    <span class="vr d-none d-md-inline-block align-self-stretch my-2 mx-1"></span>
                    <paw:exploreFilterDropdown
                            filterLabel="Transmission"
                            helperText="Gearbox type. You can pick more than one (e.g. manual and automatic)."
                            paramName="transmission"
                            ariaGroup="transmission"
                            options="${transmissionFilterOptions}"/>
                    <paw:exploreFilterDropdown
                            filterLabel="Engine / fuel"
                            helperText="Engine or fuel type of the vehicle."
                            paramName="powertrain"
                            ariaGroup="powertrain"
                            options="${powertrainFilterOptions}"/>
                    <span class="vr d-none d-md-inline-block align-self-stretch my-2 mx-1"></span>
                    <paw:exploreFilterDropdown
                            filterLabel="Price"
                            helperText="Free: zero price per day. Paid: price greater than zero."
                            paramName="price"
                            ariaGroup="price"
                            options="${priceFilterOptions}"/>
                </nav>
            </form>
            <div class="container">
                <div class="mb-6 pt-5">
                    <h4 class="font-semibold mb-1">${fn:length(results)} cars available</h4>
                </div>

                <div class="text-center">
                    <div class="row row-cols-1 row-cols-md-2 row-cols-lg-4 pt-4 g-3">
                        <c:forEach var="car" items="${results}">
                            <div class="col d-flex justify-content-center">
                                <c:choose>
                                    <c:when test="${car.imageId > 0}">
                                        <c:url var="imageUrl" value="/image/${car.imageId}" />
                                    </c:when>
                                    <c:otherwise>
                                        <c:set var="imageUrl" value="" />
                                    </c:otherwise>
                                </c:choose>

                                <paw:carCard
                                        model="${car.model}"
                                        brand="${car.brand}"
                                        price="${car.price}"
                                        image="${imageUrl}"
                                        pricePeriod="day"
                                        href="${pageContext.request.contextPath}/car-detail?listingId=${car.listingId}"/>
                            </div>
                        </c:forEach>
                    </div>
                </div>
            </div>
        </div>
        <%@include file="footer.jsp"%>
        <script>
            (function () {
                var form = document.getElementById('exploreSearchForm');
                if (!form) return;
                form.querySelectorAll('.js-explore-filter').forEach(function (cb) {
                    cb.addEventListener('change', function () {
                        form.submit();
                    });
                });
            })();
        </script>
    </body>
</html>
