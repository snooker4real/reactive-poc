package com.example.fruit;

import java.util.Map;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/fruits")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FruitResource {

    @GET
    public Uni<java.util.List<Fruit>> list() {
        return Fruit.findAllSorted();
    }

    @GET
    @Path("/{id}")
    public Uni<Response> getById(@PathParam("id") Long id) {
        return Fruit.<Fruit>findById(id)
                .onItem().ifNotNull().transform(fruit -> Response.ok(fruit).build())
                .onItem().ifNull().continueWith(() -> Response.status(Response.Status.NOT_FOUND).build());
    }

    @POST
    public Uni<Response> create(Fruit input) {
        if (input == null || input.name == null || input.name.isBlank()) {
            return Uni.createFrom().item(() -> Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "fruit name is required"))
                    .build());
        }

        Fruit fruit = new Fruit();
        fruit.name = input.name.trim();

        return Panache.withTransaction(fruit::persistAndFlush)
                .replaceWith(Response.status(Response.Status.CREATED).entity(fruit).build());
    }

    @DELETE
    @Path("/{id}")
    public Uni<Response> delete(@PathParam("id") Long id) {
        return Panache.withTransaction(() -> Fruit.deleteById(id))
                .map(deleted -> deleted
                        ? Response.noContent().build()
                        : Response.status(Response.Status.NOT_FOUND).build());
    }
}
