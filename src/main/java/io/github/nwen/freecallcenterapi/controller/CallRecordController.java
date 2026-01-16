package io.github.nwen.freecallcenterapi.controller;

import io.github.nwen.freecallcenterapi.common.Result;
import io.github.nwen.freecallcenterapi.dto.CallRecordQuery;
import io.github.nwen.freecallcenterapi.dto.CallRecordResponse;
import io.github.nwen.freecallcenterapi.dto.CallStatsResponse;
import io.github.nwen.freecallcenterapi.service.CallRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/call-records")
public class CallRecordController {

    private final CallRecordService callRecordService;

    @GetMapping
    public Result<List<CallRecordResponse>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long extensionId,
            @RequestParam(required = false) String callerNumber,
            @RequestParam(required = false) String calleeNumber,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime
    ) {
        CallRecordQuery query = CallRecordQuery.builder()
                .extensionId(extensionId)
                .callerNumber(callerNumber)
                .calleeNumber(calleeNumber)
                .status(status)
                .startTime(startTime)
                .endTime(endTime)
                .build();

        List<CallRecordResponse> records = callRecordService.query(query, page, size);
        return Result.success(records);
    }

    @GetMapping("/{id}")
    public Result<CallRecordResponse> getById(@PathVariable Long id) {
        CallRecordResponse record = callRecordService.findById(id);
        if (record == null) {
            return Result.error(404, "通话记录不存在");
        }
        return Result.success(record);
    }

    @GetMapping("/stats")
    public Result<CallStatsResponse> getStats(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(required = false) Long extensionId
    ) {
        CallStatsResponse stats;
        if (extensionId != null) {
            stats = callRecordService.getExtensionStats(extensionId, startTime, endTime);
        } else {
            stats = callRecordService.getStats(startTime, endTime);
        }
        return Result.success(stats);
    }
}
