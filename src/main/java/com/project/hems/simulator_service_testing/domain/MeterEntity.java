package com.project.hems.simulator_service_testing.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "meter_info")
@NoArgsConstructor
@Data
public class MeterEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private Double lastKnownKwh;

}
