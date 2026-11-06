package com.credibanco.qa.restfulbooker.data;

import java.util.UUID;

/**
 * Short unique suffix that isolates the data of each scenario on the shared public API (H-02).
 *
 * <p>Deliberately not derived from the data seed: replaying a seed must reproduce the data shape, never collide
 * with bookings left by a previous run.
 */
public final class UniqueMarker {

    /** 12 hex characters = 48 random bits: collision probability ~2e-7 for 10 000 markers (8 would be ~1%). */
    private static final int LENGTH = 12;

    private UniqueMarker() {
    }

    /** Twelve lowercase hexadecimal characters, unique per call. */
    public static String next() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, LENGTH);
    }
}
