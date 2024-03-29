package io.rhizomatic.api.layer;

import java.nio.file.Path;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * A JPMS module configuration.
 */
public class RzModule {
    private Path location;
    private RzLayer parent;

    public RzModule(Path location) {
        requireNonNull(location, "Layer path was null");
        this.location = location;
    }

    public Path getLocation() {
        return location;
    }

    public RzLayer getParent() {
        return parent;
    }

    void setParent(RzLayer parent) {
        this.parent = parent;
    }

}
