package com.trackit.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public final class Dtos {
    private Dtos() {}

    public record LocationUpdate(
            @NotBlank String vehicleId,
            @DecimalMin("-90") @DecimalMax("90") double lat,
            @DecimalMin("-180") @DecimalMax("180") double lng,
            @PositiveOrZero double speedKmph) {}

    public record VehicleState(String vehicleId, Long routeId, double lat, double lng, double speedKmph, long ts) {}

    public record Alert(String vehicleId, Long routeId, String stopName, int distanceMeters, int etaMinutes, long ts) {}

    public record StopView(Long id, int seq, String name, double lat, double lng) {}

    public record RouteView(Long id, String name, List<StopView> stops) {}

    public record NearbyVehicle(VehicleState vehicle, double distanceKm) {}

    public record EtaView(String vehicleId, Long stopId, String stopName, double distanceKm, int etaMinutes, String note) {}

    public record StopInput(@NotBlank String name, double lat, double lng) {}

    public record CreateRoute(@NotBlank String name, @NotEmpty @Valid List<StopInput> stops) {}

    public record CreateVehicle(@NotBlank String id, @NotNull Long routeId) {}
}
