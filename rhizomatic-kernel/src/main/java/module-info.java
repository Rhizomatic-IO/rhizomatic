/**
 * Implements the core Rhizomatic system. Applications may use the exported {@code kernel} package to boot a system. Extension module providers may use the SPI to add additional
 * system functionality.
 */
module io.rhizomatic.kernel {

    requires org.jetbrains.annotations;

    requires io.rhizomatic.api;
    requires java.logging;

    exports io.rhizomatic.kernel;
    exports io.rhizomatic.kernel.spi;
    exports io.rhizomatic.kernel.spi.subsystem;
    exports io.rhizomatic.kernel.spi.inject;
    exports io.rhizomatic.kernel.spi.scan;
    exports io.rhizomatic.kernel.spi.layer;
    exports io.rhizomatic.kernel.spi.reload;
    exports io.rhizomatic.kernel.spi.util;

//    exports io.rhizomatic.kernel.parse;
//    exports io.rhizomatic.kernel.layer;

    uses io.rhizomatic.api.Monitor;
    uses io.rhizomatic.api.SystemDefinition;
    uses io.rhizomatic.kernel.spi.subsystem.Subsystem;
    uses io.rhizomatic.kernel.spi.layer.LayerListener;


}
