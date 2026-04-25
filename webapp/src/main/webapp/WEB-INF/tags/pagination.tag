<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ attribute name="currentPage" required="true" type="java.lang.Integer" %>
<%@ attribute name="totalPages"  required="true" type="java.lang.Integer" %>
<%@ attribute name="baseUrl"     required="true" type="java.lang.String" %>
<%@ attribute name="pageParam"   required="false" type="java.lang.String" %>
<%@ attribute name="sortParam"   required="false" type="java.lang.String" %>

<%-- Renders nothing when there is only one page --%>
<c:if test="${totalPages > 1}">
    <%-- Set default pageParam if not provided --%>
    <c:if test="${empty pageParam}">
        <c:set var="pageParam" value="page"/>
    </c:if>
    
    <c:set var="sep" value="${fn:contains(baseUrl, '?') ? '&amp;' : '?'}"/>
    <c:if test="${not empty sortParam}">
        <c:set var="sortSuffix" value="&amp;sort=${fn:escapeXml(sortParam)}"/>
    </c:if>
    <c:if test="${empty sortParam}">
        <c:set var="sortSuffix" value=""/>
    </c:if>

    <spring:message code="pagination.prev" var="prevLabel"/>
    <spring:message code="pagination.next" var="nextLabel"/>

    <nav aria-label="Page navigation" class="mt-4">
        <ul class="pagination justify-content-center flex-wrap gap-1">

            <%-- Previous --%>
            <li class="page-item${currentPage == 0 ? ' disabled' : ''}">
                <c:choose>
                    <c:when test="${currentPage > 0}">
                        <a class="page-link" href="<c:out value='${baseUrl}' escapeXml='false'/>${sep}${pageParam}=${currentPage - 1}${sortSuffix}">
                            <c:out value="${prevLabel}"/>
                        </a>
                    </c:when>
                    <c:otherwise>
                        <span class="page-link"><c:out value="${prevLabel}"/></span>
                    </c:otherwise>
                </c:choose>
            </li>

            <%-- Page numbers: show up to 7 with ellipsis --%>
            <c:set var="windowStart" value="${currentPage - 2 < 0 ? 0 : currentPage - 2}"/>
            <c:set var="windowEnd"   value="${currentPage + 2 >= totalPages ? totalPages - 1 : currentPage + 2}"/>

            <%-- First page + leading ellipsis --%>
            <c:if test="${windowStart > 0}">
                <li class="page-item">
                    <a class="page-link" href="<c:out value='${baseUrl}' escapeXml='false'/>${sep}${pageParam}=0${sortSuffix}">1</a>
                </li>
                <c:if test="${windowStart > 1}">
                    <li class="page-item disabled"><span class="page-link">&hellip;</span></li>
                </c:if>
            </c:if>

            <%-- Window --%>
            <c:forEach begin="${windowStart}" end="${windowEnd}" var="p">
                <li class="page-item${p == currentPage ? ' active' : ''}">
                    <c:choose>
                        <c:when test="${p == currentPage}">
                            <span class="page-link">${p + 1}</span>
                        </c:when>
                        <c:otherwise>
                            <a class="page-link" href="<c:out value='${baseUrl}' escapeXml='false'/>${sep}${pageParam}=${p}${sortSuffix}">${p + 1}</a>
                        </c:otherwise>
                    </c:choose>
                </li>
            </c:forEach>

            <%-- Trailing ellipsis + last page --%>
            <c:if test="${windowEnd < totalPages - 1}">
                <c:if test="${windowEnd < totalPages - 2}">
                    <li class="page-item disabled"><span class="page-link">&hellip;</span></li>
                </c:if>
                <li class="page-item">
                    <a class="page-link" href="<c:out value='${baseUrl}' escapeXml='false'/>${sep}${pageParam}=${totalPages - 1}${sortSuffix}">${totalPages}</a>
                </li>
            </c:if>

            <%-- Next --%>
            <li class="page-item${currentPage >= totalPages - 1 ? ' disabled' : ''}">
                <c:choose>
                    <c:when test="${currentPage < totalPages - 1}">
                        <a class="page-link" href="<c:out value='${baseUrl}' escapeXml='false'/>${sep}${pageParam}=${currentPage + 1}${sortSuffix}">
                            <c:out value="${nextLabel}"/>
                        </a>
                    </c:when>
                    <c:otherwise>
                        <span class="page-link"><c:out value="${nextLabel}"/></span>
                    </c:otherwise>
                </c:choose>
            </li>

        </ul>
    </nav>
</c:if>
