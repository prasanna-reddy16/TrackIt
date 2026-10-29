package com.trackit.service;

import com.trackit.dto.Dtos.LocationUpdate;
import com.trackit.model.Stop;
import com.trackit.model.Vehicle;
import com.trackit.repo.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;

/** Moves fake vehicles back and forth along their routes so the system can be demoed and load tested without real GPS devices. */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "trackit.simulator.enabled", havingValue = "true")
public class Simulator {
    private final VehicleRepository vehicles;
    private final RouteService routes;
    private final LocationService locations;
    private final Random rnd = new Random();

    @Value("${trackit.simulator.tick-ms:2000}") private long tickMs;
    @Value("${trackit.simulator.speed-multiplier:4}") private double speedMultiplier;

    private List<Sim> sims;

    private static class Sim {
        String id; List<double[]> pts; int seg; double along; double speedKmph;
    }

    @Scheduled(fixedDelayString = "${trackit.simulator.tick-ms:2000}", initialDelay = 3000)
    public synchronized void tick() {
        if (sims == null) sims = buildSims();
        for (Sim s : sims) {
            advance(s, s.speedKmph / 3.6 * (tickMs / 1000.0) * speedMultiplier);
            double[] a = s.pts.get(s.seg), b = s.pts.get(s.seg + 1);
            double len = Math.max(1, GeoUtil.haversineMeters(a[0], a[1], b[0], b[1]));
            double f = Math.min(1, s.along / len);
            double jitter = 0.00002; // tiny GPS noise
            double lat = a[0] + (b[0] - a[0]) * f + (rnd.nextDouble() - 0.5) * jitter;
            double lng = a[1] + (b[1] - a[1]) * f + (rnd.nextDouble() - 0.5) * jitter;
            s.speedKmph = Math.max(12, Math.min(45, s.speedKmph + (rnd.nextDouble() - 0.5) * 4));
            try { locations.ingest(new LocationUpdate(s.id, lat, lng, Math.round(s.speedKmph * 10) / 10.0)); }
            catch (RuntimeException e) { /* vehicle removed or Redis hiccup: skip this tick */ }
        }
    }

    private void advance(Sim s, double meters) {
        while (meters > 0) {
            double[] a = s.pts.get(s.seg), b = s.pts.get(s.seg + 1);
            double left = GeoUtil.haversineMeters(a[0], a[1], b[0], b[1]) - s.along;
            if (meters < left) { s.along += meters; return; }
            meters -= left; s.along = 0; s.seg++;
            if (s.seg >= s.pts.size() - 1) { Collections.reverse(s.pts); s.seg = 0; } // turn around at the terminus
        }
    }

    private List<Sim> buildSims() {
        List<Sim> out = new ArrayList<>();
        Map<Long, Integer> idx = new HashMap<>();
        for (Vehicle v : vehicles.findAll()) {
            List<Stop> stops = routes.stopsOf(v.getRouteId());
            if (stops.size() < 2) continue;
            Sim s = new Sim();
            s.id = v.getId();
            s.pts = new ArrayList<>();
            for (Stop st : stops) s.pts.add(new double[]{st.getLat(), st.getLng()});
            int n = idx.merge(v.getRouteId(), 1, Integer::sum) - 1;
            s.seg = n % (s.pts.size() - 1);                 // spread vehicles along the route
            s.along = rnd.nextDouble() * 300;
            s.speedKmph = 22 + rnd.nextDouble() * 12;
            out.add(s);
        }
        return out;
    }
}
