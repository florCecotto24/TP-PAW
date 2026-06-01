<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><spring:message code="app.title"/> - <c:out value="${documentFileName}"/></title>
    <link rel="icon" type="image/svg+xml" href="${pageContext.request.contextPath}/assets/images/Ryden_logo.ico">
    <style>
        html, body {
            margin: 0;
            padding: 0;
            height: 100%;
        }

        .doc-frame {
            width: 100%;
            height: 100%;
            border: 0;
        }
    </style>
</head>
<body>
<iframe
        id="docFrame"
        class="doc-frame"
        title="<spring:message code='profile.documents.sectionTitle'/>"></iframe>
<script>
    (function () {
        var frame = document.getElementById('docFrame');
        var url = "<c:url value='/profile/documents/${documentType}'/>";
        fetch(url, { credentials: 'same-origin' })
            .then(function (response) {
                if (!response.ok) {
                    throw new Error('Document could not be loaded');
                }
                return response.blob();
            })
            .then(function (blob) {
                var objectUrl = URL.createObjectURL(blob);
                frame.src = objectUrl;
                window.addEventListener('beforeunload', function () {
                    URL.revokeObjectURL(objectUrl);
                });
            })
            .catch(function () {
                window.location.href = "<c:url value='/profile'/>";
            });
    })();
</script>
</body>
</html>
