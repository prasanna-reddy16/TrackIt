package com.trackit.web;

import com.trackit.dto.Dtos.*;
import com.trackit.model.TripPoint;
import com.trackit.model.Vehicle;
import com.trackit.repo.RouteRepository;
import com.trackit.repo.VehicleRepository;
import com.trackit.service.LocationService;
import com.trackit.service.RouteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TrackingController {
    private final LocationService locations;
    private final RouteService routes;
    private final RouteRepository routeRepo;
    private final VehicleRepository vehicleRepo;

    @Value("${trackit.api-key}")
    private String apiKey;

    // ---- public read endpoints ----
    @GetMapping("/routes")
    public List<RouteView> routes() { return routes.all(); }

    @GetMapping("/vehicles/live")
    public List<VehicleState> live() { return locations.live(); }

    @GetMapping("/vehicles/nearby")
    public List<NearbyVehicle> nearby(@RequestParam double lat, @RequestParam double lng,
                                      @RequestParam(defaultValue = "2") double radiusKm) {
        return locations.nearby(lat, lng, radiusKm);
    }

    @GetMapping("/vehicles/{id}/eta")
    public EtaView eta(@PathVariable String id, @RequestParam Long stopId) { return locations.eta(id, stopId); }

    @GetMapping("/vehicles/{id}/history")
    public List<TripPoint> history(@PathVariable String id, @RequestParam(defaultValue = "30") int minutes) {
        return locations.history(id, Math.min(minutes, 24 * 60));
    }

    // ---- write endpoints (need X-API-KEY) ----
    @PostMapping("/location")
    public VehicleState ingest(@RequestHeader(value = "X-API-KEY", required = false) String key,
                               @Valid @RequestBody LocationUpdate u) {
        requireKey(key);
        return locations.ingest(u);
    }

    @PostMapping("/routes")
    public RouteView createRoute(@RequestHeader(value = "X-API-KEY", required = false) String key,
                                 @Valid @RequestBody CreateRoute req) {
        requireKey(key);
        return routes.create(req);
    }

    @PostMapping("/vehicles")
    public Vehicle createVehicle(@RequestHeader(value = "X-API-KEY", required = false) String key,
                                 @Valid @RequestBody CreateVehicle req) {
        requireKey(key);
        if (!routeRepo.existsById(req.routeId())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown route");
        return vehicleRepo.save(new Vehicle(req.id(), req.routeId()));
    }

    private void requireKey(String key) {
        if (!apiKey.equals(key)) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid X-API-KEY");
    }
}
