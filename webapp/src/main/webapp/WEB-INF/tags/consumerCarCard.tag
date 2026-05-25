<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>

<%@ attribute name="card" required="true" type="ar.edu.itba.paw.webapp.dto.VehicleCardView" %>
<%@ attribute name="image" required="false" %>
<%@ attribute name="href" required="false" %>
<%@ attribute name="reviewCount" required="false" type="java.lang.Number" %>

<ryden:carCard
        model="${card.model}"
        brand="${card.brand}"
        price="${card.price}"
        image="${image}"
        pricePeriod="day"
        ratingAvg="${card.ratingAvg}"
        reviewCount="${not empty reviewCount ? reviewCount : card.reviewCount}"
        href="${href}"
        priceMarketPositionModifier="${card.priceMarketPositionModifier}"
        marketAveragePrice="${card.marketAveragePrice}"
        marketSampleCount="${card.marketSampleCount}"/>
