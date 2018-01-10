package io.rhizomatic.gradle.assembly;

import org.gradle.api.GradleException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Helper methods for processing with streams.
 */
public final class IOHelper {
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

    public static void deleteDirectory(File directory) throws GradleException {
        if (!directory.exists()) {
            return;
        }
        cleanDirectory(directory);
        if (!directory.delete()) {
            String message = "Unable to delete directory " + directory + ".";
            throw new GradleException(message);
        }
    }


    /**
     * Clean a directory without deleting it.
     *
     * @param directory directory to clean
     * @throws GradleException in case cleaning is unsuccessful
     */
    public static void cleanDirectory(File directory) throws GradleException {
        if (directory == null) {
            return;
        }
        if (!directory.exists()) {
            throw new GradleException(directory + " does not exist");
        }

        if (!directory.isDirectory()) {
            throw new GradleException(directory + " is not a directory");
        }

        File[] files = directory.listFiles();
        if (files == null) { // null if security restricted
            throw new GradleException("Failed to list contents of " + directory);
        }

        for (File file : files) {
            if (file.isDirectory()) {
                deleteDirectory(file);
            } else if (!file.delete()) {
                throw new GradleException("Unable to delete file: " + file);
            }

        }
    }

    public static void copyDirectory(File srcDir, File destDir) throws GradleException {
        if (destDir.exists()) {
            if (!destDir.isDirectory()) {
                throw new GradleException("Destination '" + destDir + "' exists but is not a directory");
            }
        } else {
            if (!destDir.mkdirs()) {
                throw new GradleException("Destination '" + destDir + "' directory cannot be created");
            }
            destDir.setLastModified(srcDir.lastModified());
        }
        if (!destDir.canWrite()) {
            throw new GradleException("Destination '" + destDir + "' cannot be written to");
        }
        // recurse
        File[] files = srcDir.listFiles();
        if (files == null) { // null if security restricted
            throw new GradleException("Failed to list contents of " + srcDir);
        }
        for (File file : files) {
            File copiedFile = new File(destDir, file.getName());
            if (file.isDirectory()) {
                copyDirectory(file, copiedFile);
            } else {
                copyFile(file, copiedFile);
            }
        }
    }

    public static void copyFile(File srcFile, File destFile) throws GradleException {
        try {
            if (srcFile == null) {
                throw new GradleException("Source must not be null");
            }
            if (destFile == null) {
                throw new GradleException("Destination must not be null");
            }
            if (!srcFile.exists()) {
                throw new GradleException("Source '" + srcFile + "' does not exist");
            }
            if (srcFile.isDirectory()) {
                throw new GradleException("Source '" + srcFile + "' exists but is a directory");
            }
            if (srcFile.getCanonicalPath().equals(destFile.getCanonicalPath())) {
                throw new GradleException("Source '" + srcFile + "' and destination '" + destFile + "' are the same");
            }
            if (destFile.getParentFile() != null && !destFile.getParentFile().exists()) {
                if (!destFile.getParentFile().mkdirs()) {
                    throw new GradleException("Destination '" + destFile + "' directory cannot be created");
                }
            }
            if (destFile.exists() && !destFile.canWrite()) {
                throw new GradleException("Destination '" + destFile + "' exists but is read-only");
            }
            if (destFile.exists() && destFile.isDirectory()) {
                throw new GradleException("Destination '" + destFile + "' exists but is a directory");
            }
            try (FileInputStream input = new FileInputStream(srcFile); FileOutputStream output = new FileOutputStream(destFile)) {
                IOHelper.copy(input, output);
            } catch (IOException e) {
                throw new GradleException("Error copying file", e);
            }
            if (srcFile.length() != destFile.length()) {
                throw new GradleException("Failed to copy full contents from '" + srcFile + "' to '" + destFile + "'");
            }
            destFile.setLastModified(srcFile.lastModified());

        } catch (IOException e) {
            throw new GradleException("Error copying file", e);
        }
    }

    public static int copy(InputStream input, OutputStream output) throws GradleException {
        try {
            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
            int count = 0;
            int n;
            while (-1 != (n = input.read(buffer))) {
                output.write(buffer, 0, n);
                count += n;
            }
            return count;
        } catch (IOException e) {
            throw new GradleException("Error copying stream", e);
        }
    }

    private IOHelper() {
    }

}