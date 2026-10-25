package mg.core.model;

import java.util.HashMap;
import java.util.Map;

public class SSession {
    private Map<String, Object> attributes = new HashMap<>();

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public void setAttribute(String name, Object objet) {
        attributes.put(name, objet);
    }

    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    public void removeAttribute(String name) {
        attributes.remove(name);
    }
}
