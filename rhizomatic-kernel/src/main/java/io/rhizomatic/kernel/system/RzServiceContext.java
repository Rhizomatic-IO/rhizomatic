package io.rhizomatic.kernel.system;

import io.rhizomatic.api.ServiceContext;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class RzServiceContext implements ServiceContext {
    private String runtimeName;
    private String domain;
    private String environment;

    private List<Runnable> callbacks = new ArrayList<>();

    public RzServiceContext(String runtimeName, String domain, String environment) {
        this.runtimeName = runtimeName;
        this.domain = domain;
        this.environment = environment;
    }

    public String getRuntimeName() {
        return runtimeName;
    }

    public String getDomain() {
        return domain;
    }

    public String getEnvironment() {
        return environment;
    }

    public void addBootCallback(Runnable runnable) {
       callbacks.add(runnable);
    }

    public void bootComplete(){
        callbacks.forEach(Runnable::run);
    }
}
