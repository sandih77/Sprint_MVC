package mg.core.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import mg.core.annotation.UrlMapping;
import mg.core.exception.DuplicateUrlMappingException;
import mg.core.mapping.MapFile;
import mg.core.mapping.UrlMethod;
import mg.core.mapping.UrlMethodMapping;

public class Utils {

    public static String getUploadDirectory() {
        Properties properties = new Properties();
        try (InputStream input = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("config.properties")) {
            if (input == null) {
                throw new IllegalStateException("Le fichier config.properties est introuvable.");
            }
            properties.load(input);
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de lire config.properties.", e);
        }

        String directory = properties.getProperty("upload.directory");
        if (directory == null || directory.isBlank()) {
            throw new IllegalStateException("La propriété upload.directory est obligatoire.");
        }
        return directory;
    }

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
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (!file.getName().endsWith(".class"))
                        continue;

                    String className = packageName + "." + file.getName().replace(".class", "");

                    try {
                        Class<?> clazz = Class.forName(className);

                        if (clazz.isAnnotationPresent(classAnnotation)) {
                            classController.add(clazz);
                        }
                    } catch (ClassNotFoundException ignored) {
                    }
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

        File[] files = directory.listFiles();
        if (files == null)
            return;

        for (File file : files) {
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

    public static boolean isMultipart(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null && contentType.toLowerCase().startsWith("multipart/form-data");
    }

    public static MapFile extractMapFile(Part part) {
        if (part == null || part.getSubmittedFileName() == null || part.getSubmittedFileName().trim().isEmpty()) {
            return null;
        }

        try {
            String fullFileName = part.getSubmittedFileName();
            if (fullFileName.contains("\\")) {
                fullFileName = fullFileName.substring(fullFileName.lastIndexOf('\\') + 1);
            }
            if (fullFileName.contains("/")) {
                fullFileName = fullFileName.substring(fullFileName.lastIndexOf('/') + 1);
            }

            String extension = "";
            int dotIndex = fullFileName.lastIndexOf('.');
            if (dotIndex >= 0 && dotIndex < fullFileName.length() - 1) {
                extension = fullFileName.substring(dotIndex + 1);
            }

            byte[] primitiveBytes;
            try (InputStream is = part.getInputStream();
                    ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    baos.write(buffer, 0, read);
                }
                primitiveBytes = baos.toByteArray();
            }

            Byte[] objectBytes = new Byte[primitiveBytes.length];
            for (int i = 0; i < primitiveBytes.length; i++) {
                objectBytes[i] = primitiveBytes[i];
            }

            MapFile mapFile = new MapFile();
            mapFile.setFileName(fullFileName);
            mapFile.setExtension(extension);
            mapFile.setBytes(objectBytes);

            return mapFile;
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la lecture du fichier uploadé : " + part.getName(), e);
        }
    }

    public static void save(MapFile mapFile, String directory) {
        if (mapFile == null) {
            throw new IllegalArgumentException("Le fichier à sauvegarder ne peut pas être null.");
        }
        if (directory == null || directory.isBlank()) {
            throw new IllegalArgumentException("Le répertoire de sauvegarde ne peut pas être vide.");
        }
        if (mapFile.getFileName() == null || mapFile.getFileName().isBlank()) {
            throw new IllegalArgumentException("Le nom du fichier ne peut pas être vide.");
        }
        if (mapFile.getBytes() == null) {
            throw new IllegalArgumentException("Le contenu du fichier ne peut pas être null.");
        }

        Path targetDirectory = Paths.get(directory);
        Path fileName = Paths.get(mapFile.getFileName()).getFileName();
        if (fileName == null || !fileName.toString().equals(mapFile.getFileName())) {
            throw new IllegalArgumentException("Le nom du fichier n'est pas valide.");
        }

        try {
            Files.createDirectories(targetDirectory);
            byte[] content = new byte[mapFile.getBytes().length];
            for (int i = 0; i < mapFile.getBytes().length; i++) {
                content[i] = mapFile.getBytes()[i];
            }
            Files.write(targetDirectory.resolve(fileName), content);
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la sauvegarde du fichier : " + mapFile.getFileName(), e);
        }
    }

    public static void save(MapFile mapFile) {
        save(mapFile, getUploadDirectory());
    }

    public static List<MapFile> extractMapFiles(HttpServletRequest request, String partName) {
        List<MapFile> files = new ArrayList<>();
        if (!isMultipart(request)) {
            return files;
        }
        try {
            Collection<Part> parts = request.getParts();
            for (Part part : parts) {
                if (part.getName().equals(partName)) {
                    MapFile mf = extractMapFile(part);
                    if (mf != null) {
                        files.add(mf);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la récupération des fichiers pour le champ : " + partName, e);
        }
        return files;
    }

    private static boolean isMapFileList(Field field) {
        if (field.getGenericType() instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) field.getGenericType();
            if (pt.getActualTypeArguments().length > 0) {
                return pt.getActualTypeArguments()[0].equals(MapFile.class);
            }
        }
        return false;
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
            boolean multipart = isMultipart(request);

            for (Field f : fields) {
                String fieldName = f.getName();
                String fullParamName = (prefix != null && !prefix.isEmpty()) ? prefix + "." + fieldName : fieldName;

                Class<?> fType = f.getType();
                Object valueToSet = null;

                if (fType.equals(MapFile.class)) {
                    if (multipart) {
                        Part part = null;
                        try {
                            part = request.getPart(fullParamName);
                        } catch (Exception ignored) {
                        }
                        if (part != null) {
                            valueToSet = extractMapFile(part);
                        }
                    }
                } else if (fType.isArray() && fType.getComponentType().equals(MapFile.class)) {
                    if (multipart) {
                        List<MapFile> files = extractMapFiles(request, fullParamName);
                        valueToSet = files.toArray(new MapFile[0]);
                    }
                } else if (List.class.isAssignableFrom(fType) && isMapFileList(f)) {
                    if (multipart) {
                        valueToSet = extractMapFiles(request, fullParamName);
                    }
                } else if (fType.isArray()) {
                    String[] rawValues = request.getParameterValues(fullParamName);
                    valueToSet = convertArray(rawValues, fType.getComponentType());
                } else if (List.class.isAssignableFrom(fType)) {
                    String[] rawValues = request.getParameterValues(fullParamName);
                    Class<?> genericType = String.class;
                    if (f.getGenericType() instanceof ParameterizedType) {
                        ParameterizedType pt = (ParameterizedType) f.getGenericType();
                        genericType = (Class<?>) pt.getActualTypeArguments()[0];
                    }
                    valueToSet = convertList(rawValues, genericType);
                } else if (isSimpleType(fType)) {
                    String rawValue = request.getParameter(fullParamName);
                    if (rawValue != null) {
                        valueToSet = convertParam(rawValue, fType);
                    }
                } else {
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
            String prefixPatternWithBracket = paramName + "[";

            Enumeration<String> parameterNames = request.getParameterNames();
            while (parameterNames.hasMoreElements()) {
                String name = parameterNames.nextElement();
                extractIndex(name, prefixPatternWithBracket, indexes);
            }

            if (isMultipart(request)) {
                try {
                    for (Part part : request.getParts()) {
                        extractIndex(part.getName(), prefixPatternWithBracket, indexes);
                    }
                } catch (Exception ignored) {
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

    private static void extractIndex(String name, String prefixPatternWithBracket, Set<Integer> indexes) {
        if (name != null && name.startsWith(prefixPatternWithBracket)) {
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
}