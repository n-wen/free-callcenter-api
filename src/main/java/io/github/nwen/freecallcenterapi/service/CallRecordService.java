package io.github.nwen.freecallcenterapi.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.nwen.freecallcenterapi.dto.CallRecordQuery;
import io.github.nwen.freecallcenterapi.dto.CallRecordResponse;
import io.github.nwen.freecallcenterapi.dto.CallStatsResponse;
import io.github.nwen.freecallcenterapi.entity.CallRecord;
import io.github.nwen.freecallcenterapi.repository.CallRecordRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
public class CallRecordService extends ServiceImpl<CallRecordRepository, CallRecord> {

    public CallRecordResponse toResponse(CallRecord record) {
        return CallRecordResponse.builder()
                .id(record.getId())
                .callId(record.getCallId())
                .callerNumber(record.getCallerNumber())
                .calleeNumber(record.getCalleeNumber())
                .direction(record.getDirection())
                .status(record.getStatus())
                .startTime(record.getStartTime())
                .answerTime(record.getAnswerTime())
                .endTime(record.getEndTime())
                .durationSeconds(record.getDurationSeconds())
                .extensionId(record.getExtensionId())
                .recordingUrl(record.getRecordingUrl())
                .createdAt(record.getCreatedAt())
                .build();
    }

    public List<CallRecordResponse> findAll(int page, int size) {
        Page<CallRecord> recordPage = this.lambdaQuery()
                .orderByDesc(CallRecord::getCreatedAt)
                .page(new Page<>(page, size));
        return recordPage.getRecords().stream().map(this::toResponse).toList();
    }

    public List<CallRecordResponse> query(CallRecordQuery query, int page, int size) {
        var queryWrapper = this.lambdaQuery();

        if (query.getExtensionId() != null) {
            queryWrapper.eq(CallRecord::getExtensionId, query.getExtensionId());
        }
        if (query.getCallerNumber() != null) {
            queryWrapper.eq(CallRecord::getCallerNumber, query.getCallerNumber());
        }
        if (query.getCalleeNumber() != null) {
            queryWrapper.eq(CallRecord::getCalleeNumber, query.getCalleeNumber());
        }
        if (query.getStatus() != null) {
            queryWrapper.eq(CallRecord::getStatus, query.getStatus());
        }
        if (query.getStartTime() != null && query.getEndTime() != null) {
            queryWrapper.between(CallRecord::getCreatedAt, query.getStartTime(), query.getEndTime());
        }

        Page<CallRecord> recordPage = queryWrapper
                .orderByDesc(CallRecord::getCreatedAt)
                .page(new Page<>(page, size));
        return recordPage.getRecords().stream().map(this::toResponse).toList();
    }

    public CallRecordResponse findById(Long id) {
        return this.lambdaQuery().eq(CallRecord::getId, id).oneOpt()
                .map(this::toResponse)
                .orElse(null);
    }

    public CallStatsResponse getStats(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null) {
            startTime = LocalDateTime.now().withDayOfMonth(1);
        }
        if (endTime == null) {
            endTime = LocalDateTime.now();
        }

        long totalCalls = this.baseMapper.countTotalCalls(startTime, endTime);
        long answeredCalls = this.baseMapper.countAnsweredCalls(startTime, endTime);
        long missedCalls = totalCalls - answeredCalls;
        double answerRate = totalCalls > 0 ? (double) answeredCalls / totalCalls * 100 : 0;

        return CallStatsResponse.builder()
                .totalCalls(totalCalls)
                .answeredCalls(answeredCalls)
                .missedCalls(missedCalls)
                .answerRate(Math.round(answerRate * 100) / 100.0)
                .build();
    }

    public CallStatsResponse getExtensionStats(Long extensionId, LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null) {
            startTime = LocalDateTime.now().withDayOfMonth(1);
        }
        if (endTime == null) {
            endTime = LocalDateTime.now();
        }

        long totalCalls = this.lambdaQuery()
                .eq(CallRecord::getExtensionId, extensionId)
                .count();
        long answeredCalls = this.lambdaQuery()
                .eq(CallRecord::getExtensionId, extensionId)
                .eq(CallRecord::getStatus, "ANSWERED")
                .count();
        long totalDuration = this.baseMapper.sumDurationByExtension(extensionId, startTime, endTime);
        long avgDuration = answeredCalls > 0 ? totalDuration / answeredCalls : 0;

        return CallStatsResponse.builder()
                .totalCalls(totalCalls)
                .answeredCalls(answeredCalls)
                .missedCalls(totalCalls - answeredCalls)
                .answerRate(totalCalls > 0 ? (double) answeredCalls / totalCalls * 100 : 0)
                .totalDuration(totalDuration)
                .avgDuration(avgDuration)
                .build();
    }

    public void createCallRecord(CallRecord record) {
        this.save(record);
        log.info("创建通话记录: callId={}, caller={}, callee={}",
                record.getCallId(), record.getCallerNumber(), record.getCalleeNumber());
    }

    public void updateCallRecord(String callId, java.util.function.Consumer<CallRecord> updater) {
        CallRecord record = this.lambdaQuery().eq(CallRecord::getCallId, callId).one();
        if (record != null) {
            updater.accept(record);
            this.updateById(record);
            log.info("更新通话记录: callId={}", callId);
        }
    }

    public void updateCallStatus(String callId, String status) {
        updateCallRecord(callId, record -> record.setStatus(status));
    }

    public void updateCallAnswerTime(String callId, LocalDateTime answerTime) {
        updateCallRecord(callId, record -> {
            record.setAnswerTime(answerTime);
            record.setStatus("ANSWERED");
        });
    }

    public void updateCallEndTime(String callId, LocalDateTime endTime) {
        updateCallRecord(callId, record -> {
            record.setEndTime(endTime);
            if (record.getAnswerTime() != null) {
                record.setDurationSeconds((int) (endTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        - record.getAnswerTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()) / 1000);
            }
        });
    }
}
