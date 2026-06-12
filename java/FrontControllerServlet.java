import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;

public class FrontControllerServlet extends HttpServlet {
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }
    
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }
    
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String uri = request.getRequestURI();
        PrintWriter out = response.getWriter();
        out.println("<!DOCTYPE html>");
        out.println("<html lang=\"en\">");
        out.println("<head>");
        out.println("<smeta charset=\"UTF-8\">");
        out.println("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
        out.println("<title>Document</title>");
        out.println("</head>");
        out.println("<body>");
        out.println("<h1>Front Controller</h1>");
        out.println("<p>URI:bonjour depuis le controlleur</p>");
        out.println("</body>");
        out.println("</html>");
    }
}