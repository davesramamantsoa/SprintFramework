package mg.itu.Servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import mg.itu.utils.MapFile;
import mg.itu.utils.MethodInfo;
import mg.itu.utils.ModelAndView;
import mg.itu.utils.UrlMethod;
import mg.itu.listener.AppListener;
import mg.itu.annotation.RestApi.RestApi;

/**
 * MultipartConfig : indispensable pour que req.getPart(...) fonctionne,
 * quelle que soit l'extension du fichier envoye (pdf, docx, zip, jpg, mp4...).
 */
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,       // 1 MB en memoire, au-dela -> disque
        maxFileSize       = 50L * 1024 * 1024, // 50 MB par fichier
        maxRequestSize    = 100L * 1024 * 1024 // 100 MB par requete
)
public class FrontControllerServlet extends HttpServlet {

    private static Set<String> uris = new HashSet<>();

    @Override
    public void init() throws ServletException {
        System.out.println("[Framework] FrontControllerServlet initialise");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        processRequest(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        processRequest(req, resp);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        processRequest(req, resp);
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        processRequest(req, resp);
    }

    protected void processRequest(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        if (AppListener.initError != null) {
            throw new ServletException(AppListener.initError);
        }

        String uri = req.getRequestURI();
        String contextPath = req.getContextPath();
        String chemin = uri.substring(contextPath.length());
        String httpMethod = req.getMethod();

        uris.add(chemin);

        UrlMethod key = new UrlMethod(chemin, httpMethod);

        if (AppListener.urlMethodMappings.containsKey(key)) {
            MethodInfo info = AppListener.urlMethodMappings.get(key);
            String simpleName = info.className.substring(info.className.lastIndexOf('.') + 1);

            try {
                Class<?> clazz = Class.forName(info.className);
                Object instance = clazz.getDeclaredConstructor().newInstance();

                Object result = null;
                Method method = null;

                // === 1. Recuperer la methode et construire les arguments (binding) ===
                try {
                    method = clazz.getMethod(info.methodName, HttpServletRequest.class,
                            HttpServletResponse.class);
                    result = method.invoke(instance, req, resp);
                } catch (NoSuchMethodException e) {
                    for (Method m : clazz.getMethods()) {
                        if (!m.getName().equals(info.methodName)) continue;
                        method = m;
                        break;
                    }
                    if (method == null) throw new NoSuchMethodException(info.methodName);

                    Object[] args = buildArgs(method, req, resp);
                    result = method.invoke(instance, args);
                }

                // === 2. Verification de l'annotation @RestApi par reflexion ===
                boolean isRestApi = false;
                if (method != null && method.isAnnotationPresent(RestApi.class)) {
                    isRestApi = true;
                }

                // === 3. Choix : JSON (RestApi) ou HTML (vue) ===
                if (isRestApi) {
                    resp.setContentType("application/json");
                    resp.setCharacterEncoding("UTF-8");
                    PrintWriter out = resp.getWriter();

                    if (result == null) {
                        out.println("{\"status\":\"ok\",\"data\":null}");
                    } else if (result instanceof String) {
                        out.println(result.toString());
                    } else {
                        out.println(convertToJson(result));
                    }
                    out.flush();
                    return;

                } else {
                    if (result != null && result instanceof ModelAndView) {
                        ModelAndView mv = (ModelAndView) result;

                        if (mv.getData() != null) {
                            for (Map.Entry<String, Object> entry : mv.getData().entrySet()) {
                                req.setAttribute(entry.getKey(), entry.getValue());
                            }
                        }

                        String viewPath = AppListener.viewPrefix + mv.getViewName()
                                + AppListener.viewSuffix;
                        RequestDispatcher dispatcher = req.getRequestDispatcher(viewPath);
                        dispatcher.forward(req, resp);
                        return;
                    }

                    if (result != null && result instanceof String) {
                        String viewPath = AppListener.viewPrefix + result.toString()
                                + AppListener.viewSuffix;
                        RequestDispatcher dispatcher = req.getRequestDispatcher(viewPath);
                        dispatcher.forward(req, resp);
                        return;
                    }

                    resp.setContentType("text/html");
                    PrintWriter out = resp.getWriter();
                    out.println("<html><body>");
                    out.println("<h1>URL: " + chemin + "</h1>");
                    out.println("<p>Methode HTTP: " + httpMethod + "</p>");
                    out.println("<p>Classe: " + simpleName + "</p>");
                    out.println("<p>Methode: " + info.methodName + "</p>");
                    if (result != null) {
                        out.println("<p>Retour: " + result.toString() + "</p>");
                    }
                    out.println("</body></html>");
                }

            } catch (Exception e) {
                throw new ServletException(e);
            }
        } else {
            resp.setContentType("text/html");
            PrintWriter out = resp.getWriter();
            out.println("<html><body>");
            out.println("<h1>404 - Aucun mapping pour: " + httpMethod + " " + chemin + "</h1>");
            out.println("<h2>Mappings disponibles</h2><ul>");
            for (Map.Entry<UrlMethod, MethodInfo> entry : AppListener.urlMethodMappings.entrySet()) {
                String simpleName = entry.getValue().className
                        .substring(entry.getValue().className.lastIndexOf('.') + 1);
                out.println(
                        "<li>" + entry.getKey().getMethod() + " " + entry.getKey().getUrl() + " -> " +
                                simpleName + "." + entry.getValue().methodName + "</li>");
            }
            out.println("</ul>");
            out.println("</body></html>");
        }
    }

    // ============================================================
    // BINDING DES ARGUMENTS (avec support MapFile / upload)
    // ============================================================

    /**
     * Construit le tableau d'arguments d'une methode controleur.
     * Supporte :
     *  - HttpServletRequest / HttpServletResponse (injectes)
     *  - primitifs, wrappers, String, LocalDate, LocalDateTime, enum
     *  - tableaux (T[])
     *  - collections (List<T>, Set<T>...)
     *  - MapFile (fichier multipart) / List<MapFile> / MapFile[]
     */
    private Object[] buildArgs(Method method, HttpServletRequest req, HttpServletResponse resp)
            throws Exception {

        Parameter[] params = method.getParameters();
        Object[] args = new Object[params.length];

        for (int i = 0; i < params.length; i++) {
            Parameter p = params[i];
            Class<?> type = p.getType();

            // Injection directe
            if (HttpServletRequest.class.isAssignableFrom(type))  { args[i] = req;  continue; }
            if (HttpServletResponse.class.isAssignableFrom(type)) { args[i] = resp; continue; }

            String name = p.getName();

            // === UPLOAD simple : MapFile ===
            if (MapFile.class.isAssignableFrom(type)) {
                args[i] = readMapFile(req, name);
                continue;
            }

            // === Collection (List/Set) ===
            if (Collection.class.isAssignableFrom(type)) {
                Class<?> elemType = String.class;
                Type generic = p.getParameterizedType();
                if (generic instanceof ParameterizedType) {
                    Type[] targs = ((ParameterizedType) generic).getActualTypeArguments();
                    if (targs.length > 0 && targs[0] instanceof Class) {
                        elemType = (Class<?>) targs[0];
                    }
                }

                // List<MapFile> : multi-upload
                if (MapFile.class.isAssignableFrom(elemType)) {
                    List<Object> files = new ArrayList<>();
                    for (Part part : req.getParts()) {
                        if (part.getName().equals(name) && part.getSize() > 0) {
                            MapFile mf = MapFile.fromPart(part);
                            if (mf != null) files.add(mf);
                        }
                    }
                    args[i] = files;
                    continue;
                }

                // Collection classique
                String[] vals = req.getParameterValues(name);
                List<Object> list = new ArrayList<>();
                if (vals != null) {
                    for (String v : vals) list.add(convert(elemType, v));
                }
                if (type == List.class || type.isAssignableFrom(List.class)) {
                    args[i] = list;
                } else {
                    try {
                        @SuppressWarnings("unchecked")
                        Collection<Object> coll = (Collection<Object>)
                                type.getDeclaredConstructor().newInstance();
                        coll.addAll(list);
                        args[i] = coll;
                    } catch (Exception e) {
                        args[i] = list;
                    }
                }
                continue;
            }

            // === Tableau ===
            if (type.isArray()) {
                // MapFile[] : multi-upload
                if (MapFile.class.isAssignableFrom(type.getComponentType())) {
                    List<Object> files = new ArrayList<>();
                    for (Part part : req.getParts()) {
                        if (part.getName().equals(name) && part.getSize() > 0) {
                            MapFile mf = MapFile.fromPart(part);
                            if (mf != null) files.add(mf);
                        }
                    }
                    Object arr = Array.newInstance(type.getComponentType(), files.size());
                    for (int j = 0; j < files.size(); j++) Array.set(arr, j, files.get(j));
                    args[i] = arr;
                    continue;
                }

                String[] vals = req.getParameterValues(name);
                if (vals == null) { args[i] = null; continue; }
                Object arr = Array.newInstance(type.getComponentType(), vals.length);
                for (int j = 0; j < vals.length; j++) {
                    Array.set(arr, j, convert(type.getComponentType(), vals[j]));
                }
                args[i] = arr;
                continue;
            }

            // === Type simple ===
            if (isSimpleType(type)) {
                String val = req.getParameter(name);
                args[i] = val == null ? defaultFor(type) : convert(type, val);
                continue;
            }

            // Fallback
            args[i] = req.getParameter(name);
        }
        return args;
    }

    /**
     * Recupere un fichier uploade depuis la requete multipart sous forme de MapFile.
     * Retourne null si aucun fichier (ou fichier vide).
     */
    private MapFile readMapFile(HttpServletRequest req, String fieldName) throws Exception {
        String contentType = req.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("multipart/")) {
            return null;
        }
        Part part = req.getPart(fieldName);
        return MapFile.fromPart(part);
    }

    @SuppressWarnings("unchecked")
    private Object convert(Class<?> type, String value) {
        if (value == null) return defaultFor(type);
        if (type == String.class) return value;

        if (type == int.class     || type == Integer.class) return Integer.parseInt(value);
        if (type == long.class    || type == Long.class)    return Long.parseLong(value);
        if (type == double.class  || type == Double.class)  return Double.parseDouble(value);
        if (type == float.class   || type == Float.class)   return Float.parseFloat(value);
        if (type == boolean.class || type == Boolean.class) return Boolean.parseBoolean(value);
        if (type == byte.class    || type == Byte.class)    return Byte.parseByte(value);
        if (type == short.class   || type == Short.class)   return Short.parseShort(value);
        if (type == char.class    || type == Character.class)
            return value.isEmpty() ? '\0' : value.charAt(0);

        if (type == LocalDate.class)     return LocalDate.parse(value);
        if (type == LocalDateTime.class) return LocalDateTime.parse(value);

        if (type.isEnum()) return Enum.valueOf((Class<? extends Enum>) type, value);

        return value;
    }

    private boolean isSimpleType(Class<?> t) {
        return t == String.class
                || t == int.class     || t == Integer.class
                || t == long.class    || t == Long.class
                || t == double.class  || t == Double.class
                || t == float.class   || t == Float.class
                || t == boolean.class || t == Boolean.class
                || t == byte.class    || t == Byte.class
                || t == short.class   || t == Short.class
                || t == char.class    || t == Character.class
                || t == LocalDate.class || t == LocalDateTime.class
                || t.isEnum();
    }

    private Object defaultFor(Class<?> t) {
        if (t == int.class)     return 0;
        if (t == long.class)    return 0L;
        if (t == double.class)  return 0.0;
        if (t == float.class)   return 0.0f;
        if (t == boolean.class) return false;
        if (t == byte.class)    return (byte) 0;
        if (t == short.class)   return (short) 0;
        if (t == char.class)    return '\0';
        return null;
    }

    // ============================================================
    // JSON (avec support MapFile)
    // ============================================================

    private String convertToJson(Object obj) {
        if (obj == null) return "null";

        // MapFile -> JSON descriptif
        if (obj instanceof MapFile) {
            MapFile f = (MapFile) obj;
            return "{\"fileName\":\"" + escapeJson(f.getFileName()) + "\","
                 + "\"extension\":\"" + escapeJson(f.getExtension()) + "\","
                 + "\"size\":" + (f.getBytes() == null ? 0 : f.getBytes().length) + ","
                 + "\"encodedFileName\":\"" + escapeJson(f.getEncodedFileName()) + "\"}";
        }

        if (obj instanceof Number || obj instanceof Boolean) return obj.toString();
        if (obj instanceof String) return "\"" + escapeJson(obj.toString()) + "\"";

        if (obj instanceof Map) {
            StringBuilder sb = new StringBuilder("{");
            Map<?, ?> map = (Map<?, ?>) obj;
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) sb.append(",");
                sb.append("\"").append(escapeJson(String.valueOf(entry.getKey()))).append("\":")
                  .append(convertToJson(entry.getValue()));
                first = false;
            }
            sb.append("}");
            return sb.toString();
        }

        if (obj instanceof Iterable) {
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object item : (Iterable<?>) obj) {
                if (!first) sb.append(",");
                sb.append(convertToJson(item));
                first = false;
            }
            sb.append("]");
            return sb.toString();
        }

        return "\"" + escapeJson(obj.toString()) + "\"";
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }
}