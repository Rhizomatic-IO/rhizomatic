package io.rhizomatic.tests;

import com.google.inject.Inject;
import io.rhizomatic.api.Monitor;
import io.rhizomatic.api.ServiceContext;
import io.rhizomatic.api.annotations.Eager;
import io.rhizomatic.api.annotations.Init;
import io.rhizomatic.api.annotations.Service;

import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 *
 */
@Service
@Eager
public class TestChannel implements TestProducer {
    private ExecutorService executor;

    @Inject
    protected Set<TestSubscriber> subscribers;

    @Inject
    ServiceContext context;

    @Inject
    Monitor monitor;

    @Init
    public void init() {
        context.addBootCallback(() -> {
            System.out.println("Service booted");
        });
    }

    public TestChannel() {

    }

    public void send(String message) {
        for (var subscriber : subscribers) {
            monitor.debug(() -> "Sending message: " + message);
            executor.submit(() -> subscriber.message(message));
        }
    }
}
