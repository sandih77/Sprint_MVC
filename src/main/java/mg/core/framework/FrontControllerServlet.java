package mg.core.framework;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.List;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mg.core.model.UrlMethodMapping;
import mg.core.utils.Utils;

public class FrontControllerServlet extends HttpServlet {
    private List<Class<?>> listController;
    private List<UrlMethodMapping> listUrlMethodMappings;

    @Override
    public void init(ServletConfig config) throws ServletException {
        try {
            super.init(config);
            String basePackage = config.getInitParameter("base-package");
            this.listController = Utils.getClassController(basePackage);
            this.listUrlMethodMappings = Utils.getAnnotedMethod(basePackage);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/plain;charset=UTF-8");

        String uri = request.getRequestURI();

        try (PrintWriter out = response.getWriter()) {

            out.println("========================================");
            out.println("          FRONT CONTROLLER");
            out.println("========================================");
            out.println();

            out.println("[REQUEST]");
            out.println("URI          : " + uri);
            out.println("Context Path : " + request.getContextPath());
            out.println();

            out.println("[CONTROLLERS]");
            if (listController.isEmpty()) {
                out.println("Aucun controller trouvé.");
            } else {
                for (Class<?> controller : listController) {
                    out.println(" - " + controller.getName());
                }
            }

            out.println();
            out.println("[URL MAPPINGS]");

            if (listUrlMethodMappings.isEmpty()) {
                out.println("Aucun mapping trouvé.");
            } else {
                for (UrlMethodMapping mapping : listUrlMethodMappings) {
                    out.printf(
                            " %-20s -> %s#%s%n",
                            mapping.getUrl(),
                            mapping.getController(),
                            mapping.getMethod());
                }
            }

            out.println();
            out.println("[MATCH RESULT]");

            boolean found = false;

            for (UrlMethodMapping mapping : listUrlMethodMappings) {

                String mappedUrl = request.getContextPath() + mapping.getUrl();

                if (mappedUrl.equalsIgnoreCase(uri)) {

                    found = true;

                    out.println("Route trouvée !");
                    out.println("----------------------------------------");
                    out.println("URL        : " + mapping.getUrl());
                    out.println("Controller : " + mapping.getController());
                    out.println("Method     : " + mapping.getMethod());
                    out.println("----------------------------------------");

                    break;
                }
            }

            if (!found) {
                out.println("404 - Aucun mapping correspondant.");
            }

            out.println();
            out.println("========================================");
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }
}
