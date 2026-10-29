package com.trackit.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(indexes = @Index(columnList = "routeId"))
@Getter @Setter @NoArgsConstructor
public class Stop {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long routeId;
    private int seq;
    private String name;
    private double lat;
    private double lng;
    public Stop(Long routeId, int seq, String name, double lat, double lng) {
        this.routeId = routeId; this.seq = seq; this.name = name; this.lat = lat; this.lng = lng;
    }
}
