<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ attribute name="imageUrls" required="true" type="java.util.List" %>
<%@ attribute name="modalId" required="true" type="java.lang.String" %>
<%@ attribute name="vehicleLabel" required="false" type="java.lang.String" %>

<c:if test="${empty vehicleLabel}">
    <spring:message code="carDetailGalleryGrid.defaultVehicleLabel" var="defaultVehicleLabel"/>
    <c:set var="vehicleLabel" value="${defaultVehicleLabel}" />
</c:if>

<c:set var="imgCount" value="${fn:length(imageUrls)}" />

<c:choose>
    <c:when test="${empty imageUrls}">
        <div class="car-detail-gallery car-detail-gallery--empty rounded-4 overflow-hidden d-flex align-items-center justify-content-center bg-secondary-subtle" style="min-height: 280px;">
            <div class="text-center text-secondary px-3">
                <i class="bi bi-image fs-1 d-block mb-2" aria-hidden="true"></i>
                <p class="mb-0 small"><spring:message code="carDetailGalleryGrid.noPhotos"/></p>
            </div>
        </div>
    </c:when>
    <c:when test="${imgCount == 1}">
        <c:url var="mainSrc" value="${imageUrls[0]}" />
        <div class="car-detail-gallery car-detail-gallery--single rounded-4 overflow-hidden">
            <spring:message code="carDetailGalleryGrid.openGallery" arguments="${vehicleLabel}" var="openGalleryAria"/>
            <button type="button"
                    class="car-detail-gallery__cell car-detail-gallery__main btn p-0 border-0 bg-transparent w-100 h-100 text-start"
                    data-bs-toggle="modal"
                    data-bs-target="#${modalId}"
                    data-carousel-index="0"
                    aria-label="${openGalleryAria}">
                <img src="${mainSrc}" class="w-100 h-100 car-detail-gallery__img" alt="${vehicleLabel}">
            </button>
        </div>
    </c:when>
    <c:when test="${imgCount == 2}">
        <c:url var="mainSrc" value="${imageUrls[0]}" />
        <c:url var="topSrc" value="${imageUrls[1]}" />
        <div class="car-detail-gallery car-detail-gallery--pair rounded-4 overflow-hidden">
            <spring:message code="carDetailGalleryGrid.openGallery" arguments="${vehicleLabel}" var="openGalleryAria"/>
            <button type="button"
                    class="car-detail-gallery__cell car-detail-gallery__main btn p-0 border-0 bg-transparent w-100 h-100 text-start"
                    data-bs-toggle="modal"
                    data-bs-target="#${modalId}"
                    data-carousel-index="0"
                    aria-label="${openGalleryAria}">
                <img src="${mainSrc}" class="w-100 h-100 car-detail-gallery__img" alt="${vehicleLabel}">
            </button>
            <button type="button"
                    class="car-detail-gallery__cell car-detail-gallery__side btn p-0 border-0 bg-transparent w-100 h-100 text-start"
                    data-bs-toggle="modal"
                    data-bs-target="#${modalId}"
                    data-carousel-index="1"
                    aria-label="${openGalleryAria}">
                <img src="${topSrc}" class="w-100 h-100 car-detail-gallery__img" alt="${vehicleLabel}">
            </button>
        </div>
    </c:when>
    <c:otherwise>
        <c:url var="mainSrc" value="${imageUrls[0]}" />
        <c:url var="topSrc" value="${imageUrls[1]}" />
        <c:url var="bottomSrc" value="${imageUrls[2]}" />
        <c:set var="extra" value="${imgCount - 3}" />

        <div class="car-detail-gallery rounded-4 overflow-hidden">
            <spring:message code="carDetailGalleryGrid.openGallery" arguments="${vehicleLabel}" var="openGalleryAria"/>
            <button type="button"
                    class="car-detail-gallery__cell car-detail-gallery__main btn p-0 border-0 bg-transparent w-100 h-100 text-start"
                    data-bs-toggle="modal"
                    data-bs-target="#${modalId}"
                    data-carousel-index="0"
                    aria-label="${openGalleryAria}">
                <img src="${mainSrc}" class="w-100 h-100 car-detail-gallery__img" alt="${vehicleLabel}">
            </button>
            <button type="button"
                    class="car-detail-gallery__cell car-detail-gallery__side btn p-0 border-0 bg-transparent w-100 h-100 text-start"
                    data-bs-toggle="modal"
                    data-bs-target="#${modalId}"
                    data-carousel-index="1"
                    aria-label="${openGalleryAria}">
                <img src="${topSrc}" class="w-100 h-100 car-detail-gallery__img" alt="${vehicleLabel}">
            </button>
            <button type="button"
                    class="car-detail-gallery__cell car-detail-gallery__side btn p-0 border-0 bg-transparent w-100 h-100 text-start position-relative"
                    data-bs-toggle="modal"
                    data-bs-target="#${modalId}"
                    data-carousel-index="2"
                    aria-label="${openGalleryAria}">
                <img src="${bottomSrc}" class="w-100 h-100 car-detail-gallery__img" alt="${vehicleLabel}">
                <c:if test="${extra > 0}">
                    <div class="car-detail-gallery__more-overlay" aria-hidden="true">+<c:out value="${extra}"/></div>
                </c:if>
            </button>
        </div>
    </c:otherwise>
</c:choose>
