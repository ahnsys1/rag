package cz.rag.frontend.web;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import cz.rag.frontend.api.BackendException;
import cz.rag.frontend.model.AskResponse;
import cz.rag.frontend.model.RagDocument;
import cz.rag.frontend.model.SearchRequest;
import cz.rag.frontend.model.SearchResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(name = "search", urlPatterns = "/search")
public class SearchServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setAttribute("query", "");
        req.setAttribute("topK", 5);
        req.setAttribute("mode", "hybrid");
        req.setAttribute("selectedDocumentIds", List.of());
        show(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String query = param(req, "query", "").trim();
        int topK = clamp(parseInt(param(req, "topK", "5"), 5), 1, 50);
        String mode = "semantic".equals(req.getParameter("mode")) ? "semantic" : "hybrid";
        String[] docParams = req.getParameterValues("docs");
        List<String> selected = docParams == null ? List.of() : Arrays.asList(docParams);
        String action = param(req, "action", "search");

        req.setAttribute("query", query);
        req.setAttribute("topK", topK);
        req.setAttribute("mode", mode);
        req.setAttribute("selectedDocumentIds", selected);

        if (!query.isEmpty()) {
            SearchRequest request = new SearchRequest(query, topK, mode, selected.isEmpty() ? null : selected);
            try {
                if ("ask".equals(action)) {
                    AskResponse res = backend.ask(request);
                    req.setAttribute("answer", res.getAnswer());
                    req.setAttribute("answerModel", res.getModel());
                    req.setAttribute("hits", res.getSources());
                } else {
                    SearchResponse res = backend.search(request);
                    req.setAttribute("hits", res.getHits());
                }
                req.setAttribute("lastQuery", query);
            } catch (BackendException e) {
                req.setAttribute("error", e.getMessage());
            }
        }
        show(req, resp);
    }

    private void show(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            req.setAttribute("documents",
                    backend.listDocuments().stream().filter(RagDocument::isIndexed).toList());
        } catch (BackendException e) {
            req.setAttribute("documents", List.<RagDocument>of());
        }
        render(req, resp, "search", "RAG – Vyhledávání", "search");
    }

    private static String param(HttpServletRequest req, String name, String def) {
        String v = req.getParameter(name);
        return v == null ? def : v;
    }

    private static int parseInt(String s, int def) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
