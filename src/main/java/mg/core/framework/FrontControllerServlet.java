package mg.core.framework;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import mg.core.annotation.Controller;
import mg.core.annotation.UrlMapping;
import mg.core.mapping.UrlMethod;
import mg.core.mapping.UrlMethodMapping;
import mg.core.utils.Utils;

public class FrontControllerServlet extends HttpServlet {

    private List<Class<?>> listController;
    private Map<UrlMethod, UrlMethodMapping> listUrlMethodMappings = new HashMap<>();

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        try {
            String basePackage = config.getInitParameter("base-package");

            listController = Utils.getClassController(basePackage, Controller.class);

            Utils.findUrlMethodMapping(basePackage,
                    Controller.class,
                    UrlMapping.class,
                    listUrlMethodMappings);

        } catch (URISyntaxException | ClassNotFoundException e) {
            throw new ServletException(e);
        }
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/plain;charset=UTF-8");

        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String relativePath = uri.substring(contextPath.length());
        String httpMethod = request.getMethod().toUpperCase();

        try (PrintWriter out = response.getWriter()) {

            out.println("======================================");
            out.println("          FRONT CONTROLLER");
            out.println("======================================");
            out.println();

            out.println("URI          : " + uri);
            out.println("Relative URI : " + relativePath);
            out.println("HTTP Method  : " + httpMethod);
            out.println();

            out.println("========== CONTROLLERS ==========");

            for (Class<?> controller : listController) {
                out.println(controller.getName());
            }

            out.println();
            out.println("========== URL MAPPINGS DISPONIBLE ==========");

            for (Map.Entry<UrlMethod, UrlMethodMapping> entry : listUrlMethodMappings.entrySet()) {

                UrlMethod key = entry.getKey();
                out.println("--------------------------------------");
                out.println("URL        : " + key.getUrl());
                out.println("HTTP       : " + key.getHttpMethod());
            }

            out.println();

            UrlMethod requestKey = new UrlMethod(relativePath, httpMethod);
            UrlMethodMapping mapping = listUrlMethodMappings.get(requestKey);

            if (mapping == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.println("404 - Aucun mapping correspondant.");
                return;
            } else {
                out.println("========== ROUTE TROUVÉE ==========");
                out.println("URL        : " + requestKey.getUrl());
                out.println("HTTP       : " + requestKey.getHttpMethod());
                out.println("Classe     : " + mapping.getClazz().getName());
                out.println("Méthode    : " + mapping.getMethod().getName());
            }

            try {
                Object controller = mapping.getClazz().getDeclaredConstructor().newInstance();
                Object result = mapping.getMethod().invoke(controller);
                out.println();
                out.println("========== RESULT ==========");
                out.println(result);
            } catch (Exception e) {
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }
}