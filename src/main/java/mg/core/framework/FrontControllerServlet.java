package mg.core.framework;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import mg.core.annotation.JsonResponse;
import mg.core.exception.DuplicateUrlMappingException;
import mg.core.mapping.UrlMethod;
import mg.core.mapping.UrlMethodMapping;
import mg.core.model.ModelAndView;
import mg.core.utils.JsonUtil;

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

            Method targetMethod = mapping.getMethod();
            Parameter[] parameters = targetMethod.getParameters();
            Object[] args = new Object[parameters.length];

            Object springContext = getServletContext().getAttribute(
                    "org.springframework.web.context.WebApplicationContext.ROOT");

            for (int i = 0; i < parameters.length; i++) {
                Class<?> paramType = parameters[i].getType();

                if (paramType.getName().equals("org.springframework.context.ApplicationContext")
                        || (springContext != null && paramType.isAssignableFrom(springContext.getClass()))) {
                    args[i] = springContext;
                } else if (HttpServletRequest.class.isAssignableFrom(paramType)) {
                    args[i] = request;
                } else if (HttpServletResponse.class.isAssignableFrom(paramType)) {
                    args[i] = response;
                } else {
                    args[i] = null;
                }
            }

            Object result = targetMethod.invoke(controller, args);

            if (result == null) {
                return;
            }

            if (targetMethod.isAnnotationPresent(JsonResponse.class)) {
                response.setContentType("application/json;charset=UTF-8");

                JsonResponse jsonAnnotation = targetMethod.getAnnotation(JsonResponse.class);
                boolean shouldFormat = jsonAnnotation.format();

                Object targetData = (result instanceof ModelAndView)
                        ? ((ModelAndView) result).getModel()
                        : result;

                if (shouldFormat) {
                    String jsonOutput = JsonUtil.formatToJson(targetData);
                    response.getWriter().print(jsonOutput);
                } else {
                    response.getWriter().print(targetData);
                }
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