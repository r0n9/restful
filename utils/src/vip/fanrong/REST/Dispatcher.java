package vip.fanrong.REST;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.UnknownHostException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import vip.fanrong.REST.REST.DispatcherException;

/**
 * Default dispatcher implementation.
 * 
 * @author Mike Banos
 *
 */
public class Dispatcher extends HttpServlet {
    /**
     * Default constructor.
     */
    public Dispatcher() {
        super();
    }

    /**
     * Default post request handler.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }

    /**
     * Default get request handler.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        setBufferSize(response);

        response.setStatus(HTTP_STATUS_200);
        response.setContentType(JSON_CONTENT_TYPE);
        setHeader(response);

        try {
            RESTResult value = dispatch(request, response);
            JsonOutputUtil.outputObject(response.getOutputStream(), value, RESTResult.class);
        } catch (DispatcherException e) {
            sendError(e, response.getOutputStream(), response);
        } catch (Exception e) {
            sendException(e, response.getOutputStream(), response);
        } finally {
            response.getOutputStream().close();
        }
    }

    /**
     * Set the response header. By default, nothing is changed.
     * 
     * @param response The servlet response whose header to set.
     */
    protected void setHeader(HttpServletResponse response) {
        try {
            response.setHeader("X-Content-Pipeline",
                    java.net.InetAddress.getLocalHost().getHostName());
        } catch (UnknownHostException e) {
            log.error("Error retrieving local host information.", e);
        }
    }

    /**
     * Send a REST error.
     * 
     * @param e The caught exception.
     * @param os The output stream to write to.
     * @param response The servlet response.
     */
    private void sendError(DispatcherException e, ServletOutputStream os,
                           HttpServletResponse response) {
        log.warn("DispatcherException", e);

        response.setStatus(HTTP_STATUS_500);
        REST.sendError(os, e);
    }

    /**
     * Send a REST error.
     * 
     * @param e The caught exception.
     * @param os The output stream to write to.
     * @param response The servlet response.
     */
    private void sendException(Exception e, ServletOutputStream os, HttpServletResponse response) {
        log.warn("Exception Propogated", e);

        response.setStatus(HTTP_STATUS_500);
        REST.sendException(os, e);
    }


    /**
     * Dispatch a request.
     * 
     * @param os The output stream to write to.
     * @param request The servlet request to dispatch.
     * @return The corresponding REST result.
     * @throws DispatcherException
     * @throws IOException
     */
    protected RESTResult dispatch(HttpServletRequest request, HttpServletResponse response)
            throws DispatcherException, IOException {
        return REST.dispatch(response, request.getServletPath() + request.getPathInfo(),
                request.getQueryString(), getRequestBody(request));
    }


    /**
     * Set the buffer sieze for a response.
     * 
     * @param response The response to set a buffer size for.
     */
    protected void setBufferSize(HttpServletResponse response) {
        response.setBufferSize(64 * 1024);
    }

    // TODO: put in util class?
    protected String getRequestBody(HttpServletRequest request) throws IOException {
        StringBuffer jb = new StringBuffer();
        String line = null;

        try {
            BufferedReader reader = request.getReader();
            while ((line = reader.readLine()) != null)
                jb.append(line);
        } catch (IOException e) {
            log.error("Error reading HTTP request.", e);

            throw e;
        }

        return jb.toString();
    }

    /**
     * Auto-generated version id.
     */
    private static final long serialVersionUID = 1655097415152084262L;

    protected static final int HTTP_STATUS_200 = 200;
    protected static final int HTTP_STATUS_500 = 500;

    protected final static String XML_CONTENT_TYPE = "text/xml";
    protected final static String JSON_CONTENT_TYPE = "application/json";
    protected final static String HTML_CONTENT_TYPE = "text/html";

    // TODO: these should probably be somewhere else
    protected final static String XML = "XML";
    protected final static String JSON = "JSON";
    protected final static String HTML = "HTML";
    protected final static String RESPONSE_TYPE_PARAM = "responseType";

    private static final Logger log = LogManager.getLogger(Dispatcher.class);
}
