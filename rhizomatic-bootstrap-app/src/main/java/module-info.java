import io.rhizomatic.bootstrap.app.AppSystemDefinition;
import io.rhizomatic.api.SystemDefinition;

/**
 * Bootstraps a production system from the current project layout.
 */
module io.rhizomatic.bootstrap.production {
    requires io.rhizomatic.api;

    provides SystemDefinition with AppSystemDefinition;
}