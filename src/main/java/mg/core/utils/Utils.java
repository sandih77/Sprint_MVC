package mg.core.utils;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import jakarta.servlet.http.HttpServletRequest;
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

    public static boolean isSimpleType(Class<?> type) {
        return type.isPrimitive()
                || type == String.class
                || type == Integer.class
                || type == Long.class
                || type == Double.class
                || type == Float.class
                || type == Boolean.class
                || type == LocalDate.class
                || type == LocalDateTime.class;
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
                    return 0.0f;
                if (targetType == long.class)
                    return 0L;
            }
            return null;
        }

        rawValue = rawValue.trim();

        if (targetType == String.class)
            return rawValue;
        if (targetType == Integer.class || targetType == int.class)
            return Integer.parseInt(rawValue);
        if (targetType == Long.class || targetType == long.class)
            return Long.parseLong(rawValue);
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

    public static Object convertArray(String[] rawValues, Class<?> componentType) {
        if (rawValues == null) {
            return Array.newInstance(componentType, 0);
        }
        Object array = Array.newInstance(componentType, rawValues.length);
        for (int i = 0; i < rawValues.length; i++) {
            Array.set(array, i, convertParam(rawValues[i], componentType));
        }
        return array;
    }

    public static List<?> convertList(String[] rawValues, Class<?> itemType) {
        List<Object> list = new ArrayList<>();
        if (rawValues != null) {
            for (String raw : rawValues) {
                list.add(convertParam(raw, itemType));
            }
        }
        return list;
    }

    public static Object bindObject(HttpServletRequest request, Class<?> clazz, String prefix) {
        try {
            Object instance = clazz.getDeclaredConstructor().newInstance();
            Field[] fields = clazz.getDeclaredFields();

            for (Field f : fields) {
                String fieldName = f.getName();
                String fullParamName = (prefix != null && !prefix.isEmpty()) ? prefix + "." + fieldName : fieldName;

                Class<?> fType = f.getType();
                Object valueToSet = null;

                if (fType.isArray()) {
                    // Champ de type tableau (ex: String[], Integer[])
                    String[] rawValues = request.getParameterValues(fullParamName);
                    valueToSet = convertArray(rawValues, fType.getComponentType());
                } else if (List.class.isAssignableFrom(fType)) {
                    // Champ de type List (ex: List<String>)
                    String[] rawValues = request.getParameterValues(fullParamName);
                    Class<?> genericType = String.class;
                    if (f.getGenericType() instanceof ParameterizedType) {
                        ParameterizedType pt = (ParameterizedType) f.getGenericType();
                        genericType = (Class<?>) pt.getActualTypeArguments()[0];
                    }
                    valueToSet = convertList(rawValues, genericType);
                } else if (isSimpleType(fType)) {
                    // Champ primitif / simple
                    String rawValue = request.getParameter(fullParamName);
                    if (rawValue != null) {
                        valueToSet = convertParam(rawValue, fType);
                    }
                } else {
                    // Objet imbriqué (ex: Address)
                    valueToSet = bindObject(request, fType, fullParamName);
                }

                if (valueToSet != null) {
                    String setterName = "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
                    try {
                        Method setter = clazz.getMethod(setterName, fType);
                        setter.invoke(instance, valueToSet);
                    } catch (NoSuchMethodException nsm) {
                        f.setAccessible(true);
                        f.set(instance, valueToSet);
                    }
                }
            }
            return instance;
        } catch (Exception e) {
            throw new RuntimeException("Erreur de binding pour l'objet " + clazz.getSimpleName(), e);
        }
    }

    public static Object bindObjectArray(HttpServletRequest request, Class<?> componentType, String paramName) {
        try {
            Set<Integer> indexes = new TreeSet<>();
            Enumeration<String> parameterNames = request.getParameterNames();

            String prefixPatternWithBracket = paramName + "[";

            while (parameterNames.hasMoreElements()) {
                String name = parameterNames.nextElement();
                if (name.startsWith(prefixPatternWithBracket)) {
                    int closeBracket = name.indexOf(']');
                    if (closeBracket > prefixPatternWithBracket.length() - 1) {
                        String indexStr = name.substring(prefixPatternWithBracket.length(), closeBracket);
                        try {
                            indexes.add(Integer.parseInt(indexStr));
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
            }

            Object array = Array.newInstance(componentType, indexes.size());
            int i = 0;
            for (Integer index : indexes) {
                String itemPrefix = paramName + "[" + index + "]";
                Object item = bindObject(request, componentType, itemPrefix);
                Array.set(array, i++, item);
            }

            return array;
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors du binding du tableau " + componentType.getSimpleName() + "[]", e);
        }
    }
}
