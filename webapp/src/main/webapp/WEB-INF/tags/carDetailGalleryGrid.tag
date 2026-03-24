<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ attribute name="mainImage" required="true" type="java.lang.String" %>
<%@ attribute name="topImage" required="true" type="java.lang.String" %>
<%@ attribute name="bottomImage" required="true" type="java.lang.String" %>
<%@ attribute name="mainAlt" required="false" type="java.lang.String" %>
<%@ attribute name="topAlt" required="false" type="java.lang.String" %>
<%@ attribute name="bottomAlt" required="false" type="java.lang.String" %>
<%@ attribute name="modalId" required="true" type="java.lang.String" %>

<c:if test="${empty mainAlt}">
    <c:set var="mainAlt" value="Vehicle" />
</c:if>
<c:if test="${empty topAlt}">
    <c:set var="topAlt" value="Interior" />
</c:if>
<c:if test="${empty bottomAlt}">
    <c:set var="bottomAlt" value="Rear view" />
</c:if>

<div class="car-detail-gallery rounded-4 overflow-hidden">
    <button type="button"
            class="car-detail-gallery__cell car-detail-gallery__main btn p-0 border-0 bg-transparent w-100 h-100 text-start"
            data-bs-toggle="modal"
            data-bs-target="#${modalId}"
            data-carousel-index="0"
            aria-label="Open gallery: ${mainAlt}">
        <img src="${mainImage}" class="w-100 h-100 car-detail-gallery__img" alt="${mainAlt}">
    </button>
    <button type="button"
            class="car-detail-gallery__cell car-detail-gallery__side btn p-0 border-0 bg-transparent w-100 h-100 text-start"
            data-bs-toggle="modal"
            data-bs-target="#${modalId}"
            data-carousel-index="1"
            aria-label="Open gallery: ${topAlt}">
        <img src="${topImage}" class="w-100 h-100 car-detail-gallery__img" alt="${topAlt}">
    </button>
    <button type="button"
            class="car-detail-gallery__cell car-detail-gallery__side btn p-0 border-0 bg-transparent w-100 h-100 text-start"
            data-bs-toggle="modal"
            data-bs-target="#${modalId}"
            data-carousel-index="2"
            aria-label="Open gallery: ${bottomAlt}">
        <img src="${bottomImage}" class="w-100 h-100 car-detail-gallery__img" alt="${bottomAlt}">
    </button>
</div>
