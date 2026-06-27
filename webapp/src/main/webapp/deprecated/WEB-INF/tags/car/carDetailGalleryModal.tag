<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ attribute name="modalId" required="true" type="java.lang.String" %>
<%@ attribute name="carouselId" required="true" type="java.lang.String" %>
<%@ attribute name="mediaItems" required="true" type="java.util.List" %>
<%@ attribute name="vehicleLabel" required="false" type="java.lang.String" %>

<c:if test="${empty vehicleLabel}">
    <spring:message code="carDetailGalleryModal.defaultVehicleLabel" var="defaultVehicleLabel"/>
    <c:set var="vehicleLabel" value="${defaultVehicleLabel}" />
</c:if>

<div class="modal fade car-detail-gallery-modal" id="<c:out value='${modalId}'/>" tabindex="-1" aria-hidden="true" aria-labelledby="<c:out value='${modalId}'/>Title">
    <div class="modal-dialog modal-dialog-centered modal-xl">
        <div class="modal-content bg-dark text-white border-0">
            <div class="modal-header border-0">
                <h2 class="modal-title fs-6" id="<c:out value='${modalId}'/>Title"><spring:message code="carDetailGalleryModal.title"/></h2>
                <spring:message code="common.close" var="closeLabel"/>
                <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="<c:out value='${closeLabel}'/>"></button>
            </div>
            <div class="modal-body p-0">
                <div id="<c:out value='${carouselId}'/>" class="carousel slide carousel-fade" data-bs-ride="false">
                    <div class="carousel-inner">
                        <c:forEach items="${mediaItems}" var="item" varStatus="st">
                            <c:url var="slideSrc" value="${item.url}" />
                            <div class="carousel-item<c:if test="${st.first}"> active</c:if>">
                                <div class="car-detail-carousel-frame">
                                    <c:choose>
                                        <c:when test="${item.video}">
                                            <spring:message code="carDetailGalleryModal.videoAlt" arguments="${vehicleLabel}, ${st.count}" var="videoAlt"/>
                                            <video src="<c:out value='${slideSrc}'/>"
                                                   class="car-detail-carousel-backdrop"
                                                   muted
                                                   playsinline
                                                   preload="metadata"
                                                   tabindex="-1"
                                                   aria-hidden="true"></video>
                                            <video src="<c:out value='${slideSrc}'/>"
                                                   class="car-detail-carousel-video"
                                                   controls playsinline preload="metadata"
                                                   aria-label="<c:out value='${videoAlt}'/>">
                                                <source src="<c:out value='${slideSrc}'/>" type="<c:out value='${item.contentType}'/>"/>
                                            </video>
                                        </c:when>
                                        <c:otherwise>
                                            <spring:message code="carDetailGalleryModal.photoAlt" arguments="${vehicleLabel}, ${st.count}" var="photoAlt"/>
                                            <img src="<c:out value='${slideSrc}'/>"
                                                 class="car-detail-carousel-backdrop"
                                                 alt=""
                                                 aria-hidden="true">
                                            <img src="<c:out value='${slideSrc}'/>"
                                                 class="car-detail-carousel-img"
                                                 alt="<c:out value='${photoAlt}'/>">
                                        </c:otherwise>
                                    </c:choose>
                                </div>
                            </div>
                        </c:forEach>
                    </div>
                    <c:if test="${not empty mediaItems}">
                        <button class="carousel-control-prev" type="button" data-bs-target="#<c:out value='${carouselId}'/>" data-bs-slide="prev">
                            <span class="carousel-control-prev-icon" aria-hidden="true"></span>
                            <span class="visually-hidden"><spring:message code="common.previous"/></span>
                        </button>
                        <button class="carousel-control-next" type="button" data-bs-target="#<c:out value='${carouselId}'/>" data-bs-slide="next">
                            <span class="carousel-control-next-icon" aria-hidden="true"></span>
                            <span class="visually-hidden"><spring:message code="common.next"/></span>
                        </button>
                    </c:if>
                </div>
            </div>
        </div>
    </div>
</div>
