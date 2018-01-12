/**
 * The Rhizomatic API.
 *
 * The annotations package contains annotations for configuring module types and service implementations.
 *
 * The layer package contains the model and classes for building layer configurations.
 *
 * {@link io.rhizomatic.api.SystemDefinition} is used by bootstrap modules to configure a system. Bootstrap modules (or library modules) may optionally supply a
 * {@link io.rhizomatic.api.Monitor} implementation using a JPMS ServiceLoader provider to capture and report log and other system events.
 */
module io.rhizomatic.api {

    exports io.rhizomatic.api;
    exports io.rhizomatic.api.annotations;
    exports io.rhizomatic.api.layer;
    exports io.rhizomatic.api.web;

}