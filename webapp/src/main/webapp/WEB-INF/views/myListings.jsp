<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<!DOCTYPE html>
<html lang="en">
    <head>
        <title><spring:message code="myListings.pageTitle"/></title>
        <%@include file="header.jsp"%>
    </head>
    <body class="has-fixed-navbar">
        <ryden:navbar/>
        <div class="container pt-5 pb-4">
            <spring:message code="myListings.heading" var="myListingsLabel"/>
            <ryden:breadcrumbTrail currentLabel="${myListingsLabel}"/>
            <section class="reservation-management-header mt-4 pt-5 mb-4">
                <h1 class="h3 fw-bold mb-2"><spring:message code="myListings.heading"/></h1>
                <p class="text-secondary mb-0"><spring:message code="myListings.subheading"/></p>
            </section>

            <c:set var="hasActiveFilters" value="${not empty param.q or not empty param.listingStatus}"/>

            <c:url var="myListingsBaseUrl" value="/my-listings">
                <c:if test="${not empty param.q}">
                    <c:param name="q"><c:out value="${param.q}"/></c:param>
                </c:if>
                <c:if test="${not empty ownerListingStatusFilter}">
                    <c:param name="listingStatus"><c:out value="${ownerListingStatusFilter}"/></c:param>
                </c:if>
            </c:url>

            <c:if test="${not empty results or hasActiveFilters}">
                <spring:message code="validation.dropdown.invalid" var="myListingsDropdownInvalid" htmlEscape="true"/>
                <form id="myListingsFilterForm" class="row g-2 align-items-end mb-4" method="get" action="${pageContext.request.contextPath}/my-listings"
                      data-ryden-dropdown-invalid="<c:out value='${myListingsDropdownInvalid}'/>">
                    <div class="col-md-5 col-lg-4">
                        <label class="form-label small text-secondary mb-1" for="myListings_q"><spring:message code="myListings.filter.query"/></label>
                        <input type="search" class="form-control" id="myListings_q" name="q" value="<c:out value='${param.q}'/>"
                               placeholder="<spring:message code='myListings.filter.query.placeholder'/>"/>
                    </div>
                    <div class="col-md-4 col-lg-3">
                        <label class="form-label small text-secondary mb-1" for="myListings_status"><spring:message code="myListings.filter.status"/></label>
                        <select class="form-select" id="myListings_status" name="listingStatus">
                            <option value="" ${empty ownerListingStatusFilter ? 'selected="selected"' : ''}><spring:message code="myListings.filter.status.any"/></option>
                            <option value="active" ${ownerListingStatusFilter eq 'active' ? 'selected="selected"' : ''}><spring:message code="enum.listing.status.ACTIVE"/></option>
                            <option value="paused" ${ownerListingStatusFilter eq 'paused' ? 'selected="selected"' : ''}><spring:message code="enum.listing.status.PAUSED"/></option>
                            <option value="finished" ${ownerListingStatusFilter eq 'finished' ? 'selected="selected"' : ''}><spring:message code="enum.listing.status.FINISHED"/></option>
                        </select>
                    </div>
                    <div class="col-auto d-flex flex-wrap gap-2">
                        <button type="submit" class="btn btn-primary"><spring:message code="myListings.filter.search"/></button>
                        <a href="${pageContext.request.contextPath}/my-listings" class="btn btn-outline-secondary"><spring:message code="search.filters.clear"/></a>
                    </div>
                </form>

                <div class="mb-3 d-flex flex-wrap align-items-center justify-content-between gap-2">
                    <h2 class="h5 mb-0">
                        <c:choose>
                            <c:when test="${myListingsPage.totalItems > 0}">
                                <spring:message code="myListings.resultsRange"
                                                arguments="${myListingsPage.firstItemNumber},${myListingsPage.lastItemNumber},${myListingsPage.totalItems}"/>
                            </c:when>
                            <c:otherwise>
                                <spring:message code="myListings.resultsCount" arguments="0"/>
                            </c:otherwise>
                        </c:choose>
                    </h2>
                </div>
            </c:if>

            <c:choose>
                <c:when test="${empty results}">
                    <div class="search-empty-state text-center">
                        <h2 class="h4 fw-semibold mb-2">
                            <spring:message code="myListings.noResults.title"/>
                        </h2>
                        <div class="search-empty-state__actions mt-4">
                            <a href="${pageContext.request.contextPath}/my-listings" class="btn btn-outline-secondary">
                                <spring:message code="search.filters.clear"/>
                            </a>
                        </div>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="d-flex flex-column gap-3">
                        <c:forEach var="car" items="${results}">
                            <c:url var="listingDetailUrl" value="/my-listings/${car.listingId}"/>
                            <a href="<c:out value='${listingDetailUrl}'/>" class="reservation-card text-decoration-none text-reset">
                                <article class="card border-0 shadow-sm rounded-4 overflow-hidden reservation-card__surface position-relative">
                                    <c:if test="${not empty car.statusKey}">
                                        <c:choose>
                                            <c:when test="${car.statusKey == 'ACTIVE'}">
                                                <span class="position-absolute top-0 end-0 m-3" style="background-color:#198754; color:#ffffff; padding:.25rem .5rem; border-radius:.375rem; font-weight:600; font-size:.75rem;">
                                                    <spring:message code="enum.listing.status.ACTIVE"/>
                                                </span>
                                            </c:when>
                                            <c:when test="${car.statusKey == 'PAUSED'}">
                                                <span class="position-absolute top-0 end-0 m-3" style="background-color:#e4960b; color:#ffffff; padding:.25rem .5rem; border-radius:.375rem; font-weight:600; font-size:.75rem;">
                                                    <spring:message code="enum.listing.status.PAUSED"/>
                                                </span>
                                            </c:when>
                                            <c:when test="${car.statusKey == 'FINISHED'}">
                                                <span class="position-absolute top-0 end-0 m-3" style="background-color:#6c757d; color:#ffffff; padding:.25rem .5rem; border-radius:.375rem; font-weight:600; font-size:.75rem;">
                                                    <spring:message code="enum.listing.status.FINISHED"/>
                                                </span>
                                            </c:when>
                                        </c:choose>
                                    </c:if>
                                    <div class="row g-0 align-items-stretch">
                                        <div class="col-12 col-md-3 reservation-card__media-wrap">
                                            <c:choose>
                                                <c:when test="${car.imageId > 0}">
                                                    <c:url var="listingImgUrl" value="/image/${car.imageId}"/>
                                                    <img src="<c:out value='${listingImgUrl}'/>" alt="<c:out value='${car.brand} ${car.model}'/>" class="reservation-card__media">
                                                </c:when>
                                                <c:otherwise>
                                                    <div class="reservation-card__media reservation-card__media--placeholder d-flex align-items-center justify-content-center text-secondary">
                                                        <i class="bi bi-car-front fs-1" aria-hidden="true"></i>
                                                    </div>
                                                </c:otherwise>
                                            </c:choose>
                                        </div>
                                        <div class="col-12 col-md-9">
                                            <div class="card-body p-3 p-md-4 h-100 d-flex flex-column justify-content-between gap-3">
                                                <div>
                                                    <h3 class="h5 fw-semibold mb-1"><c:out value="${car.brand} ${car.model}"/></h3>
                                                </div>
                                                <div class="pt-1 d-flex align-items-center justify-content-between gap-2 flex-wrap">
                                                    <div class="reservation-price-compact">
                                                        <span class="reservation-card__meta-label mb-0"><spring:message code="myListings.card.pricePerDay"/></span>
                                                        <span class="h5 mb-0 fw-bold text-primary">$<c:out value="${car.price}"/></span>
                                                    </div>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </article>
                            </a>
                        </c:forEach>
                    </div>

                    <ryden:pagination
                            currentPage="${myListingsPage.currentPage}"
                            totalPages="${myListingsPage.totalPages}"
                            baseUrl="${myListingsBaseUrl}"/>
                </c:otherwise>
            </c:choose>
        </div>
        <script>
            (function () {
                var form = document.getElementById('myListingsFilterForm');
                var sel = document.getElementById('myListings_status');
                if (!form || !sel) return;
                var allowed = ['', 'active', 'paused', 'finished'];
                var msg = form.getAttribute('data-ryden-dropdown-invalid') || '';
                form.addEventListener('submit', function (ev) {
                    var v = (sel.value || '').trim().toLowerCase();
                    if (allowed.indexOf(v) < 0) {
                        ev.preventDefault();
                        sel.setCustomValidity(msg);
                        sel.reportValidity();
                        return;
                    }
                    sel.setCustomValidity('');
                });
            })();
        </script>
        <%@include file="footer.jsp"%>
    </body>
</html>

