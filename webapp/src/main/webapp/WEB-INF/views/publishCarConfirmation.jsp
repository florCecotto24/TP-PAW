<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<!DOCTYPE html>
<html lang="en">
<head>
  <%@include file="header.jsp"%>
  <title>Publish confirmation</title>
</head>
<body class="bg-light has-fixed-navbar">
<ryden:navbar/>

<main class="container py-5">
  <div class="row justify-content-center">
    <div class="col-md-9 col-lg-7">
      <div class="card border-0 shadow-sm rounded-4">
        <div class="card-body p-4 p-md-5 text-center">
          <h1 class="h3 fw-bold mb-3"><spring:message code="publishCar.confirmation.title"/></h1>
          <p class="mb-2">
            <spring:message code="publishCar.confirmation.greeting.before"/>
            <c:out value="${publisher.forename}"/> <c:out value="${publisher.surname}"/><spring:message code="publishCar.confirmation.greeting.after"/>
          </p>
          <p class="mb-2">
            <spring:message code="publishCar.confirmation.message.before"/> <strong><c:out value="${listing.title}"/></strong> <spring:message code="publishCar.confirmation.message.after"/>
          </p>
          <p class="text-secondary"><spring:message code="publishCar.confirmation.details"/></p>
          <div class="card mb-3 mt-3">
            <div class="card-body text-start">
              <p><strong><spring:message code="publishCar.confirmation.brand"/></strong> <c:out value="${car.brand}"/></p>
              <p><strong><spring:message code="publishCar.confirmation.model"/></strong> <c:out value="${car.model}"/> </p>
              <p><strong><spring:message code="publishCar.confirmation.pricePerDay"/></strong> $<c:out value="${listing.dayPrice}"/></p>
              <p class="${not empty listing.description ? "d-block" : "d-none"}"><strong><spring:message code="publishCar.confirmation.description"/></strong> <c:out value="${listing.description}"/></p>
              <p><strong><spring:message code="publishCar.confirmation.plate"/></strong> <c:out value="${car.plate}"/></p>
              <p><strong><spring:message code="publishCar.confirmation.type"/></strong> <c:out value="${car.type.prettyName()}"/></p>
              <p><strong><spring:message code="publishCar.confirmation.powertrain"/></strong> <c:out value="${car.powertrain.prettyName()}"/></p>
              <p><strong><spring:message code="publishCar.confirmation.transmission"/></strong> <c:out value="${car.transmission.prettyName()}"/></p>
            </div>
          </div>


          <a href="<c:url value='/search'/>" class="btn btn-primary btn-action btn-action-md"><spring:message code="common.backToSearch"/></a>
        </div>
      </div>
    </div>
  </div>
</main>

<%@include file="footer.jsp"%>
</body>
</html>
