package com.example.pricing;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.jboss.resteasy.reactive.RestStreamElementType;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/prices")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PriceResource {

    @Inject
    PricePipeline pipeline;

    @POST
    @Path("/{price}")
    public Uni<Response> publish(@PathParam("price") double price) {
        if (price <= 0) {
            return Uni.createFrom().item(() -> Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "price must be greater than zero"))
                    .build());
        }

        return pipeline.send(price)
                .replaceWith(Response.accepted(Map.of("published", price)).build());
    }

    @GET
    @Path("/last")
    public Response last() {
        Double value = pipeline.lastProcessedPrice();
        if (value == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("message", "No price has been consumed yet"))
                    .build();
        }

        return Response.ok(Map.of("lastProcessedPrice", value)).build();
    }

    @GET
    @Path("/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    public Multi<PriceTick> stream(
            @DefaultValue("20") @QueryParam("intervalMs") long intervalMs,
            @DefaultValue("100") @QueryParam("count") long count) {

        long safeIntervalMs = Math.max(1, intervalMs);
        long safeCount = Math.max(1, Math.min(5000, count));

        return Multi.createFrom().ticks().every(Duration.ofMillis(safeIntervalMs))
                .onOverflow().dropPreviousItems()
                .onItem().transform(sequence -> new PriceTick(
                        sequence,
                        ThreadLocalRandom.current().nextDouble(90.0, 110.0),
                        Instant.now()))
                .select().first(safeCount);
    }
}
