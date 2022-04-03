package io.rhizomatic.api;

/**
 *
 */
public interface ServiceContext {

    String getRuntimeName();

    String getEnvironment();

    void addBootCallback(Runnable runnable);

    void addShutdownCallback(Runnable runnable);
}
