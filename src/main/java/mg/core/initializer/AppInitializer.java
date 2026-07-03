package mg.core.initializer;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import mg.core.utils.Utils;
import mg.core.annotation.Controller;
import mg.core.annotation.UrlMapping;
import mg.core.exception.DuplicateUrlMappingException;
import mg.core.mapping.UrlMethod;
import mg.core.mapping.UrlMethodMapping;

@WebListener
public class AppInitializer implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext context = sce.getServletContext();
        String basePackage = context.getInitParameter("base-package");

        try {
            List<Class<?>> controllers = Utils.getClassController(basePackage, Controller.class);
            Map<UrlMethod, UrlMethodMapping> mappings = new HashMap<>();
            Map<UrlMethod, DuplicateUrlMappingException> mappingErrors = new HashMap<>();
            Utils.findUrlMethodMapping(basePackage, Controller.class, UrlMapping.class, mappings, mappingErrors);

            context.setAttribute("controllers", controllers);
            context.setAttribute("mappings", mappings);
            context.setAttribute("mappingErrors", mappingErrors);
        } catch (URISyntaxException | ClassNotFoundException e) {

        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }
}
