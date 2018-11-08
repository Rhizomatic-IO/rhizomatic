package io.rhizomatic.web.http;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.HandlerWrapper;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Provides support for SPAs such as Angular that require app URLs to be re-written to index.html. Rewrites all paths except those containing ".", which point to resource
 * files such as Javascript, CSS, and images.
 */
public class RewriteHandler extends HandlerWrapper {

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        target = calculateTarget(target);
        baseRequest.setPathInfo(target);
        super.handle(target, baseRequest, request, response);
    }

    private String calculateTarget(String target) {
        int pos = target.lastIndexOf(".");
        if (pos > 1 && pos < target.length() - 1) {
            return target;
        }
        return "/index.html";
    }
}
