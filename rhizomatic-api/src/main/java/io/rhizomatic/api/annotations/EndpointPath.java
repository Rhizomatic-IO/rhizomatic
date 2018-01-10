package io.rhizomatic.api.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Configures the base path for endpoints such as servlets and REST resources in a module. If more than one module has the same base path, their contents will be concatenated.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.MODULE, ElementType.ANNOTATION_TYPE})
public @interface EndpointPath {
    String value();
}
