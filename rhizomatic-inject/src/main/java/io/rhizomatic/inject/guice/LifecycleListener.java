package io.rhizomatic.inject.guice;

import com.google.inject.TypeLiteral;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
import io.rhizomatic.api.RhizomaticException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Handles invocation of lifecycle methods, e.g. initialization.
 */
public class LifecycleListener implements TypeListener {
    private Map<Class<?>, Method> methodMap;

    public LifecycleListener(Map<Class<?>, Method> methodMap) {
        this.methodMap = methodMap;
    }

    public <I> void hear(final TypeLiteral<I> typeLiteral, TypeEncounter<I> typeEncounter) {
        Method method = methodMap.get(typeLiteral.getRawType());
        if (method == null) {
            return;
        }
        typeEncounter.register((InjectionListener<I>) instance -> {
            method.setAccessible(true);
            try {
                method.invoke(instance);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RhizomaticException(e);
            }

        });
    }

}
