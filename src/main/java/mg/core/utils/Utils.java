package mg.core.utils;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import mg.core.annotation.UrlMapping;
import mg.core.exception.DuplicateUrlMappingException;
import mg.core.mapping.UrlMethod;
import mg.core.mapping.UrlMethodMapping;

public class Utils {

    public static List<Class<?>> getClassController(
            String packageName,
            Class<? extends Annotation> classAnnotation) throws URISyntaxException {

        List<Class<?>> classController = new ArrayList<>();

        String path = packageName.replace(".", "/");
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL resource = classLoader.getResource(path);

        if (resource == null)
            return classController;

        File directory = new File(resource.toURI());

        if (directory.exists()) {
            for (File file : directory.listFiles()) {
                if (!file.getName().endsWith(".class"))
                    continue;

                String className = packageName + "." + file.getName().replace(".class", "");

                try {
                    Class<?> clazz = Class.forName(className);

                    if (clazz.isAnnotationPresent(classAnnotation)) {
                        classController.add(clazz);
                    }
                } catch (ClassNotFoundException e) {
                }
            }
        }

        return classController;
    }

    public static void findUrlMethodMapping(
            String packageName,
            Class<? extends Annotation> classAnnotation,
            Class<? extends Annotation> methodAnnotation,
            Map<UrlMethod, UrlMethodMapping> urlMethodMappings)
            throws URISyntaxException, ClassNotFoundException {
        findUrlMethodMapping(packageName, classAnnotation, methodAnnotation, urlMethodMappings, null);
    }

    public static void findUrlMethodMapping(
            String packageName,
            Class<? extends Annotation> classAnnotation,
            Class<? extends Annotation> methodAnnotation,
            Map<UrlMethod, UrlMethodMapping> urlMethodMappings,
            Map<UrlMethod, DuplicateUrlMappingException> mappingErrors)
            throws URISyntaxException, ClassNotFoundException {

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String path = packageName.replace(".", "/");
        URL resource = classLoader.getResource(path);

        if (resource == null)
            return;

        File directory = new File(resource.toURI());

        if (!directory.exists())
            return;

        for (File file : directory.listFiles()) {
            if (!file.getName().endsWith(".class"))
                continue;

            String className = packageName + "." + file.getName().replace(".class", "");
            Class<?> clazz = Class.forName(className);

            if (clazz.isAnnotationPresent(classAnnotation)) {

                for (Method m : clazz.getDeclaredMethods()) {

                    if (m.isAnnotationPresent(methodAnnotation)) {

                        UrlMapping urlMapping = m.getAnnotation(UrlMapping.class);

                        String url = urlMapping.path();
                        String httpMethod = urlMapping.method().toUpperCase();

                        UrlMethod key = new UrlMethod(url, httpMethod);
                        UrlMethodMapping value = new UrlMethodMapping(clazz, m);

                        if (urlMethodMappings.containsKey(key)) {
                            UrlMethodMapping existing = urlMethodMappings.get(key);

                            DuplicateUrlMappingException exception = new DuplicateUrlMappingException(
                                    "Duplicate URL mapping detected: "
                                            + url + " [" + httpMethod + "]\n"
                                            + "First: " + existing.getClazz().getName() + "#"
                                            + existing.getMethod().getName() + "\n"
                                            + "Second: " + clazz.getName() + "#"
                                            + m.getName());

                            if (mappingErrors != null) {
                                mappingErrors.put(key, exception);
                                continue;
                            }

                            throw exception;
                        }

                        urlMethodMappings.put(key, value);
                    }
                }
            }
        }
    }

    public static Object convertParam(String rawValue, Class<?> targetType) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            if (targetType.isPrimitive()) {
                if (targetType == boolean.class)
                    return false;
                if (targetType == int.class)
                    return 0;
                if (targetType == double.class)
                    return 0.0;
                if (targetType == float.class)
                    return 0.0;
                if (targetType == long.class)
                    return 0L;
            }
            return null;
        }

        if (targetType == String.class)
            return rawValue;
        if (targetType == Integer.class || targetType == int.class)
            return Integer.parseInt(rawValue);
        if (targetType == Double.class || targetType == double.class)
            return Double.parseDouble(rawValue);
        if (targetType == float.class || targetType == Float.class)
            return Float.parseFloat(rawValue);
        if (targetType == boolean.class || targetType == Boolean.class)
            return Boolean.parseBoolean(rawValue);
        if (targetType == LocalDate.class)
            return LocalDate.parse(rawValue);
        if (targetType == LocalDateTime.class)
            return LocalDateTime.parse(rawValue);
        return rawValue;
    }
}
