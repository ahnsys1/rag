# RAG frontend – JSP (Jakarta Servlet 6 / JSP 3.1 / JSTL 3)

Server-side alternativa k Angular aplikaci `rag_frontend`. Stejné obrazovky a funkce,
ale vykreslené na serveru pomocí JSP a servletů; volání REST API backendu (FastAPI)
probíhá z Javy (`java.net.http.HttpClient` + Jackson).

```
rag_frontend_jsp/
├── pom.xml                                  # WAR, Java 21, JSTL, Jackson, cargo (dev Tomcat)
├── Dockerfile                               # maven build → tomcat:10.1
└── src/main/
    ├── java/cz/rag/frontend/
    │   ├── api/BackendClient.java           # /api/health, /documents, /search, /ask …
    │   ├── model/*.java                     # DTO (snake_case JSON → JavaBean)
    │   └── web/
    │       ├── IndexServlet.java            # /            → redirect /search
    │       ├── SearchServlet.java           # /search      GET form, POST Hledat / Zeptat se
    │       ├── DocumentsServlet.java        # /documents   GET seznam, POST multipart upload
    │       └── DocumentActionServlet.java   # /documents/{id}/download, /documents/{id}/delete
    └── webapp/
        ├── WEB-INF/web.xml
        ├── WEB-INF/views/{search,documents,error}.jsp + fragments/header.jspf
        └── static/styles.css                # port SCSS → CSS
```

## Mapování Angular → JSP

| Angular                                   | JSP / Servlet                                             |
|-------------------------------------------|-----------------------------------------------------------|
| `App` (topbar + `/api/health`)            | `BaseServlet.render()` + `fragments/header.jspf`           |
| `SearchComponent` (signals, ngModel)      | `SearchServlet` – POST form, hodnoty se vrací jako atributy |
| `DocumentsComponent` polling každé 2 s    | `<meta http-equiv="refresh" content="3">` dokud něco indexuje |
| drag & drop + `forkJoin` upload           | multipart form, JS jen nastaví `input.files` a odešle       |
| `HttpClient` blob download                | `GET /documents/{id}/download` proxy včetně `Content-Disposition` |
| `confirm()` před smazáním                 | `onsubmit="return confirm(...)"` + `POST …/delete`          |
| `describe(err)` (detail / 0 / status)     | `BackendException` v `BackendClient.describeError()`        |

## Spuštění

### Docker (spolu s backendem)

```bash
cd ~/Desktop/rag
docker compose up -d --build frontend-jsp
# http://localhost:4301
```

### Lokálně (vývoj)

Vyžaduje JDK 21+ a Maven; backend musí běžet na `http://localhost:8000`
(viz kořenový README – `uvicorn app.main:app --reload`).

```bash
cd rag_frontend_jsp
mvn package cargo:run              # stáhne Tomcat 10.1 a nasadí ROOT.war na http://localhost:4301
```

Jiná adresa backendu:

```bash
RAG_BACKEND_URL=http://127.0.0.1:8000 mvn package cargo:run
```

### Konfigurace

| Proměnná          | Default                  | Popis                          |
|-------------------|--------------------------|--------------------------------|
| `RAG_BACKEND_URL` | `http://localhost:8000`  | základ URL FastAPI backendu    |

Limity uploadu: 60 MB / soubor, 300 MB / požadavek (`@MultipartConfig`); backend má
vlastní limit `MAX_UPLOAD_MB` (50 MB) a případnou chybu zobrazí u souboru.
