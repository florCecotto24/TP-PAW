<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <title>Counterparty profile — <spring:message code="app.title"/></title>
    <%@include file="header.jsp" %>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/counterparty-profile.css">
</head>
<body class="has-fixed-navbar bg-light counterparty-profile-body">
<ryden:navbar/>

<main class="counterparty-profile-page">
    <div class="counterparty-profile-container">
        <div class="row g-4">
            <div class="col-12">
                <ryden:counterpartyProfileHeader
                        forename="${counterpartyForename}"
                        surname="${counterpartySurname}"
                        about="${counterpartyAbout}"
                        memberSinceDisplay="${counterpartyMemberSinceDisplay}"
                        profileImageId="${counterpartyProfileImageId}"
                        averageRating="${counterpartyAverageRating}"/>

                <ryden:counterpartyProfileReviews
                        comments="${recentReviewComments}"/>
            </div>
        </div>
    </div>
</main>

<%@include file="footer.jsp" %>
</body>
</html>
