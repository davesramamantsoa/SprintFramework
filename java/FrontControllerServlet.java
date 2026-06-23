package com.app.annotations;
import com.app.annotations.Controller;
import com.app.annotations.URLMapping;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.lang.reflect.Method;

public class FrontControllerServlet extends HttpServlet {
    
    private List<String> nomsClasses = new ArrayList<>();
    Map<String, Method> mapMethodes = new HashMap<>();    
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
                                    for (Method method : clazz.getDeclaredMethods()) {
                                        if (method.isAnnotationPresent(URLMapping.class)) {
                                            String urlMapping = method.getAnnotation(URLMapping.class).value();
                                            mapMethodes.put(urlMapping, method);
                                        }
                                    }
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
    

    String urlDemandee = request.getPathInfo();
    
    out.println("<html>");
    out.println("<body>");

   
    if (urlDemandee != null && mapMethodes.containsKey(urlDemandee)) {
        
        Method methodeCible = mapMethodes.get(urlDemandee);
        Class<?> classeParente = methodeCible.getDeclaringClass();
        
        out.println("Classe : " + classeParente.getSimpleName() + "<br>");
        out.println("Methode : " + methodeCible.getName() + "()");
        
        try {
            Object instanceControleur = classeParente.getDeclaredConstructor().newInstance();
            methodeCible.invoke(instanceControleur);
        } catch (Exception e) {
          
        }

    }else {
        out.println("classe et methode disponible : <br>");
    
        for (String urlKey : mapMethodes.keySet()) {
            
            
            Method methode = mapMethodes.get(urlKey);
            Class<?> classeParente = methode.getDeclaringClass();
            
            out.println("Classe : " + classeParente.getSimpleName() + " / ");
            out.println("Methode : " + methode.getName() + "()<br>");
        }
    
    out.println("</body>");
    out.println("</html>");
}
}
}