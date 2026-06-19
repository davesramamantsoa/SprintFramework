import com.app.annotations.Controller;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.net.URL;
import java.util.*;

public class FrontControllerServlet extends HttpServlet {
    
    private List<String> nomsClasses = new ArrayList<>();
    
    @Override
    public void init() throws ServletException {
        try {
            String nomPackage = "com.app.controllers";
            String cheminPackage = nomPackage.replace(".", "/");
            
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Enumeration<URL> resources = classLoader.getResources(cheminPackage);
            
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                
                if (resource.getProtocol().equals("file")) {
                    File directory = new File(resource.getFile());
                    
                    if (directory.exists()) {
                        for (File fichier : directory.listFiles()) {
                            if (fichier.getName().endsWith(".class")) {
                                String nomClasse = nomPackage + "." + 
                                    fichier.getName().replace(".class", "");
                                
                                Class<?> clazz = Class.forName(nomClasse);
                                
                                if (clazz.isAnnotationPresent(Controller.class)) {
                                    nomsClasses.add(clazz.getSimpleName());
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        processRequest(request, response);
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        processRequest(request, response);
    }
    
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        
        out.println("<html>");
        out.println("<head><title>Front Controller</title></head>");
        out.println("<body>");
        out.println("<h1>Front Controller</h1>");
       
        out.println("<h2>Classes avec @Controller :</h2>");
        out.println("<ul>");
        
        if (nomsClasses.isEmpty()) {
            out.println("<li>Aucune classe avec @Controller</li>");
        } else {
            for (String nom : nomsClasses) {
                out.println("<li>" + nom + "</li>");
            }
        }
        
        out.println("</ul>");
        out.println("<p>Total: " + nomsClasses.size() + " classe(s)</p>");
        out.println("</body>");
        out.println("</html>");
    }
}