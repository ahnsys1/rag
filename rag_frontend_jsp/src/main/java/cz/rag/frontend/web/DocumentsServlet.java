package cz.rag.frontend.web;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import cz.rag.frontend.api.BackendException;
import cz.rag.frontend.model.RagDocument;
import cz.rag.frontend.model.UploadItem;
import cz.rag.frontend.model.UploadResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;

/** Document list + upload (multipart POST). Upload results survive the redirect via the session. */
@WebServlet(name = "documents", urlPatterns = "/documents")
@MultipartConfig(maxFileSize = 60L * 1024 * 1024, maxRequestSize = 300L * 1024 * 1024,
        fileSizeThreshold = 1024 * 1024)
public class DocumentsServlet extends BaseServlet {

    static final String FLASH_UPLOADS = "uploads";
    static final String FLASH_ERROR = "flashError";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session != null) {
            moveFlash(session, req, FLASH_UPLOADS);
            moveFlash(session, req, FLASH_ERROR);
        }
        if (req.getAttribute("error") == null && req.getAttribute(FLASH_ERROR) != null) {
            req.setAttribute("error", req.getAttribute(FLASH_ERROR));
        }

        List<RagDocument> docs = List.of();
        try {
            docs = backend.listDocuments();
        } catch (BackendException e) {
            req.setAttribute("error", e.getMessage());
        }
        long processing = docs.stream().filter(RagDocument::isProcessing).count();
        int chunks = docs.stream().mapToInt(RagDocument::getChunkCount).sum();
        req.setAttribute("documents", docs);
        req.setAttribute("processingCount", processing);
        req.setAttribute("indexedChunks", chunks);
        render(req, resp, "documents", "RAG – Dokumenty", "documents");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        List<UploadItem> results = new ArrayList<>();
        for (Part part : req.getParts()) {
            if (!"files".equals(part.getName()) || part.getSubmittedFileName() == null
                    || part.getSubmittedFileName().isBlank()) {
                continue;
            }
            String name = Paths.get(part.getSubmittedFileName()).getFileName().toString();
            try (InputStream in = part.getInputStream()) {
                UploadResponse res = backend.upload(name, part.getContentType(), in.readAllBytes());
                results.add(res.isDuplicate()
                        ? new UploadItem(name, "duplicate", "Dokument je již zaindexován.")
                        : new UploadItem(name, "accepted", "Přijato k indexaci."));
            } catch (BackendException e) {
                results.add(new UploadItem(name, "error", e.getMessage()));
            }
        }
        if (!results.isEmpty()) {
            req.getSession(true).setAttribute(FLASH_UPLOADS, results);
        }
        resp.sendRedirect(req.getContextPath() + "/documents");
    }

    private static void moveFlash(HttpSession session, HttpServletRequest req, String key) {
        Object value = session.getAttribute(key);
        if (value != null) {
            req.setAttribute(key, value);
            session.removeAttribute(key);
        }
    }
}
