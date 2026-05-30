<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ attribute name="mediaItems" required="true" type="java.util.List" %>
<%@ attribute name="modalId" required="true" type="java.lang.String" %>
<%@ attribute name="vehicleLabel" required="false" type="java.lang.String" %>

<c:if test="${empty vehicleLabel}">
    <spring:message code="carDetailGalleryGrid.defaultVehicleLabel" var="defaultVehicleLabel"/>
    <c:set var="vehicleLabel" value="${defaultVehicleLabel}" />
</c:if>

<c:set var="mediaCount" value="${fn:length(mediaItems)}" />

<c:choose>
    <c:when test="${empty mediaItems}">
        <div class="car-detail-gallery car-detail-gallery--empty rounded-4 overflow-hidden d-flex align-items-center justify-content-center bg-secondary-subtle" style="min-height: 280px;">
            <div class="text-center text-secondary px-3">
                <i class="bi bi-image fs-1 d-block mb-2" aria-hidden="true"></i>
                <p class="mb-0 small"><spring:message code="carDetailGalleryGrid.noPhotos"/></p>
            </div>
        </div>
    </c:when>
    <c:otherwise>
        <spring:message code="carDetailGalleryGrid.openGallery" arguments="${vehicleLabel}" var="openGalleryAria"/>
        <c:choose>
            <c:when test="${mediaCount == 1}">
                <c:url var="mainSrc" value="${mediaItems[0].url}" />
                <div class="car-detail-gallery car-detail-gallery--single rounded-4 overflow-hidden">
                    <button type="button"
                            class="car-detail-gallery__cell car-detail-gallery__main btn p-0 border-0 bg-transparent w-100 h-100 text-start position-relative"
                            data-bs-toggle="modal"
                            data-bs-target="#<c:out value='${modalId}'/>"
                            data-carousel-index="0"
                            aria-label="<c:out value='${openGalleryAria}'/>">
                        <c:choose>
                            <c:when test="${mediaItems[0].video}">
                                <video src="<c:out value='${mainSrc}'/>" class="w-100 h-100 car-detail-gallery__video" muted playsinline preload="metadata"></video>
                                <span class="car-detail-gallery__play-overlay" aria-hidden="true"><i class="bi bi-play-circle"></i></span>
                            </c:when>
                            <c:otherwise>
                                <img src="<c:out value='${mainSrc}'/>" class="w-100 h-100 car-detail-gallery__img" alt="<c:out value='${vehicleLabel}'/>">
                            </c:otherwise>
                        </c:choose>
                    </button>
                </div>
            </c:when>
            <c:when test="${mediaCount == 2}">
                <c:url var="mainSrc" value="${mediaItems[0].url}" />
                <c:url var="topSrc" value="${mediaItems[1].url}" />
                <div class="car-detail-gallery car-detail-gallery--pair rounded-4 overflow-hidden">
                    <button type="button"
                            class="car-detail-gallery__cell car-detail-gallery__main btn p-0 border-0 bg-transparent w-100 h-100 text-start position-relative"
                            data-bs-toggle="modal"
                            data-bs-target="#<c:out value='${modalId}'/>"
                            data-carousel-index="0"
                            aria-label="<c:out value='${openGalleryAria}'/>">
                        <c:choose>
                            <c:when test="${mediaItems[0].video}">
                                <video src="<c:out value='${mainSrc}'/>" class="w-100 h-100 car-detail-gallery__video" muted playsinline preload="metadata"></video>
                                <span class="car-detail-gallery__play-overlay" aria-hidden="true"><i class="bi bi-play-circle"></i></span>
                            </c:when>
                            <c:otherwise>
                                <img src="<c:out value='${mainSrc}'/>" class="w-100 h-100 car-detail-gallery__img" alt="<c:out value='${vehicleLabel}'/>">
                            </c:otherwise>
                        </c:choose>
                    </button>
                    <button type="button"
                            class="car-detail-gallery__cell car-detail-gallery__side btn p-0 border-0 bg-transparent w-100 h-100 text-start position-relative"
                            data-bs-toggle="modal"
                            data-bs-target="#<c:out value='${modalId}'/>"
                            data-carousel-index="1"
                            aria-label="<c:out value='${openGalleryAria}'/>">
                        <c:choose>
                            <c:when test="${mediaItems[1].video}">
                                <video src="<c:out value='${topSrc}'/>" class="w-100 h-100 car-detail-gallery__video" muted playsinline preload="metadata"></video>
                                <span class="car-detail-gallery__play-overlay" aria-hidden="true"><i class="bi bi-play-circle"></i></span>
                            </c:when>
                            <c:otherwise>
                                <img src="<c:out value='${topSrc}'/>" class="w-100 h-100 car-detail-gallery__img" alt="<c:out value='${vehicleLabel}'/>">
                            </c:otherwise>
                        </c:choose>
                    </button>
                </div>
            </c:when>
            <c:otherwise>
                <c:url var="mainSrc" value="${mediaItems[0].url}" />
                <c:url var="topSrc" value="${mediaItems[1].url}" />
                <c:url var="bottomSrc" value="${mediaItems[2].url}" />
                <c:set var="extra" value="${mediaCount - 3}" />

                <div class="car-detail-gallery rounded-4 overflow-hidden">
                    <button type="button"
                            class="car-detail-gallery__cell car-detail-gallery__main btn p-0 border-0 bg-transparent w-100 h-100 text-start position-relative"
                            data-bs-toggle="modal"
                            data-bs-target="#<c:out value='${modalId}'/>"
                            data-carousel-index="0"
                            aria-label="<c:out value='${openGalleryAria}'/>">
                        <c:choose>
                            <c:when test="${mediaItems[0].video}">
                                <video src="<c:out value='${mainSrc}'/>" class="w-100 h-100 car-detail-gallery__video" muted playsinline preload="metadata"></video>
                                <span class="car-detail-gallery__play-overlay" aria-hidden="true"><i class="bi bi-play-circle"></i></span>
                            </c:when>
                            <c:otherwise>
                                <img src="<c:out value='${mainSrc}'/>" class="w-100 h-100 car-detail-gallery__img" alt="<c:out value='${vehicleLabel}'/>">
                            </c:otherwise>
                        </c:choose>
                    </button>
                    <button type="button"
                            class="car-detail-gallery__cell car-detail-gallery__side btn p-0 border-0 bg-transparent w-100 h-100 text-start position-relative"
                            data-bs-toggle="modal"
                            data-bs-target="#<c:out value='${modalId}'/>"
                            data-carousel-index="1"
                            aria-label="<c:out value='${openGalleryAria}'/>">
                        <c:choose>
                            <c:when test="${mediaItems[1].video}">
                                <video src="<c:out value='${topSrc}'/>" class="w-100 h-100 car-detail-gallery__video" muted playsinline preload="metadata"></video>
                                <span class="car-detail-gallery__play-overlay" aria-hidden="true"><i class="bi bi-play-circle"></i></span>
                            </c:when>
                            <c:otherwise>
                                <img src="<c:out value='${topSrc}'/>" class="w-100 h-100 car-detail-gallery__img" alt="<c:out value='${vehicleLabel}'/>">
                            </c:otherwise>
                        </c:choose>
                    </button>
                    <button type="button"
                            class="car-detail-gallery__cell car-detail-gallery__side btn p-0 border-0 bg-transparent w-100 h-100 text-start position-relative"
                            data-bs-toggle="modal"
                            data-bs-target="#<c:out value='${modalId}'/>"
                            data-carousel-index="2"
                            aria-label="<c:out value='${openGalleryAria}'/>">
                        <c:choose>
                            <c:when test="${mediaItems[2].video}">
                                <video src="<c:out value='${bottomSrc}'/>" class="w-100 h-100 car-detail-gallery__video" muted playsinline preload="metadata"></video>
                                <span class="car-detail-gallery__play-overlay" aria-hidden="true"><i class="bi bi-play-circle"></i></span>
                            </c:when>
                            <c:otherwise>
                                <img src="<c:out value='${bottomSrc}'/>" class="w-100 h-100 car-detail-gallery__img" alt="<c:out value='${vehicleLabel}'/>">
                            </c:otherwise>
                        </c:choose>
                        <c:if test="${extra > 0}">
                            <div class="car-detail-gallery__more-overlay" aria-hidden="true">+<c:out value="${extra}"/></div>
                        </c:if>
                    </button>
                </div>
            </c:otherwise>
        </c:choose>
    </c:otherwise>
</c:choose>
