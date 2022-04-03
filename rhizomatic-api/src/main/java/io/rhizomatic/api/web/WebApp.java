package io.rhizomatic.api.web;

import java.nio.file.Path;

import static java.util.Objects.requireNonNull;

/**
 * A web app configuration.
 */
public class WebApp {
    private Path[] contentRoots;
    private String contextPath;

    /**
     * Ctor.
     *
     * @param contextPath the context part of the web app URL relative to the base url
     * @param contentRoots the content roots to serve
     */
    public WebApp(String contextPath, Path... contentRoots) {
        requireNonNull(contextPath, "Context path cannot be null");
        requireNonNull(contentRoots, "Content roots cannot be null");
        this.contentRoots = contentRoots;
        this.contextPath = contextPath;
    }

    public String getContextPath() {
        return contextPath;
    }

    public Path[] getContentRoots() {
        return contentRoots;
    }

}
