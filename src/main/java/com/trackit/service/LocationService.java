package com.trackit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackit.dto.Dtos.*;
import com.trackit.model.Stop;
import com.trackit.model.TripPoint;
import com.trackit.repo.TripPointRepository;
import com.trackit.repo.VehicleRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class LocationService {
    static final String GEO_KEY = "vehicles:geo";       // Redis GEO set: latest position of every vehicle
    static final String STATE_KEY = "vehicles:state";    // Redis hash: vehicleId -> latest VehicleState JSON

    private final StringRedisTemplate redis;
    private final VehicleRepository vehicles;
    private final RouteService routes;
    private final TripPointRepository tripPoints;
    private final TrackingPublisher publisher;
    private final ObjectMapper om;
    private final double geofenceMeters;

    private final Map<String, Long> vehicleRoute = new ConcurrentHashMap<>();
    private final Queue<TripPoint> buffer = new ConcurrentLinkedQueue<>();

    public LocationService(StringRedisTemplate redis, VehicleRepository vehicles, RouteService routes,
                           TripPointRepository tripPoints, TrackingPublisher publisher, ObjectMapper om,
                           @Value("${trackit.geofence-meters:500}") double geofenceMeters) {
        this.redis = redis; this.vehicles = vehicles; this.routes = routes;
        this.tripPoints = tripPoints; this.publisher = publisher; this.om = om; this.geofenceMeters = geofenceMeters;
    }

    /** Hot path: called for every GPS ping (from drivers or the simulator). */
    public VehicleState ingest(LocationUpdate u) {
        Long routeId = vehicleRoute.computeIfAbsent(u.vehicleId(), id ->
                vehicles.findById(id).orElseThrow(() -> new NoSuchElementException("Unknown vehicle " + id)).getRouteId());

        VehicleState s = new VehicleState(u.vehicleId(), routeId, u.lat(), u.lng(), u.speedKmph(), System.currentTimeMillis());

        redis.opsForGeo().add(GEO_KEY, new Point(u.lng(), u.lat()), u.vehicleId());
        redis.opsForHash().put(STATE_KEY, u.vehicleId(), toJson(s));

        publisher.publish("/topic/vehicles", s);
        publisher.publish("/topic/route/" + routeId, s);

        TripPoint p = new TripPoint();
        p.setVehicleId(s.vehicleId()); p.setLat(s.lat()); p.setLng(s.lng());
        p.setSpeedKmph(s.speedKmph()); p.setTs(Instant.ofEpochMilli(s.ts()));
        buffer.add(p);

        checkGeofences(s);
        return s;
    }

    /** Batched writes: GPS history goes to Postgres every 5 seconds instead of one INSERT per ping. */
    @Scheduled(fixedDelay = 5000)
    public void flushHistory() {
        List<TripPoint> batch = new ArrayList<>();
        TripPoint p;
        while (batch.size() < 5000 && (p = buffer.poll()) != null) batch.add(p);
        if (!batch.isEmpty()) tripPoints.saveAll(batch);
    }

    private void checkGeofences(VehicleState s) {
        for (Stop stop : routes.stopsOf(s.routeId())) {
            double d = GeoUtil.haversineMeters(s.lat(), s.lng(), stop.getLat(), stop.getLng());
            if (d <= geofenceMeters) {
                // one alert per vehicle+stop every 10 minutes
                Boolean first = redis.opsForValue().setIfAbsent("alert:" + s.vehicleId() + ":" + stop.getId(), "1", Duration.ofMinutes(10));
                if (Boolean.TRUE.equals(first)) {
                    publisher.publish("/topic/alerts/" + s.routeId(), new Alert(s.vehicleId(), s.routeId(),
                            stop.getName(), (int) d, GeoUtil.etaMinutes(d, s.speedKmph()), s.ts()));
                }
            }
        }
    }

    public List<VehicleState> live() {
        List<Object> raw = redis.opsForHash().values(STATE_KEY);
        List<VehicleState> out = new ArrayList<>();
        for (Object o : raw) out.add(fromJson((String) o));
        return out;
    }

    public Optional<VehicleState> stateOf(String vehicleId) {
        Object o = redis.opsForHash().get(STATE_KEY, vehicleId);
        return o == null ? Optional.empty() : Optional.of(fromJson((String) o));
    }

    /** Redis GEORADIUS: vehicles within radiusKm of a point, nearest first. */
    public List<NearbyVehicle> nearby(double lat, double lng, double radiusKm) {
        var results = redis.opsForGeo().radius(GEO_KEY,
                new Circle(new Point(lng, lat), new Distance(radiusKm, Metrics.KILOMETERS)),
                RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs().includeDistance().sortAscending());
        List<NearbyVehicle> out = new ArrayList<>();
        if (results == null) return out;
        results.getContent().forEach(r -> stateOf(r.getContent().getName())
                .ifPresent(st -> out.add(new NearbyVehicle(st, Math.round(r.getDistance().getValue() * 1000) / 1000.0))));
        return out;
    }

    public EtaView eta(String vehicleId, Long stopId) {
        VehicleState s = stateOf(vehicleId).orElseThrow(() -> new NoSuchElementException("No live position for " + vehicleId));
        Stop stop = routes.stopsOf(s.routeId()).stream().filter(x -> x.getId().equals(stopId)).findFirst()
                .orElseThrow(() -> new NoSuchElementException("Stop " + stopId + " is not on this vehicle's route"));
        double d = GeoUtil.haversineMeters(s.lat(), s.lng(), stop.getLat(), stop.getLng());
        return new EtaView(vehicleId, stopId, stop.getName(), Math.round(d / 10.0) / 100.0,
                GeoUtil.etaMinutes(d, s.speedKmph()), "Straight-line distance; real road ETA would use a routing engine.");
    }

    public List<TripPoint> history(String vehicleId, int minutes) {
        flushHistory(); // make sure the freshest points are visible
        return tripPoints.findByVehicleIdAndTsAfterOrderByTs(vehicleId, Instant.now().minus(Duration.ofMinutes(minutes)));
    }

    private String toJson(VehicleState s) {
        try { return om.writeValueAsString(s); } catch (JsonProcessingException e) { throw new IllegalStateException(e); }
    }

    private VehicleState fromJson(String json) {
        try { return om.readValue(json, VehicleState.class); } catch (JsonProcessingException e) { throw new IllegalStateException(e); }
    }
}
