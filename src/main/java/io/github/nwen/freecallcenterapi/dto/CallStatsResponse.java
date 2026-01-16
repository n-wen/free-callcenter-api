package io.github.nwen.freecallcenterapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CallStatsResponse {

    private long totalCalls;
    private long answeredCalls;
    private long missedCalls;
    private double answerRate;
    private long totalDuration;
    private long avgDuration;
}
