package mg.core.framework;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.List;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mg.core.utils.Utils;

public class FrontControllerServlet extends HttpServlet {
    private List<String> listController;


    @Override
    public void init(ServletConfig config) throws ServletException {
        try {
            super.init(config);
            String basePackage = config.getInitParameter("base-package");
            this.listController = Utils.getClassController(basePackage);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        PrintWriter out = response.getWriter();
        out.println("URL : " + uri);
        for (String lc : listController) {
            out.println(lc);
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }
}
