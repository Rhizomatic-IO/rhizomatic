package io.rhizomatic.api.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Configures a service module. This annotation is optional as unannotated modules are assumed to be service modules.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.MODULE, ElementType.ANNOTATION_TYPE})
public @interface ServiceModule {

    /**
     * Specifies the system profiles the module will be activated for.
     */
    String[] profiles() default "";

}
