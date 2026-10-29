package com.trackit.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter @Setter @NoArgsConstructor
public class Vehicle {
    @Id
    private String id;       // e.g. KA01-R1-01
    private Long routeId;
    public Vehicle(String id, Long routeId) { this.id = id; this.routeId = routeId; }
}
