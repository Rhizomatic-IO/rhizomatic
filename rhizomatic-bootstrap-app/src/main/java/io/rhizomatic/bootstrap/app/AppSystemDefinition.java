package io.rhizomatic.bootstrap.app;

import io.rhizomatic.api.SystemDefinition;
import io.rhizomatic.api.layer.RzLayer;
import io.rhizomatic.api.web.WebApp;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static java.nio.file.Files.newDirectoryStream;

/**
 * Loads all modules (exploded or jar) in the app subdirectory. The bootstrap jar is placed in the root image directory, which is the parent of /app.
 */
public class AppSystemDefinition implements SystemDefinition {
    private List<RzLayer> layers;
    private List<WebApp> webApps = new ArrayList<>();

    public AppSystemDefinition() {
        try {
            var imageDirectory = getImageDirectory(AppSystemDefinition.class);
            var appDir = imageDirectory.resolve("app");

            var layerBuilder = RzLayer.Builder.newInstance("main");
            newDirectoryStream(appDir, Files::isDirectory).forEach(layerBuilder::module);
            layers = List.of(layerBuilder.build());

            var webappDir = imageDirectory.resolve("webapp");
            newDirectoryStream(webappDir, Files::isDirectory).forEach(location -> webApps.add(new WebApp("/" + location.getFileName(), webappDir.resolve(location.getFileName()))));

        } catch (IOException e) {
            throw new RuntimeException("Unable to bootstrap system", e);
        }
    }

    public List<RzLayer> getLayers() {
        return layers;
    }

    public List<WebApp> getWebApps() {
        return webApps;
    }

    public static Path getImageDirectory(Class<?> clazz) throws IllegalStateException {
        // get the name of the Class's bytecode
        var name = clazz.getName();
        int last = name.lastIndexOf('.');
        if (last != -1) {
            name = name.substring(last + 1);
        }
        name = name + ".class";

        // get location of the bytecode - should be a jar: URL
        var url = clazz.getResource(name);
        if (url == null) {
            throw new IllegalStateException("Unable to get location of bytecode resource " + name);
        }

        var jarLocation = url.toString();
        if (!jarLocation.startsWith("jar:")) {
            throw new IllegalStateException("Must be run from a jar: " + url);
        }

        // extract the location of thr jar from the resource URL
        jarLocation = jarLocation.substring(4, jarLocation.lastIndexOf("!/"));
        if (!jarLocation.startsWith("file:")) {
            throw new IllegalStateException("Must be run from a local filesystem: " + jarLocation);
        }

        var jarFile = new File(URI.create(jarLocation));
        return jarFile.getParentFile().toPath();
    }

}
