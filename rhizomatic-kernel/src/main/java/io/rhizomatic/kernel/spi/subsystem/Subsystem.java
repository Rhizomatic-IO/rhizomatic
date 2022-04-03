package io.rhizomatic.kernel.spi.subsystem;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * Defines a subsystem and its lifecycle.
 */
public abstract class Subsystem {
    private String name;

    public Subsystem(String name) {
        requireNonNull(name, "Subsystem name is null");
        this.name = name;
    }

    /**
     * Returns the unique subsystem name.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the names of library modules used by this subsystem that perform reflection on user modules. When loaded into the system,
     * user modules must be opened to these library modules.
     */
    public Set<String> openModulesTo() {
        return Collections.emptySet();
    }

    /**
     * Instantiates the subsystem, registering contained SPI services with the context.
     */
    public void instantiate(SubsystemContext context) {
    }

    /**
     * Assembles services in the subsystem.
     */
    public void assemble(SubsystemContext context) {
    }

    /**
     * Signals the subsystem to perform application initialization, such as publishing endpoints.
     */
    public void applicationInitialize(SubsystemContext context) {
    }

    /**
     * Signals the subsystem to transition services to the ready state so the system can receive requests.
     */
    public void start(SubsystemContext context) {
    }

    /**
     * Signals the subsystem to release open resources and shutdown services.
     */
    public void shutdown() {
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var subsystem = (Subsystem) o;
        return Objects.equals(name, subsystem.name);
    }

    public int hashCode() {
        return Objects.hash(name);
    }
}
