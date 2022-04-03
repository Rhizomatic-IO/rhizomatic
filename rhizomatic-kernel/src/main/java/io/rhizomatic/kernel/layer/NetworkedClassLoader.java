package io.rhizomatic.kernel.layer;

import java.io.IOException;
import java.net.URL;
import java.security.SecureClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A classloader that implements a multi-parent network and delegates exclusively to its parents.
 */
public class NetworkedClassLoader extends SecureClassLoader {
    private final List<ClassLoader> parents = new ArrayList<>();

    /**
     * Constructs a classloader with a set of resources and a single parent.
     *
     * @param name a name used to identify this classloader
     * @param parent the initial parent
     */
    public NetworkedClassLoader(String name, ClassLoader parent, Set<ClassLoader> parents) {
        super(name, parent);
        if (parent == null) {
            throw new IllegalArgumentException("Parent classloader cannot be null");
        }
        this.parents.addAll(parents);
    }

    public URL findResource(String name) {
        // look in parents
        for (var parent : parents) {
            URL resource = parent.getResource(name);
            if (resource != null) {
                return resource;
            }
        }
        return null;
    }

    public Enumeration<URL> findResources(String name) throws IOException {
        // LinkedHashSet because we want all resources in the order found but no duplicates
        var resources = new LinkedHashSet<URL>();
        for (var parent : parents) {
            var parentResources = parent.getResources(name);
            while (parentResources.hasMoreElements()) {
                resources.add(parentResources.nextElement());
            }
        }
        var currentResources = super.findResources(name);
        while (currentResources.hasMoreElements()) {
            resources.add(currentResources.nextElement());
        }
        return Collections.enumeration(resources);
    }

    public String toString() {
        return getName();
    }

    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        // look for previously loaded classes
        var clazz = findLoadedClass(name);
        if (clazz == null) {
            // look in the primary parent
            try {
                clazz = Class.forName(name, resolve, getParent());
            } catch (ClassNotFoundException e) {
                // continue
            }
            if (clazz == null) {
                // look in other parents
                for (var parent : parents) {
                    try {
                        clazz = parent.loadClass(name);
                        break;
                    } catch (ClassNotFoundException e) {
                        //noinspection UnnecessaryContinue
                        continue;
                    }
                }
            }
            if (clazz == null) {
                throw new ClassNotFoundException(name);
            }
        }
        if (resolve) {
            resolveClass(clazz);
        }
        return clazz;
    }

}
