package com.drugs.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
public class DrugStatisticsDTO {
    private long totalDrugs;
    private long expiredDrugs;
    private long activeDrugs;
    private long alertSentCount;
    private Map<String, Long> drugsByForm;
}
