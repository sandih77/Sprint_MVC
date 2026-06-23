package mg.core.utils;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import mg.core.annotation.Controller;
import mg.core.annotation.UrlMapping;
import mg.core.model.UrlMethodMapping;

public class Utils {
    public static List<Class<?>> getClassController(String packageName) throws URISyntaxException {
        List<Class<?>> classController = new ArrayList<>();
        String path = packageName.replace(".", "/");
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL resource = classLoader.getResource(path);
        File directory = new File(resource.toURI());
        if (directory.exists()) {
            for (File file : directory.listFiles()) {
                String className = packageName + "." + file.getName().replace(".class", "");
                try {
                    Class<?> clazz = Class.forName(className);

                    if (clazz.isAnnotationPresent(Controller.class)) {
                        classController.add(clazz);
                    }

                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
        return classController;
    }

    public static List<UrlMethodMapping> getAnnotedMethod(String packageName) throws URISyntaxException {
        List<UrlMethodMapping> list = new ArrayList<>();

        String path = packageName.replace(".", "/");
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL resource = classLoader.getResource(path);
        File directory = new File(resource.toURI());
        if (directory.exists()) {
            for (File file : directory.listFiles()) {
                String className = packageName + "." + file.getName().replace(".class", "");
                try {
                    Class<?> clazz = Class.forName(className);
                    if (clazz.isAnnotationPresent(Controller.class)) {
                        for (Method m : clazz.getDeclaredMethods()) {
                            if (m.isAnnotationPresent(UrlMapping.class)) {
                                UrlMethodMapping urlMethodMapping = new UrlMethodMapping();

                                urlMethodMapping.setController(clazz.getName());
                                urlMethodMapping.setMethod(m.getName());

                                UrlMapping annotation = m.getAnnotation(UrlMapping.class);
                                urlMethodMapping.setUrl(annotation.value());

                                list.add(urlMethodMapping);
                            }
                        }
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }

        return list;
    }
}
