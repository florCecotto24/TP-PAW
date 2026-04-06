<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<!DOCTYPE html>
<html lang="en">
    <head>
        <title><spring:message code="search.pageTitle"/></title>
        <%@include file="header.jsp"%>
    </head>
    <body class="has-fixed-navbar">
        <paw:navbar/>
        <div>
            <form id="exploreSearchForm" class="search-menu sticky-top w-100" method="get" action="${pageContext.request.contextPath}/search">
                <paw:searchBar/>
                <nav class="navbar navbar-expand-lg d-flex justify-content-center align-items-center border py-2 flex-wrap gap-1"
                     aria-label="Search filters">
                    <spring:message code="search.filter.category" var="categoryLabel"/>
                    <spring:message code="search.filter.category.helper" var="categoryHelper"/>
                    <paw:exploreFilterDropdown
                            filterLabel="${categoryLabel}"
                            helperText="${categoryHelper}"
                            paramName="category"
                            ariaGroup="category"
                            options="${categoryFilterOptions}"/>
                    <span class="vr d-none d-md-inline-block align-self-stretch my-2 mx-1"></span>
                    <spring:message code="search.filter.transmission" var="transmissionLabel"/>
                    <spring:message code="search.filter.transmission.helper" var="transmissionHelper"/>
                    <paw:exploreFilterDropdown
                            filterLabel="${transmissionLabel}"
                            helperText="${transmissionHelper}"
                            paramName="transmission"
                            ariaGroup="transmission"
                            options="${transmissionFilterOptions}"/>
                    <spring:message code="search.filter.powertrain" var="powertrainLabel"/>
                    <spring:message code="search.filter.powertrain.helper" var="powertrainHelper"/>
                    <paw:exploreFilterDropdown
                            filterLabel="${powertrainLabel}"
                            helperText="${powertrainHelper}"
                            paramName="powertrain"
                            ariaGroup="powertrain"
                            options="${powertrainFilterOptions}"/>
                    <span class="vr d-none d-md-inline-block align-self-stretch my-2 mx-1"></span>
                    <spring:message code="search.filter.price" var="priceLabel"/>
                    <spring:message code="search.filter.price.helper" var="priceHelper"/>
                    <paw:exploreFilterDropdown
                            filterLabel="${priceLabel}"
                            helperText="${priceHelper}"
                            paramName="price"
                            ariaGroup="price"
                            options="${priceFilterOptions}"/>
                </nav>
            </form>
            <div class="container">
                <div class="mb-6 pt-5">
                    <h4 class="font-semibold mb-1"><spring:message code="search.resultsCount" arguments="${fn:length(results)}"/></h4>
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
