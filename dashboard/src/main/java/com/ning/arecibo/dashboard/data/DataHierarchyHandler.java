package com.ning.arecibo.dashboard.data;

import java.util.List;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;

public interface DataHierarchyHandler {
    public String getDataHierarchyResponse(HttpServletRequest request, List<String> tokens) throws IOException;
}
