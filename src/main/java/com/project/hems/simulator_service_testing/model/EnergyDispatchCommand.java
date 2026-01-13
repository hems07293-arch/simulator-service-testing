package com.project.hems.simulator_service_testing.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class EnergyDispatchCommand {

    private Long siteId;
    private SourceType sourceType;
    private Double energyAmountKwh;
    private DispatchMode dispatchMode;
}
