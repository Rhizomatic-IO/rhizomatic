package io.rhizomatic.kernel.spi.reload;

/**
 *
 */
public interface ReloadListener {

    void onInstanceChanged(Object instance);

    void onInstanceAdded(Object instance);

}
