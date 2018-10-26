package io.rhizomatic.bootstrap.app;

import io.rhizomatic.api.SystemDefinition;
import io.rhizomatic.api.layer.RzLayer;
import io.rhizomatic.api.web.WebApp;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.newDirectoryStream;

/**
 * Loads all modules (exploded or jar) in the app subdirectory. The bootstrap jar is placed in the root image directory, which is the parent of /app.
 */
public class AppSystemDefinition implements SystemDefinition {
    private List<RzLayer> layers;
    private List<WebApp> webApps = new ArrayList<>();

    public AppSystemDefinition() {
        try {
            Path imageDirectory = getImageDirectory(AppSystemDefinition.class);
            Path appDir = imageDirectory.resolve("app");

            RzLayer.Builder layerBuilder = RzLayer.Builder.newInstance("main");
            newDirectoryStream(appDir, p -> isDirectory(p)).forEach(layerBuilder::module);
            layers = List.of(layerBuilder.build());

            Path webappDir = imageDirectory.resolve("webapp");
            newDirectoryStream(webappDir, p -> isDirectory(p)).forEach(location -> webApps.add(new WebApp("/" + location.getFileName(), webappDir.resolve(location.getFileName()))));

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
        String name = clazz.getName();
        int last = name.lastIndexOf('.');
        if (last != -1) {
            name = name.substring(last + 1);
        }
        name = name + ".class";

        // get location of the bytecode - should be a jar: URL
        URL url = clazz.getResource(name);
        if (url == null) {
            throw new IllegalStateException("Unable to get location of bytecode resource " + name);
        }

        String jarLocation = url.toString();
        if (!jarLocation.startsWith("jar:")) {
            throw new IllegalStateException("Must be run from a jar: " + url);
        }

        // extract the location of thr jar from the resource URL
        jarLocation = jarLocation.substring(4, jarLocation.lastIndexOf("!/"));
        if (!jarLocation.startsWith("file:")) {
            throw new IllegalStateException("Must be run from a local filesystem: " + jarLocation);
        }

        File jarFile = new File(URI.create(jarLocation));
        return jarFile.getParentFile().toPath();
    }

}
