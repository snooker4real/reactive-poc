package com.example.pricing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PriceResourceTest {

    @Test
    void publishRejectsNonPositivePrice() {
        PriceResource resource = new PriceResource();

        int status = resource.publish(0).await().indefinitely().getStatus();

        assertEquals(400, status);
    }
}
