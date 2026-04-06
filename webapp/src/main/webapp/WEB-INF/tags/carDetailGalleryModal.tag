<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ attribute name="modalId" required="true" type="java.lang.String" %>
<%@ attribute name="carouselId" required="true" type="java.lang.String" %>
<%@ attribute name="imageUrls" required="true" type="java.util.List" %>
<%@ attribute name="vehicleLabel" required="false" type="java.lang.String" %>

<c:if test="${empty vehicleLabel}">
    <spring:message code="carDetailGalleryModal.defaultVehicleLabel" var="defaultVehicleLabel"/>
    <c:set var="vehicleLabel" value="${defaultVehicleLabel}" />
</c:if>

<div class="modal fade" id="${modalId}" tabindex="-1" aria-hidden="true" aria-labelledby="${modalId}Title">
    <div class="modal-dialog modal-dialog-centered modal-xl">
        <div class="modal-content bg-dark text-white border-0">
            <div class="modal-header border-0">
                <h2 class="modal-title fs-6" id="${modalId}Title"><spring:message code="carDetailGalleryModal.title"/></h2>
                <spring:message code="common.close" var="closeLabel"/>
                <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="${closeLabel}"></button>
            </div>
            <div class="modal-body p-0">
                <div id="${carouselId}" class="carousel slide" data-bs-ride="false">
                    <div class="carousel-inner">
                        <c:forEach items="${imageUrls}" var="u" varStatus="st">
                            <c:url var="slideSrc" value="${u}" />
                            <div class="carousel-item<c:if test="${st.first}"> active</c:if>">
                                <spring:message code="carDetailGalleryModal.photoAlt" arguments="${vehicleLabel}, ${st.count}" var="photoAlt"/>
                                <img src="${slideSrc}" class="d-block w-100 car-detail-carousel-img" alt="${photoAlt}">
                            </div>
                        </c:forEach>
                    </div>
                    <c:if test="${not empty imageUrls}">
                        <button class="carousel-control-prev" type="button" data-bs-target="#${carouselId}" data-bs-slide="prev">
                            <span class="carousel-control-prev-icon" aria-hidden="true"></span>
                            <span class="visually-hidden"><spring:message code="common.previous"/></span>
                        </button>
                        <button class="carousel-control-next" type="button" data-bs-target="#${carouselId}" data-bs-slide="next">
                            <span class="carousel-control-next-icon" aria-hidden="true"></span>
                            <span class="visually-hidden"><spring:message code="common.next"/></span>
                        </button>
                    </c:if>
                </div>
            </div>
        </div>
    </div>
</div>
