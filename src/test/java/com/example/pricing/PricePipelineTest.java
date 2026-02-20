package com.example.pricing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PricePipelineTest {

    @Test
    void consumeStoresLastProcessedPrice() {
        PricePipeline pipeline = new PricePipeline();

        pipeline.consume(42.5).await().indefinitely();

        assertEquals(42.5, pipeline.lastProcessedPrice());
    }
}
