package io.github.nwen.freecallcenterapi.controller;

import io.github.nwen.freecallcenterapi.common.Result;
import io.github.nwen.freecallcenterapi.entity.CallRecord;
import io.github.nwen.freecallcenterapi.service.CallRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/call-records")
public class CallRecordCdrController {

    private final CallRecordService callRecordService;

    @PostMapping("/cdr")
    public Result<Void> receiveCdr(@RequestBody Map<String, Object> cdrData) {
        try {
            log.info("Received CDR data: {}", cdrData);

            CallRecord record = parseCdr(cdrData);
            if (record != null) {
                callRecordService.createCallRecord(record);
                return Result.success();
            }
            return Result.error(400, "Invalid CDR data");
        } catch (Exception e) {
            log.error("Failed to process CDR", e);
            return Result.error(500, "Failed to process CDR: " + e.getMessage());
        }
    }

    private CallRecord parseCdr(Map<String, Object> cdrData) {
        try {
            Map<String, Object> variables = (Map<String, Object>) cdrData.get("variables");
            if (variables == null) {
                variables = (Map<String, Object>) cdrData.get("fields");
            }

            if (variables == null) {
                log.warn("No variables in CDR data");
                return null;
            }

            String callId = getString(variables, "uuid");
            if (callId == null) {
                callId = getString(variables, "call_uuid");
            }

            String callerNumber = getString(variables, "caller_number");
            if (callerNumber == null) {
                callerNumber = getString(variables, "sip_from_user");
            }

            String calleeNumber = getString(variables, "destination_number");
            if (calleeNumber == null) {
                calleeNumber = getString(variables, "sip_to_user");
            }

            if (callerNumber == null || calleeNumber == null) {
                log.warn("Missing caller or callee number in CDR");
                return null;
            }

            String startTimeStr = getString(variables, "start_stamp");
            String answerTimeStr = getString(variables, "answer_stamp");
            String endTimeStr = getString(variables, "end_stamp");

            LocalDateTime startTime = parseTime(startTimeStr);
            LocalDateTime answerTime = parseTime(answerTimeStr);
            LocalDateTime endTime = parseTime(endTimeStr);

            Integer duration = getInt(variables, "duration");
            Integer billableSeconds = getInt(variables, "billsec");

            String direction = getString(variables, "call_direction");

            return CallRecord.builder()
                    .callId(callId)
                    .callerNumber(callerNumber)
                    .calleeNumber(calleeNumber)
                    .direction(direction != null ? direction : "outbound")
                    .status(duration != null && duration > 0 ? "ANSWERED" : "NO_ANSWER")
                    .startTime(startTime)
                    .answerTime(answerTime)
                    .endTime(endTime)
                    .durationSeconds(duration)
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse CDR data", e);
            return null;
        }
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    private Integer getInt(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDateTime parseTime(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(timeStr, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(timeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } catch (Exception ex) {
                log.debug("Failed to parse time: {}", timeStr);
                return null;
            }
        }
    }
}
