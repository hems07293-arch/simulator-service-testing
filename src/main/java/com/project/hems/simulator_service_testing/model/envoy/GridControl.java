package com.project.hems.simulator_service_testing.model.envoy;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

@ToString
@Data
@Builder
public class GridControl {

    private Boolean allowImport;
    private Boolean allowExport;
    private Double maxImportW;
    private Double maxExportW;
}
