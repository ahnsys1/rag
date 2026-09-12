package cz.rag.frontend.web;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cz.rag.frontend.api.BackendClient;
import cz.rag.frontend.api.BackendException;
import cz.rag.frontend.model.DownloadedFile;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Per-document actions:
 *   GET  /documents/{id}/download  – streams the original file
 *   POST /documents/{id}/delete    – removes document + index, redirects back to the list
 */
@WebServlet(name = "documentActions", urlPatterns = "/documents/*")
public class DocumentActionServlet extends HttpServlet {

    private static final Pattern PATH = Pattern.compile("^/([0-9a-fA-F-]{36})/(download|delete)$");

    private final BackendClient backend = BackendClient.get();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Matcher m = match(req, resp);
        if (m == null) {
            return;
        }
        if (!"download".equals(m.group(2))) {
            resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        try {
            DownloadedFile file = backend.download(m.group(1));
            resp.setContentType(file.contentType());
            if (file.contentDisposition() != null) {
                resp.setHeader("Content-Disposition", file.contentDisposition());
            }
            resp.setContentLength(file.data().length);
            resp.getOutputStream().write(file.data());
        } catch (BackendException e) {
            flashErrorAndRedirect(req, resp, e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Matcher m = match(req, resp);
        if (m == null) {
            return;
        }
        if (!"delete".equals(m.group(2))) {
            resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        try {
            backend.delete(m.group(1));
        } catch (BackendException e) {
            req.getSession(true).setAttribute(DocumentsServlet.FLASH_ERROR, e.getMessage());
        }
        resp.sendRedirect(req.getContextPath() + "/documents");
    }

    private static Matcher match(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo() == null ? "" : req.getPathInfo();
        Matcher m = PATH.matcher(path);
        if (!m.matches()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }
        return m;
    }

    private static void flashErrorAndRedirect(HttpServletRequest req, HttpServletResponse resp,
                                              String message) throws IOException {
        req.getSession(true).setAttribute(DocumentsServlet.FLASH_ERROR, message);
        resp.sendRedirect(req.getContextPath() + "/documents");
    }
}
