<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="fragments/header.jspf" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="llmOff" value="${not empty health and not health.llmAvailable}"/>

<section class="page">
  <header class="page-header">
    <div>
      <h1>Sémantické vyhledávání</h1>
      <p class="muted">Dotaz se převede na vektor a porovná s úryvky dokumentů v PostgreSQL / pgvector.</p>
    </div>
  </header>

  <form class="search-form" method="post" action="${ctx}/search" id="search-form">
    <textarea name="query" rows="3" placeholder="Search..."><c:out value="${query}"/></textarea>

    <div class="controls">
      <label>
        Režim
        <select name="mode">
          <option value="hybrid" ${mode eq 'hybrid' ? 'selected' : ''}>Hybridní (vektor + fulltext)</option>
          <option value="semantic" ${mode eq 'semantic' ? 'selected' : ''}>Pouze sémantický</option>
        </select>
      </label>
      <label>
        Počet výsledků
        <input name="topK" type="number" min="1" max="50" value="${topK}">
      </label>
      <label class="grow">
        Omezit na dokumenty
        <select name="docs" multiple size="3">
          <c:forEach var="d" items="${documents}">
            <c:set var="sel" value="false"/>
            <c:forEach var="id" items="${selectedDocumentIds}">
              <c:if test="${id eq d.id}"><c:set var="sel" value="true"/></c:if>
            </c:forEach>
            <option value="${d.id}" ${sel ? 'selected' : ''}><c:out value="${d.filename}"/></option>
          </c:forEach>
        </select>
      </label>
    </div>

    <div class="actions">
      <button type="submit" name="action" value="search" class="btn primary">Hledat</button>
      <button type="submit" name="action" value="ask" class="btn"
              title="${llmOff ? 'LLM není nakonfigurováno – vrátí se jen nalezené pasáže' : ''}">
        Zeptat se (RAG)
      </button>
      <c:if test="${llmOff}">
        <span class="muted small">LLM vypnuto – odpověď bude složena z nalezených pasáží.</span>
      </c:if>
    </div>
  </form>

  <c:if test="${not empty error}">
    <p class="alert"><c:out value="${error}"/></p>
  </c:if>

  <c:if test="${not empty answer}">
    <article class="answer">
      <h2>Odpověď
        <c:if test="${not empty answerModel}"><span class="muted small">(<c:out value="${answerModel}"/>)</span></c:if>
      </h2>
      <pre><c:out value="${answer}"/></pre>
    </article>
  </c:if>

  <c:if test="${not empty lastQuery}">
    <h2 class="results-title">
      ${not empty answer ? 'Zdroje' : 'Výsledky'}
      <span class="muted small">pro „<c:out value="${lastQuery}"/>“</span>
    </h2>
    <c:if test="${empty hits}">
      <p class="muted">Nic nenalezeno.</p>
    </c:if>
    <ol class="hits">
      <c:forEach var="hit" items="${hits}" varStatus="st">
        <li class="hit">
          <header>
            <span class="badge">[${st.count}]</span>
            <strong><c:out value="${hit.filename}"/></strong>
            <c:if test="${not empty hit.page}"><span class="muted">strana ${hit.page}</span></c:if>
            <span class="muted">úryvek #${hit.chunkIndex + 1}</span>
            <span class="score" title="${mode eq 'hybrid' ? 'Relativní RRF skóre' : 'Kosinová podobnost'}">
              <fmt:formatNumber value="${hit.score}" minFractionDigits="2" maxFractionDigits="3"/>
            </span>
          </header>
          <p><c:out value="${hit.content}"/></p>
        </li>
      </c:forEach>
    </ol>
  </c:if>
</section>

<script>
  // Ctrl+Enter submits the search, like the Angular (keydown.control.enter) binding.
  document.querySelector('#search-form textarea').addEventListener('keydown', function (e) {
    if (e.ctrlKey && e.key === 'Enter') {
      e.preventDefault();
      this.form.requestSubmit(this.form.querySelector('button[value="search"]'));
    }
  });
  // Disable buttons and show progress text while the request is running.
  document.getElementById('search-form').addEventListener('submit', function (e) {
    var btn = e.submitter;
    var buttons = this.querySelectorAll('button');
    setTimeout(function () {
      buttons.forEach(function (b) { b.disabled = true; });
      if (btn) btn.textContent = btn.value === 'ask' ? 'Generuji odpověď…' : 'Hledám…';
    }, 0);
  });
</script>
<%@ include file="fragments/footer.jspf" %>
