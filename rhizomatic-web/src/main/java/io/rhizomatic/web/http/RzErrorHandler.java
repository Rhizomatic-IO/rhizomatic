package io.rhizomatic.web.http;

import org.eclipse.jetty.server.handler.ErrorHandler;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.Writer;

/**
 * Replaces the default Jetty error handler to return a JSON error containing the HTTP code.
 */
public class RzErrorHandler extends ErrorHandler {

    protected void writeErrorPage(HttpServletRequest request, Writer writer, int code, String message, boolean showStacks) throws IOException {
        writer.write("{ error: '" + code + "'}");
    }
}
