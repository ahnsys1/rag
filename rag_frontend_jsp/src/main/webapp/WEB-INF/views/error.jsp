<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="RAG – Chyba"/>
<%@ include file="fragments/header.jspf" %>
<section class="page">
  <header class="page-header">
    <div>
      <h1>Něco se pokazilo</h1>
      <p class="muted">HTTP ${pageContext.errorData.statusCode}</p>
    </div>
  </header>
  <p class="alert">
    <c:choose>
      <c:when test="${pageContext.errorData.statusCode eq 404}">Stránka nebyla nalezena.</c:when>
      <c:when test="${not empty pageContext.exception}"><c:out value="${pageContext.exception.message}"/></c:when>
      <c:otherwise>Neočekávaná chyba.</c:otherwise>
    </c:choose>
  </p>
  <p><a class="btn" href="${pageContext.request.contextPath}/search">Zpět na vyhledávání</a></p>
</section>
<%@ include file="fragments/footer.jspf" %>
