package mg.core.utils;

import java.util.Enumeration;
import java.util.Map;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpSession;
import mg.core.model.SSession;

public class SSessionUtils {

    public static SSession getSSession(ServletContext context, HttpSession httpSession) {
        if (httpSession == null) {
            return new SSession();
        }

        String sessionKey = "SSESSION_" + httpSession.getId();
        Object existing = context.getAttribute(sessionKey);
        SSession session = (existing instanceof SSession s) ? s : new SSession();

        Enumeration<String> attributeNames = httpSession.getAttributeNames();
        if (attributeNames != null) {
            while (attributeNames.hasMoreElements()) {
                String attributeName = attributeNames.nextElement();
                session.setAttribute(attributeName, httpSession.getAttribute(attributeName));
            }
        }

        context.setAttribute(sessionKey, session);
        return session;
    }

    public static void updateSession(HttpSession httpSession, SSession session) {
        if (httpSession == null || session == null || session.getAttributes() == null) {
            return;
        }

        Enumeration<String> attributeNames = httpSession.getAttributeNames();
        if (attributeNames != null) {
            java.util.List<String> toRemove = java.util.Collections.list(attributeNames);
            for (String key : toRemove) {
                httpSession.removeAttribute(key);
            }
        }

        for (Map.Entry<String, Object> entry : session.getAttributes().entrySet()) {
            httpSession.setAttribute(entry.getKey(), entry.getValue());
        }
    }
}