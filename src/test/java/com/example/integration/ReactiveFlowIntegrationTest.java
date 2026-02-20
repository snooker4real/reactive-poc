package com.example.integration;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;

@QuarkusTest
class ReactiveFlowIntegrationTest {

    @Test
    void fruitCrudFlowWorksEndToEnd() {
        String uniqueName = "Fruit-" + UUID.randomUUID();

        Number createdId = given()
                .contentType("application/json")
                .body(Map.of("name", uniqueName))
                .when().post("/api/fruits")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("name", is(uniqueName))
                .extract().path("id");
        long id = createdId.longValue();

        given()
                .when().get("/api/fruits/{id}", id)
                .then()
                .statusCode(200)
                .body("id", is((int) id))
                .body("name", is(uniqueName));

        given()
                .when().delete("/api/fruits/{id}", id)
                .then()
                .statusCode(204);

        given()
                .when().get("/api/fruits/{id}", id)
                .then()
                .statusCode(404);
    }

    @Test
    void pricePublishIsConsumedAndExposedViaRest() throws InterruptedException {
        double publishedPrice = 101.25;

        float published = given()
                .contentType("application/json")
                .when().post("/api/prices/{price}", publishedPrice)
                .then()
                .statusCode(202)
                .extract().path("published");

        assertEquals((float) publishedPrice, published, 0.000001f);

        awaitLastProcessedPrice(publishedPrice, Duration.ofSeconds(15));
    }

    @Test
    void priceStreamEndpointProducesSseEvents() {
        String body = given()
                .accept("text/event-stream")
                .queryParam("intervalMs", 2)
                .queryParam("count", 5)
                .when().get("/api/prices/stream")
                .then()
                .statusCode(200)
                .contentType(startsWith("text/event-stream"))
                .extract().asString();

        assertTrue(body.contains("data:"), "SSE stream should contain at least one event payload");
    }

    private static void awaitLastProcessedPrice(double expected, Duration timeout) throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        AssertionError lastError = null;

        while (System.nanoTime() < deadline) {
            Response response = given().when().get("/api/prices/last").andReturn();
            if (response.statusCode() == 200) {
                double actual = response.jsonPath().getDouble("lastProcessedPrice");
                if (Math.abs(actual - expected) < 0.000001) {
                    return;
                }
                lastError = new AssertionError(
                        "Expected /api/prices/last to be " + expected + " but was " + actual);
            } else {
                lastError = new AssertionError(
                        "Expected /api/prices/last status 200 but was " + response.statusCode());
            }

            Thread.sleep(200);
        }

        if (lastError != null) {
            throw lastError;
        }

        throw new AssertionError("Timed out waiting for consumed price");
    }
}
