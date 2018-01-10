package io.rhizomatic.kernel.spi.util;

/**
 *
 */
public class ClassHelper {

    /**
     * Converts the file name into a class name.
     */
    public static String getClassName(String fileName) {
        return fileName.replace("/", ".").substring(0, fileName.length() - ".class".length());
    }

    private ClassHelper() {
    }
}
