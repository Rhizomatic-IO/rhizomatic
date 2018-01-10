package io.rhizomatic.api.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Denotes a service.
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
public @interface Service {

    /**
     * Specifies the contract types the service is bound to for injection.
     */
    Class<?>[] values() default Void.class;

    /**
     * Specifies the system profiles the service will be activated for.
     */
    String[] profiles() default "";

    /**
     * Specifies ordering when injecting collections of services. Order is descending, i.e. a higher values is placed before a lower value.
     */
    int order() default Integer.MIN_VALUE;
}
