package com.project.hems.simulator_service_testing.model.envoy;

import com.project.hems.simulator_service_testing.model.BatteryMode;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

@ToString
@Data
@Builder
public class BatteryControl {
    private BatteryMode mode;
    private Long targetPowerW;
    private Long maxChargeW;
    private Long maxDischargeW;
    private Double minSocPercent;
    private Double maxSocPercent;
}
