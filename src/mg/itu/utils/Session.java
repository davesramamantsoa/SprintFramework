package mg.itu.utils;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpSession;

public class Session {

    private static ServletContext servletContext;
    private static Map<String, Object> globalSessionMap;

    private Map<String, Object> sessionMap;

    public Session() {
        this.sessionMap = new HashMap<>();
    }

    // Appele par AppListener au demarrage
    public static void init(ServletContext context, Map<String, Object> sessionMap) {
        servletContext = context;
        globalSessionMap = sessionMap;
        System.out.println("[Framework] Session initialisee avec Map globale");
    }

    public static ServletContext getServletContext() {
        return servletContext;
    }

    public static Map<String, Object> getGlobalSessionMap() {
        return globalSessionMap;
    }

    // Charge depuis la HttpSession
    public void load(HttpSession httpSession) {
        sessionMap.clear();
        Enumeration<String> names = httpSession.getAttributeNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            sessionMap.put(name, httpSession.getAttribute(name));
        }
        // Sync avec la Map globale
        if (globalSessionMap != null) {
            globalSessionMap.clear();
            globalSessionMap.putAll(sessionMap);
        }
    }

    // Ecrit dans la HttpSession
    public void commit(HttpSession httpSession) {
        Enumeration<String> names = httpSession.getAttributeNames();
        while (names.hasMoreElements()) {
            httpSession.removeAttribute(names.nextElement());
        }
        for (Map.Entry<String, Object> e : sessionMap.entrySet()) {
            httpSession.setAttribute(e.getKey(), e.getValue());
        }
        // Sync avec la Map globale
        if (globalSessionMap != null) {
            globalSessionMap.clear();
            globalSessionMap.putAll(sessionMap);
        }
    }

    public void setAttribute(String name, Object value) {
        sessionMap.put(name, value);
    }

    public Object getAttribute(String name) {
        return sessionMap.get(name);
    }

    public void removeAttribute(String name) {
        sessionMap.remove(name);
    }

    public Map<String, Object> getSessionMap() {
        return sessionMap;
    }

    public void setSessionMap(Map<String, Object> sessionMap) {
        this.sessionMap = sessionMap;
    }
}