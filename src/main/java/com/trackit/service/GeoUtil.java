package com.trackit.service;

public final class GeoUtil {
    private static final double EARTH_RADIUS_M = 6_371_000;
    private GeoUtil() {}

    /** Great-circle distance in meters. */
    public static double haversineMeters(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * EARTH_RADIUS_M * Math.asin(Math.sqrt(a));
    }

    /** ETA in minutes from distance and speed; uses a floor speed so a stopped bus doesn't give "infinity". */
    public static int etaMinutes(double meters, double speedKmph) {
        double kmph = Math.max(speedKmph, 15.0);
        double minutes = (meters / 1000.0) / kmph * 60.0;
        return (int) Math.ceil(minutes - 1e-9); // epsilon avoids 4.0000000001 -> 5
    }
}
