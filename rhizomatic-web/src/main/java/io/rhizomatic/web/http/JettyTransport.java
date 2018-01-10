package io.rhizomatic.web.http;

import io.rhizomatic.api.Monitor;
import io.rhizomatic.kernel.spi.SystemConfiguration;
import io.rhizomatic.kernel.spi.subsystem.SubsystemContext;
import org.eclipse.jetty.server.Server;

/**
 * Provides HTTP communication to the system via Jetty.
 */
public class JettyTransport {
    @SystemConfiguration
    private static final String HTTP_PORT = "http.port";
    @SystemConfiguration
    private static final String HTTPS_PORT = "https.port";
    @SystemConfiguration
    private static final String HTTPS_ENABLED = "https.enabled";

    private static final String LOG_CLASS = "org.eclipse.jetty.util.log.class";
    private static final String ANNOUNCE = "org.eclipse.jetty.util.log.announce";

    private int httpPort;

    private Server server;
    private Monitor monitor;

    public JettyTransport() {
        System.setProperty(LOG_CLASS, RzJettyLogger.class.getName());
        System.setProperty(ANNOUNCE, "false");
    }

    public void initialize(SubsystemContext context) {
        httpPort = getHttp(context);
        server = new Server(httpPort);
    }

    public void start(SubsystemContext context) {
        monitor = context.getMonitor();
        RzJettyLogger.MONITOR = monitor;


        server.setErrorHandler(new RzErrorHandler());
        try {
            server.start();

            monitor.info(() -> "Listening on HTTP " + httpPort);
        } catch (Exception e) {
            monitor.severe(() -> "Error starting HTTP transport", e);
        }
    }

    public void shutdown() {
        if (server == null) {
            return;
        }
        try {
            server.stop();
        } catch (Exception e) {
            monitor.severe(() -> "Error stopping HTTP transport", e);
        }
    }

    public Server getServer() {
        return server;
    }

    private int getHttp(SubsystemContext context) {
        Integer httpPort = context.getConfiguration(Integer.class, HTTP_PORT);
        if (httpPort == null) {
            httpPort = 8080;
        }
        return httpPort;
    }

}
