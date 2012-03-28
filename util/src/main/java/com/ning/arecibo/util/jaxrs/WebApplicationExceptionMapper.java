/*
 * Copyright 2010-2012 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.ning.arecibo.util.jaxrs;

import com.google.common.base.Joiner;
import com.sun.jersey.core.spi.factory.ResponseImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class WebApplicationExceptionMapper implements ExceptionMapper<WebApplicationException>
{
    private static final Logger logger = LoggerFactory.getLogger(WebApplicationExceptionMapper.class);

    @Override
    public Response toResponse(final WebApplicationException ex)
    {
        final String warningMsg = buildWarningMessage(ex, "");
        final Response originalResponse = ex.getResponse();

        // TODO: respect the original warning header?
        final Response.ResponseBuilder responseBuilder = Response.fromResponse(originalResponse);
        if (!warningMsg.isEmpty()) {
            responseBuilder.header("Warning", "199 " + warningMsg);
        }
        final Response response = responseBuilder.build();

        String warningHeader = null;
        if (response.getMetadata() != null && response.getMetadata().get("Warning") != null) {
            warningHeader = Joiner.on(",").join(response.getMetadata().get("Warning"));
        }

        if (response instanceof ResponseImpl) {
            if (warningHeader != null && !warningHeader.isEmpty()) {
                logger.error("Response {} ({}): {}", new Object[]{((ResponseImpl) response).getStatusType(), response.getStatus(), warningHeader});
            }
            else {
                logger.error("Response {} ({})", ((ResponseImpl) response).getStatusType(), response.getStatus());
            }
        }
        else {
            if (warningHeader != null) {
                logger.error("Response {}: {}", response.getStatus(), warningHeader);
            }
            else {
                logger.error("Response {}", response.getStatus());
            }
        }

        return response;
    }

    private String buildWarningMessage(final Throwable t, final String prevWarningMessage)
    {
        String newWarningMessage = "";
        // Skip these, not really useful (e.g. 400 requests)
        if (!(t instanceof WebApplicationException)) {
            newWarningMessage = prevWarningMessage + ": " + t.getClass().toString();
        }

        if (t.getCause() != null) {
            return buildWarningMessage(t.getCause(), newWarningMessage);
        }
        else {
            if (t.getMessage() != null) {
                // Truncate cause to avoid headers overflows
                final char[] message = new char[100];
                final char[] src = t.getMessage().toCharArray();
                System.arraycopy(src, 0, message, 0, Math.min(src.length, message.length));
                return newWarningMessage + ": " + new String(message);
            }
            else {
                return newWarningMessage;
            }
        }
    }
}