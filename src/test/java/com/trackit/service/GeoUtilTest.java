package com.trackit.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GeoUtilTest {
    @Test void samePointIsZero() {
        assertEquals(0, GeoUtil.haversineMeters(12.97, 77.59, 12.97, 77.59), 0.001);
    }
    @Test void majesticToMgRoadIsAboutThreeKm() {
        double d = GeoUtil.haversineMeters(12.9774, 77.5727, 12.9756, 77.6068);
        assertTrue(d > 3500 && d < 4000, "distance was " + d);
    }
    @Test void etaUsesMinimumSpeed() {
        assertEquals(4, GeoUtil.etaMinutes(1000, 0));   // 1 km at the 15 km/h floor
        assertEquals(2, GeoUtil.etaMinutes(1000, 30));  // 1 km at 30 km/h
    }
}
