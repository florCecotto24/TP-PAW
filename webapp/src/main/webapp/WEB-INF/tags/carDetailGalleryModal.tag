<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ attribute name="modalId" required="true" type="java.lang.String" %>
<%@ attribute name="carouselId" required="true" type="java.lang.String" %>
<%@ attribute name="mainImage" required="true" type="java.lang.String" %>
<%@ attribute name="topImage" required="true" type="java.lang.String" %>
<%@ attribute name="bottomImage" required="true" type="java.lang.String" %>
<%@ attribute name="mainAlt" required="false" type="java.lang.String" %>
<%@ attribute name="topAlt" required="false" type="java.lang.String" %>
<%@ attribute name="bottomAlt" required="false" type="java.lang.String" %>

<c:if test="${empty mainAlt}">
    <c:set var="mainAlt" value="Vehicle" />
</c:if>
<c:if test="${empty topAlt}">
    <c:set var="topAlt" value="Interior" />
</c:if>
<c:if test="${empty bottomAlt}">
    <c:set var="bottomAlt" value="Rear view" />
</c:if>

<div class="modal fade" id="${modalId}" tabindex="-1" aria-hidden="true" aria-labelledby="${modalId}Title">
    <div class="modal-dialog modal-dialog-centered modal-xl">
        <div class="modal-content bg-dark text-white border-0">
            <div class="modal-header border-0">
                <h2 class="modal-title fs-6" id="${modalId}Title">Gallery</h2>
                <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body p-0">
                <div id="${carouselId}" class="carousel slide" data-bs-ride="false">
                    <div class="carousel-inner">
                        <div class="carousel-item active">
                            <img src="${mainImage}" class="d-block w-100 car-detail-carousel-img" alt="${mainAlt}">
                        </div>
                        <div class="carousel-item">
                            <img src="${topImage}" class="d-block w-100 car-detail-carousel-img" alt="${topAlt}">
                        </div>
                        <div class="carousel-item">
                            <img src="${bottomImage}" class="d-block w-100 car-detail-carousel-img" alt="${bottomAlt}">
                        </div>
                    </div>
                    <button class="carousel-control-prev" type="button" data-bs-target="#${carouselId}" data-bs-slide="prev">
                        <span class="carousel-control-prev-icon" aria-hidden="true"></span>
                        <span class="visually-hidden">Previous</span>
                    </button>
                    <button class="carousel-control-next" type="button" data-bs-target="#${carouselId}" data-bs-slide="next">
                        <span class="carousel-control-next-icon" aria-hidden="true"></span>
                        <span class="visually-hidden">Next</span>
                    </button>
                </div>
            </div>
        </div>
    </div>
</div>
