package io.rhizomatic.kernel.layer;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Intended for use when networked layers are fully implemented.
 */
public class NetworkedURLClassLoader extends URLClassLoader {
    private final List<ClassLoader> parents = new CopyOnWriteArrayList<>();

    /**
     * Constructs a classloader with a set of resources and a single parent.
     *
     * @param name a name used to identify this classloader
     * @param urls the URLs from which to load classes and resources
     * @param parent the initial parent
     */
    public NetworkedURLClassLoader(String name, URL[] urls, ClassLoader parent) {
        super(name, urls, parent);
        if (parent == null) {
            throw new IllegalArgumentException("Parent classloader cannot be null");
        }
    }

    /**
     * Add a resource URL to the classloader.
     */
    public void addURL(URL url) {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkCreateClassLoader();
        }
        super.addURL(url);
    }

    /**
     * Add a parent to the classloader.
     */
    public void addParent(ClassLoader parent) {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkCreateClassLoader();
        }
        if (parent != null) {
            parents.add(parent);
        }
    }

    public URL findResource(String name) {
        // look in parents
        for (ClassLoader parent : parents) {
            URL resource = parent.getResource(name);
            if (resource != null) {
                return resource;
            }
        }
        // look in the classloader
        return super.findResource(name);
    }

    public Enumeration<URL> findResources(String name) throws IOException {
        // LinkedHashSet because we want all resources in the order found but no duplicates
        Set<URL> resources = new LinkedHashSet<>();
        for (ClassLoader parent : parents) {
            Enumeration<URL> parentResources = parent.getResources(name);
            while (parentResources.hasMoreElements()) {
                resources.add(parentResources.nextElement());
            }
        }
        Enumeration<URL> myResources = super.findResources(name);
        while (myResources.hasMoreElements()) {
            resources.add(myResources.nextElement());
        }
        return Collections.enumeration(resources);
    }

    public String toString() {
        return getName();
    }

    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        try {
            // look for previously loaded classes
            Class<?> clazz = findLoadedClass(name);
            if (clazz == null) {
                // look in the primary parent
                try {
                    clazz = Class.forName(name, resolve, getParent());
                } catch (ClassNotFoundException e) {
                    // continue
                }
                if (clazz == null) {
                    // look in other parents
                    for (ClassLoader parent : parents) {
                        try {
                            clazz = parent.loadClass(name);
                            break;
                        } catch (ClassNotFoundException e) {
                            //noinspection UnnecessaryContinue
                            continue;
                        }
                    }
                }
                // look in the current classloader
                if (clazz == null) {
                    clazz = findClass(name);
                }
            }
            if (resolve) {
                resolveClass(clazz);
            }
            return clazz;
        } catch (NoClassDefFoundError e) {
            // Handle loading of module-info lazily.
            //
            // Methods such as Module.getAnnotations() and its variants attempt to load a module info class using the internal
            // private method Module.moduleInfoClass(). For named modules, this method delegates to Module.loadModuleInfoClass(), which loads the byte stream from the model
            // and then attempts to load it with a synthetic classloader. This loader re-writes the class using an embedded version of ASM ClassWriter in the
            // Module.loadModuleInfoClass() prior to loading it, replacing ACC_MODULE with ACC_INTERFACE. The is presumably because module-info.class contains ACC_MODULE
            // which is recognized by the VM but not by all JDK classes. For example some classsloaders will throw NoClassDefFoundError when ACC_MODULE is found loading a class.
            //
            // If the module-info.class is not re-written with ACC_MODULE replaced by ACC_INTERFACE, a NoClassDefFoundError definition error will be thrown when attempting to
            // instantiate module-info.
            //
            // The synthetic loader delegates to this classloader since this loader is the parent of the module loader. This loader throws NoClassDefFoundError when attempting
            // to load module-info, which is caught here. After catching, return null so the synthetic classloader can re-write the bytes and load it.
            //
            // This is done lazily to avoid string compares on every class load.
            if ("module-info".equals(name)) {
                return null;
            }
            throw e;
        }
    }

}
