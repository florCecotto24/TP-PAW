<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<!DOCTYPE html>
<html lang="en">
<head>
  <%@include file="header.jsp"%>
  <title>Publish confirmation</title>
</head>
<body class="bg-light has-fixed-navbar">
<paw:navbar/>

<main class="container py-5">
  <div class="row justify-content-center">
    <div class="col-md-9 col-lg-7">
      <div class="card border-0 shadow-sm rounded-4">
        <div class="card-body p-4 p-md-5 text-center">
          <h1 class="h3 fw-bold mb-3">Car published</h1>
          <p class="mb-2">Thank you, <c:out value="${publisher.forename} ${publisher.surname}"/>.</p>
          <p class="text-secondary mb-3">
            Your car <strong>${listing.title}</strong> is now listed on Ryden and available for rental. You can manage your listing and view reservations from your profile.
          </p>
          <p class="text-secondary">The details of your listing are as follows:</p>
          <div class="card mb-3 mt-3">
            <div class="card-body text-start">
              <p><strong>Brand:</strong> <c:out value="${car.brand}"/></p>
              <p><strong>Model:</strong> <c:out value="${car.model}"/> </p>
              <p><strong>Price per day:</strong> $<c:out value="${listing.dayPrice}"/></p>
              <p class="${not empty listing.description ? "d-block" : "d-none"}"><strong>Description:</strong> <c:out value="${listing.description}"/></p>
              <p><strong>Plate:</strong> <c:out value="${car.plate}"/></p>
              <p><strong>Type:</strong> <c:out value="${car.type}"/></p>
              <p><strong>Powertrain:</strong> <c:out value="${car.powertrain}"/></p>
              <p><strong>Transmission:</strong> <c:out value="${car.transmission}"/></p>
            </div>
          </div>


          <a href="<c:url value='/search'/>" class="btn btn-primary px-4">Back to search</a>
        </div>
      </div>
    </div>
  </div>
</main>

<%@include file="footer.jsp"%>
</body>
</html>
