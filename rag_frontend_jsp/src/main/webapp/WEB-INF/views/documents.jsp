<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ include file="fragments/header.jspf" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<section class="page">
  <header class="page-header">
    <div>
      <h1>Dokumenty</h1>
      <p class="muted">
        ${fn:length(documents)} dokumentů · ${indexedChunks} úryvků v indexu
        <c:if test="${processingCount gt 0}">
          · <span class="badge processing">${processingCount} se indexuje</span>
        </c:if>
      </p>
    </div>
    <a class="btn ghost" href="${ctx}/documents">Obnovit</a>
  </header>

  <form id="upload-form" method="post" action="${ctx}/documents" enctype="multipart/form-data">
    <label class="dropzone" id="dropzone">
      <input type="file" name="files" multiple accept=".pdf,.docx,.txt,.md" hidden>
      <strong>Přetáhněte soubory sem</strong> nebo klikněte pro výběr
      <span class="muted">PDF, DOCX, TXT, MD</span>
    </label>
  </form>

  <c:if test="${not empty uploads}">
    <ul class="uploads" id="uploads">
      <c:forEach var="u" items="${uploads}">
        <li class="${u.state}">
          <span class="name"><c:out value="${u.name}"/></span>
          <span class="state">
            <c:choose>
              <c:when test="${u.state eq 'error'}">Chyba: <c:out value="${u.message}"/></c:when>
              <c:otherwise><c:out value="${u.message}"/></c:otherwise>
            </c:choose>
          </span>
        </li>
      </c:forEach>
      <li class="actions"><button type="button" class="link" onclick="document.getElementById('uploads').remove()">Skrýt</button></li>
    </ul>
  </c:if>

  <c:if test="${not empty error}">
    <p class="alert"><c:out value="${error}"/></p>
  </c:if>

  <table class="docs">
    <thead>
      <tr>
        <th>Soubor</th>
        <th>Stav</th>
        <th class="num">Úryvky</th>
        <th class="num">Strany</th>
        <th class="num">Velikost</th>
        <th>Nahráno</th>
        <th></th>
      </tr>
    </thead>
    <tbody>
      <c:forEach var="doc" items="${documents}">
        <tr>
          <td class="name">
            <c:out value="${doc.filename}"/>
            <c:if test="${not empty doc.error}">
              <div class="error-text"><c:out value="${doc.error}"/></div>
            </c:if>
          </td>
          <td><span class="badge ${doc.status}"><c:out value="${doc.status}"/></span></td>
          <td class="num">${doc.chunkCount}</td>
          <td class="num">${empty doc.pageCount ? '–' : doc.pageCount}</td>
          <td class="num"><fmt:formatNumber value="${doc.sizeKb}" maxFractionDigits="0"/> kB</td>
          <td><c:out value="${doc.createdAtFormatted}"/></td>
          <td class="num">
            <a class="btn small" href="${ctx}/documents/${doc.id}/download">Stáhnout</a>
            <form method="post" action="${ctx}/documents/${doc.id}/delete" class="inline"
                  data-filename="<c:out value='${doc.filename}'/>"
                  onsubmit="return confirm('Smazat dokument \u201e' + this.dataset.filename + '\u201c včetně indexu?')">
              <button type="submit" class="btn danger small">Smazat</button>
            </form>
          </td>
        </tr>
      </c:forEach>
      <c:if test="${empty documents}">
        <tr>
          <td colspan="7" class="empty">Zatím nebyl nahrán žádný dokument.</td>
        </tr>
      </c:if>
    </tbody>
  </table>
</section>

<script>
  (function () {
    var form = document.getElementById('upload-form');
    var zone = document.getElementById('dropzone');
    var input = zone.querySelector('input[type=file]');

    function submitFiles() {
      if (!input.files.length) return;
      zone.classList.add('busy');
      zone.querySelector('strong').textContent = 'Nahrává se…';
      form.submit();
    }

    input.addEventListener('change', submitFiles);
    zone.addEventListener('dragover', function (e) { e.preventDefault(); zone.classList.add('active'); });
    zone.addEventListener('dragleave', function () { zone.classList.remove('active'); });
    zone.addEventListener('drop', function (e) {
      e.preventDefault();
      zone.classList.remove('active');
      if (e.dataTransfer && e.dataTransfer.files.length) {
        input.files = e.dataTransfer.files;
        submitFiles();
      }
    });
  })();
</script>
<%@ include file="fragments/footer.jspf" %>
