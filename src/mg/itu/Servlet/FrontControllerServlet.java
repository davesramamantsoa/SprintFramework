package mg.itu.Servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mg.itu.utils.MethodInfo;
import mg.itu.utils.ModelAndView;
import mg.itu.utils.UrlMethod;
import mg.itu.listener.AppListener;
import mg.itu.annotation.RestApi.RestApi;

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

                // Recuperer la methode (avec ou sans req/resp)
                try {
                    method = clazz.getMethod(info.methodName, HttpServletRequest.class,
                            HttpServletResponse.class);
                    result = method.invoke(instance, req, resp);
                } catch (NoSuchMethodException e) {
                    method = clazz.getMethod(info.methodName);
                    result = method.invoke(instance);
                }

                // === Verification de l'annotation @RestApi par reflexion ===
                boolean isRestApi = false;
                if (method != null && method.isAnnotationPresent(RestApi.class)) {
                    isRestApi = true;
                }

                // === Choix : JSON (RestApi) ou HTML (vue) ===
                if (isRestApi) {
                    // Retour JSON via PrintWriter
                    resp.setContentType("application/json");
                    resp.setCharacterEncoding("UTF-8");
                    PrintWriter out = resp.getWriter();

                    if (result == null) {
                        out.println("{\"status\":\"ok\",\"data\":null}");
                    } else if (result instanceof String) {
                        out.println(result.toString());
                    } else {
                        // Conversion simple en JSON pour les types basiques
                        out.println(convertToJson(result));
                    }
                    out.flush();
                    return;

                } else {
                    // Retour HTML classique (ModelAndView ou String => vue)
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

                    // Affichage par defaut
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

    /**
     * Conversion simple en JSON pour les types basiques.
     * Pour des objets complexes, utilisez une librairie (Gson, Jackson, etc.)
     */
    private String convertToJson(Object obj) {
        if (obj == null) {
            return "null";
        }
        if (obj instanceof Number || obj instanceof Boolean) {
            return obj.toString();
        }
        if (obj instanceof String) {
            return "\"" + escapeJson(obj.toString()) + "\"";
        }
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
        // Fallback : toString entre quotes
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