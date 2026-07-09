package mg.core.framework;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import mg.core.exception.DuplicateUrlMappingException;
import mg.core.mapping.UrlMethod;
import mg.core.mapping.UrlMethodMapping;
import mg.core.model.ModelAndView;

public class FrontControllerServlet extends HttpServlet {

    private List<Class<?>> listController;
    private Map<UrlMethod, UrlMethodMapping> listUrlMethodMappings;
    private Map<UrlMethod, DuplicateUrlMappingException> mappingErrors;

    private String prefix;
    private String suffix;

    @SuppressWarnings("unchecked")
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        listController = (List<Class<?>>) getServletContext().getAttribute("controllers");
        listUrlMethodMappings = (Map<UrlMethod, UrlMethodMapping>) getServletContext().getAttribute("mappings");
        mappingErrors = (Map<UrlMethod, DuplicateUrlMappingException>) getServletContext()
                .getAttribute("mappingErrors");

        if (listController == null) {
            listController = new ArrayList<>();
        }

        if (listUrlMethodMappings == null) {
            listUrlMethodMappings = new HashMap<>();
        }

        if (mappingErrors == null) {
            mappingErrors = new HashMap<>();
        }

        prefix = getInitParameter("prefix");
        suffix = getInitParameter("suffix");
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String relativePath = uri.substring(contextPath.length());
        String httpMethod = request.getMethod().toUpperCase();

        UrlMethod requestKey = new UrlMethod(relativePath, httpMethod);

        DuplicateUrlMappingException duplicate = mappingErrors.get(requestKey);
        if (duplicate != null) {
            throw new ServletException(duplicate);
        }

        UrlMethodMapping mapping = listUrlMethodMappings.get(requestKey);

        if (mapping == null) {
            response.sendError(
                    HttpServletResponse.SC_NOT_FOUND,
                    "Aucun mapping trouvé pour : " + relativePath + " [" + httpMethod + "]");
            return;
        }

        try {
            Object controller = mapping.getClazz()
                    .getDeclaredConstructor()
                    .newInstance();

            Object result = mapping.getMethod().invoke(controller);

            if (result == null) {
                return;
            }

            if (result instanceof ModelAndView) {
                ModelAndView mv = (ModelAndView) result;
                if (mv.getModel() != null) {
                    for (Map.Entry<String, Object> entry : mv.getModel().entrySet()) {
                        request.setAttribute(entry.getKey(), entry.getValue());
                    }
                }
                String destination = prefix + mv.getView() + suffix;
                request.getRequestDispatcher(destination).forward(request, response);
                return;
            }

            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().print(result);

        } catch (Exception e) {
            throw new ServletException(
                    "Erreur lors de l'exécution de la méthode : "
                            + mapping.getMethod().getName(),
                    e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }
}