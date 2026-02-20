package com.example.pricing;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.MutinyEmitter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class PricePipeline {

    private static final Logger LOG = Logger.getLogger(PricePipeline.class);

    @Inject
    @Channel("prices-out")
    MutinyEmitter<Double> emitter;

    private final AtomicReference<Double> lastProcessedPrice = new AtomicReference<>();

    public Uni<Void> send(double price) {
        return emitter.send(price);
    }

    @Incoming("prices-in")
    public Uni<Void> consume(double price) {
        return Uni.createFrom().voidItem()
                .invoke(() -> {
                    lastProcessedPrice.set(price);
                    LOG.infov("Consumed price {0}", price);
                });
    }

    public Double lastProcessedPrice() {
        return lastProcessedPrice.get();
    }
}
