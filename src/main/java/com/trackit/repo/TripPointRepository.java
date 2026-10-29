package com.trackit.repo;

import com.trackit.model.TripPoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface TripPointRepository extends JpaRepository<TripPoint, Long> {
    List<TripPoint> findByVehicleIdAndTsAfterOrderByTs(String vehicleId, Instant since);
}
