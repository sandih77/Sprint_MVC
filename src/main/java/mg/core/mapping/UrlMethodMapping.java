package mg.core.mapping;

import java.lang.reflect.Method;

public class UrlMethodMapping {
    private Class<?> clazz;
    private Method method;

    public UrlMethodMapping() {
    }

    public UrlMethodMapping(Class<?> clazz, Method method) {
        this.setClazz(clazz);
        this.setMethod(method);
    }

    public Class<?> getClazz() {
        return this.clazz;
    }

    public void setClazz(Class<?> clazz) {
        this.clazz = clazz;
    }

    public Method getMethod() {
        return this.method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

}
