package io.rhizomatic.web.scan;

import io.rhizomatic.kernel.spi.scan.Introspector;
import io.rhizomatic.kernel.spi.scan.ScanIndex;

import javax.servlet.annotation.WebServlet;
import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;
import java.lang.reflect.Modifier;

/**
 * Adds JAX-RS resources and providers to the scan index.
 */
public class WebIntrospector implements Introspector {
    public void introspect(Class<?> type, ScanIndex.Builder builder) {
        if (Modifier.isAbstract(type.getModifiers())) {
            return;
        }

        // check instance if it is a JAX-RS resource, JAX-RS provider or servlet
        var pathAnnotation = type.getAnnotation(Path.class);
        var providerAnnotation = type.getAnnotation(Provider.class);
        var servletAnnotation = type.getAnnotation(WebServlet.class);

        if (pathAnnotation == null && providerAnnotation == null && servletAnnotation == null) {
            return;
        }

        if (pathAnnotation != null) {
            builder.service(type);
            builder.qualified(type, Path.class);
        }

        if (providerAnnotation != null) {
            builder.service(type);
            builder.qualified(type, Provider.class);
        }

        if (servletAnnotation != null) {
            builder.service(type);
            builder.qualified(type, WebServlet.class);
        }
    }
}
