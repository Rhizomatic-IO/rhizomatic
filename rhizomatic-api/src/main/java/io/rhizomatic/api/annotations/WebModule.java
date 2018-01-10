package io.rhizomatic.api.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Configures a web module. If more than one module has the same base path, their contents will be concatenated.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.MODULE, ElementType.ANNOTATION_TYPE})
public @interface WebModule {

    /**
     * Configures the base url for the module.
     */
    String value() default "";

    /**
     * Specifies the system profiles the module will be activated for.
     */
    String[] profiles() default "";

    /**
     * Configures the content path. Default is the root directory of the module.
     */
    String content() default "";
}
