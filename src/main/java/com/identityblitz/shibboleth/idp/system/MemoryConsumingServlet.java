package com.identityblitz.shibboleth.idp.system;

/**
 * User: vkarpov
 * Date: 16.11.11
 */

import edu.internet2.middleware.shibboleth.idp.util.HttpServletHelper;
import org.opensaml.util.storage.StorageService;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;

public class MemoryConsumingServlet extends HttpServlet {

    private static StorageService storageService;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        ServletContext servletContext = config.getServletContext();
        storageService = HttpServletHelper.getStorageService(servletContext);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        PrintWriter out = resp.getWriter();

        out.println("<html>");
        out.println("<head><title>Memory consuming</title></head>");
        out.println("<img src=\"/idp/images/logo.jpg\" />");
        out.println("<h1>Partitions</h1>");
        Iterator<String> itr = storageService.getPartitions();
        if(itr.hasNext()) {
            while(itr.hasNext()) {
                String partition = itr.next();
                out.println("partition: " + partition + ", size = " +
                        getCount(storageService.getKeys(partition)) + "</br>");
            }
        }
        else {
            out.println("No partitions");
        }

        out.println("<h1>Detail information</h1>");
        itr = storageService.getPartitions();
        if(itr.hasNext()) {
            while(itr.hasNext()) {
                String partition = itr.next();
                out.println("<h2>partition: " + partition + "</h2>");

                Iterator<String> keys = storageService.getKeys(partition);
                while (keys.hasNext())
                    out.println("key: " + keys.next() + "</br>");
            }
        }
        else {
            out.println("No information");
        }

        out.println("<body>");
        out.println("</body></html>");
    }

    private static long getCount(Iterator<?> iterator) {
        long count = 0;
        while (iterator.hasNext()) {
            count ++;
            iterator.next();
        }
        return count;
    }
}
