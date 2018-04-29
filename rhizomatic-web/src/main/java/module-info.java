import io.rhizomatic.kernel.spi.subsystem.Subsystem;
import io.rhizomatic.web.WebSubsystem;

/**
 * Provides HTTP, Servlet, Webapp, and REST support.
 */
open module io.rhizomatic.web {
    requires io.rhizomatic.api;
    requires io.rhizomatic.kernel;

    requires java.ws.rs;
    requires javax.servlet.api;
    requires jetty.server;
    requires jetty.util;
    requires jetty.servlet;
    requires jersey.container.servlet.core;
    requires jersey.server;
    requires jersey.common;
    requires javax.annotation.api;  // needed by Jersey
    requires com.fasterxml.jackson.jaxrs.json;
    requires jackson.jaxrs.base;
    requires com.fasterxml.jackson.databind;


    provides Subsystem with WebSubsystem;
}