package io.github.nwen.freecallcenterapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CallRecordResponse {

    private Long id;
    private String callId;
    private String callerNumber;
    private String calleeNumber;
    private String direction;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime answerTime;
    private LocalDateTime endTime;
    private Integer durationSeconds;
    private Long extensionId;
    private String recordingUrl;
    private LocalDateTime createdAt;
}
