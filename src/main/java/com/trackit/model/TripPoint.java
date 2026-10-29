package com.trackit.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(indexes = @Index(name = "idx_trip_vehicle_ts", columnList = "vehicleId, ts"))
@Getter @Setter @NoArgsConstructor
public class TripPoint {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String vehicleId;
    private double lat;
    private double lng;
    private double speedKmph;
    private Instant ts;
}
