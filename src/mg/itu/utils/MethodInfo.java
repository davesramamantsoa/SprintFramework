package mg.itu.utils;

public class MethodInfo {
    public static final String SESSION_USER = "user";
    public static final String SESSION_ROLE = "role";

    public String className;
    public String methodName;

    public MethodInfo(String className, String methodName) {
        this.className = className;
        this.methodName = methodName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public static boolean hasRole(Object user, String requiredRole) {
        if (requiredRole == null || requiredRole.trim().isEmpty())
            return true; 
        if (user == null)
            return false;

        String userRole = resolveUserRole(user);
        if (userRole == null)
            return false;

        String needed = requiredRole.trim().toUpperCase();
        for (String part : needed.split(",")) {
            if (part.trim().isEmpty()) continue;
            if (part.trim().equals(userRole.toUpperCase())) return true;
        }
        return false;
    }

   
    public static String resolveUserRole(Object user) {
        if (user == null) return null;

    
        try {
            java.lang.reflect.Method m = user.getClass().getMethod("getRole");
            Object r = m.invoke(user);
            if (r != null) return r.toString();
        } catch (Exception ignored) { }
        if (user instanceof String) return (String) user;

        try {
            if (Session.getGlobalSessionMap() != null) {
                Object r = Session.getGlobalSessionMap().get(SESSION_ROLE);
                if (r != null) return r.toString();
            }
        } catch (Throwable ignored) { }

        return null;
    }
}