package com.ning.arecibo.dashboard.data;

import java.io.IOException;
import java.io.Writer;
import java.util.StringTokenizer;
import java.util.List;
import java.util.ArrayList;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.ning.arecibo.dashboard.data.v1.V1DataHierarchyHandler;
import static javax.servlet.http.HttpServletResponse.*;

import com.ning.arecibo.util.Logger;

public class DataHierarchyServlet extends HttpServlet {

    private final static Logger log = Logger.getLogger(DataHierarchyServlet.class);

    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        try {

            log.debug("requestUri = %s",request.getRequestURI());

            StringTokenizer st = new StringTokenizer(request.getRequestURI(),"/");

            // skip endpoint name
            if(!skipEndPoint(response,st))
                return;

            // get version
            HierarchyVersion version = parseVersion(response,st);
            if(version == null)
                return;

            // parse tokens into an ordered list
            List<String> tokens = parseRemainingTokensToList(st);

            // get handler
            // TODO: make this a factory, or some such
            DataHierarchyHandler handler;
            if(version.equals(HierarchyVersion.v1))
                handler = new V1DataHierarchyHandler();
            else
                handler = null;

            // get dataResponse
            String dataResponse;
            if(handler != null)
                dataResponse = handler.getDataHierarchyResponse(request,tokens);
            else
                dataResponse = null;

            // send response
            response.setContentType("text/plain");
            Writer writer = response.getWriter();

            if(dataResponse != null)
                writer.write(dataResponse);

            writer.flush();
            writer.close();
        }
        catch(RuntimeException ruEx) {
            log.warn(ruEx);
            response.sendError(SC_BAD_REQUEST,ruEx.getMessage());
        }
    }

    private boolean skipEndPoint(HttpServletResponse response,StringTokenizer st)
            throws IOException {

        // skip endpoint name
        if(st.hasMoreTokens()) {
            String endpointName = st.nextToken();
            log.debug("got endpointName: %s",endpointName);
            return true;
        }
        else {
            response.sendError(SC_BAD_REQUEST,"Missing request URI");
            return false;
        }
    }

    private HierarchyVersion parseVersion(HttpServletResponse response,StringTokenizer st)
            throws IOException {
        // get version
        HierarchyVersion version;
        String versionToken = null;
        try {
            if (st.hasMoreTokens()) {
                versionToken = st.nextToken();
                log.debug("got version: %s", versionToken);

                // check the version
                return(HierarchyVersion.valueOf(versionToken));
            }
            else {
                response.sendError(SC_BAD_REQUEST, "Missing version specification");
                return null;
            }
        }
        catch(RuntimeException ruEx) {
            response.sendError(SC_BAD_REQUEST, "Couldn't parse version specifier: '" + versionToken + "'" +
                                                ", allowed versions are: " + HierarchyVersion.getValueList());
            return null;
        }
    }

    private List<String> parseRemainingTokensToList(StringTokenizer st) {

        List<String> list = new ArrayList<String>();
        while(st.hasMoreTokens()) {
            list.add(st.nextToken());
        }

        return list;
    }
}
