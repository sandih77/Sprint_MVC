package mg.core.utils;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import mg.core.annotation.Controller;

public class Utils {
    public static List<String> getClassController(String packageName) throws URISyntaxException {
        List<String> classController = new ArrayList<>();
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
                        classController.add(clazz.getName());
                    }

                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
        return classController;
    }
}
