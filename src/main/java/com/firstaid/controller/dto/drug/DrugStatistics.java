package com.firstaid.controller.dto.drug;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DrugStatistics {
    private long totalDrugs;
    private long expiredDrugs;
    private long activeDrugs;
    private long alertSentCount;
    private Map<String, Long> drugsByForm;
}
