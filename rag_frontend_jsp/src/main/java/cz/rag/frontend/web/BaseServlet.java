package cz.rag.frontend.web;

import java.io.IOException;

import cz.rag.frontend.api.BackendClient;
import cz.rag.frontend.api.BackendException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Shared rendering: resolves backend health for the top bar and forwards to a JSP view. */
abstract class BaseServlet extends HttpServlet {

    protected final BackendClient backend = BackendClient.get();

    protected void render(HttpServletRequest req, HttpServletResponse resp, String view,
                          String title, String active) throws ServletException, IOException {
        try {
            req.setAttribute("health", backend.health());
        } catch (BackendException e) {
            req.setAttribute("offline", Boolean.TRUE);
        }
        req.setAttribute("pageTitle", title);
        req.setAttribute("active", active);
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("text/html;charset=UTF-8");
        req.getRequestDispatcher("/WEB-INF/views/" + view + ".jsp").forward(req, resp);
    }
}
