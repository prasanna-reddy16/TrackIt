package com.trackit;

import com.trackit.model.Route;
import com.trackit.model.Stop;
import com.trackit.model.Vehicle;
import com.trackit.repo.RouteRepository;
import com.trackit.repo.StopRepository;
import com.trackit.repo.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/** Seeds two Bengaluru routes (approximate coordinates) with simulated buses. */
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {
    private final RouteRepository routes;
    private final StopRepository stops;
    private final VehicleRepository vehicles;

    @Value("${trackit.simulator.vehicles-per-route:5}")
    private int perRoute;

    @Override
    public void run(String... args) {
        if (routes.count() > 0) return;

        Route r1 = routes.save(new Route("R1: Majestic - Indiranagar"));
        addStops(r1, new Object[][]{
                {"Majestic (Kempegowda Bus Stn)", 12.9774, 77.5727}, {"Vidhana Soudha", 12.9797, 77.5907},
                {"MG Road", 12.9756, 77.6068}, {"Trinity Circle", 12.9733, 77.6166}, {"Indiranagar", 12.9784, 77.6408}});

        Route r2 = routes.save(new Route("R2: Silk Board - Marathahalli"));
        addStops(r2, new Object[][]{
                {"Silk Board", 12.9177, 77.6233}, {"Koramangala", 12.9352, 77.6245},
                {"Domlur", 12.9609, 77.6387}, {"HAL Old Airport Rd", 12.9592, 77.6620}, {"Marathahalli", 12.9591, 77.6974}});

        for (int i = 1; i <= perRoute; i++) {
            vehicles.save(new Vehicle(String.format("KA01-R1-%02d", i), r1.getId()));
            vehicles.save(new Vehicle(String.format("KA01-R2-%02d", i), r2.getId()));
        }
    }

    private void addStops(Route r, Object[][] data) {
        for (int i = 0; i < data.length; i++) {
            stops.save(new Stop(r.getId(), i, (String) data[i][0], (Double) data[i][1], (Double) data[i][2]));
        }
    }
}
