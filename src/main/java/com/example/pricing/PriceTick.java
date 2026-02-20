package com.example.pricing;

import java.time.Instant;

public record PriceTick(long sequence, double price, Instant emittedAt) {
}
