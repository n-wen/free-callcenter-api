package io.github.nwen.freecallcenterapi.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.nwen.freecallcenterapi.entity.CallRecord;
import io.github.nwen.freecallcenterapi.entity.Extension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class CallRecordRepositoryTest {

    @Autowired
    private CallRecordRepository callRecordRepository;

    @Autowired
    private ExtensionRepository extensionRepository;

    private Long extensionId;

    @BeforeEach
    void setUp() {
        callRecordRepository.delete(new LambdaQueryWrapper<>());
        extensionRepository.delete(new LambdaQueryWrapper<>());

        Extension extension = Extension.builder()
                .extensionNumber("1001")
                .password("password")
                .displayName("Test Agent")
                .enabled(true)
                .context("default")
                .build();
        extensionRepository.insert(extension);
        extensionId = extension.getId();
    }

    @Test
    void testInsertAndFindById() {
        String callId = UUID.randomUUID().toString();
        LocalDateTime startTime = LocalDateTime.now().minusMinutes(5);
        LocalDateTime answerTime = LocalDateTime.now().minusMinutes(4);
        LocalDateTime endTime = LocalDateTime.now();

        CallRecord record = CallRecord.builder()
                .callId(callId)
                .callerNumber("+1234567890")
                .calleeNumber("1001")
                .direction("INBOUND")
                .status("ANSWERED")
                .startTime(startTime)
                .answerTime(answerTime)
                .endTime(endTime)
                .durationSeconds(60)
                .extensionId(extensionId)
                .recordingUrl("/recordings/" + callId + ".wav")
                .build();

        callRecordRepository.insert(record);

        CallRecord found = callRecordRepository.selectById(record.getId());
        assertNotNull(found);
        assertEquals(callId, found.getCallId());
        assertEquals("+1234567890", found.getCallerNumber());
        assertEquals("INBOUND", found.getDirection());
    }

    @Test
    void testFindByCallId() {
        String callId = UUID.randomUUID().toString();

        CallRecord record = CallRecord.builder()
                .callId(callId)
                .callerNumber("+0987654321")
                .calleeNumber("1002")
                .direction("OUTBOUND")
                .status("INITIATED")
                .startTime(LocalDateTime.now())
                .build();

        callRecordRepository.insert(record);

        CallRecord found = callRecordRepository.selectOne(
                new LambdaQueryWrapper<CallRecord>()
                        .eq(CallRecord::getCallId, callId)
        );

        assertNotNull(found);
        assertEquals("+0987654321", found.getCallerNumber());
    }

    @Test
    void testFindByCallerNumber() {
        String caller = "+1112223333";
        for (int i = 0; i < 3; i++) {
            CallRecord record = CallRecord.builder()
                    .callId(UUID.randomUUID().toString())
                    .callerNumber(caller)
                    .calleeNumber("100" + i)
                    .direction("INBOUND")
                    .status("ANSWERED")
                    .startTime(LocalDateTime.now())
                    .durationSeconds(60 + i * 10)
                    .build();
            callRecordRepository.insert(record);
        }

        List<CallRecord> results = callRecordRepository.selectList(
                new LambdaQueryWrapper<CallRecord>()
                        .eq(CallRecord::getCallerNumber, caller)
                        .orderByDesc(CallRecord::getStartTime)
        );

        assertEquals(3, results.size());
    }

    @Test
    void testFindByCalleeNumber() {
        String callee = "1005";
        for (int i = 0; i < 2; i++) {
            CallRecord record = CallRecord.builder()
                    .callId(UUID.randomUUID().toString())
                    .callerNumber("+555666777" + i)
                    .calleeNumber(callee)
                    .direction("INBOUND")
                    .status("ANSWERED")
                    .startTime(LocalDateTime.now())
                    .durationSeconds(120)
                    .build();
            callRecordRepository.insert(record);
        }

        List<CallRecord> results = callRecordRepository.selectList(
                new LambdaQueryWrapper<CallRecord>()
                        .eq(CallRecord::getCalleeNumber, callee)
        );

        assertEquals(2, results.size());
    }

    @Test
    void testUpdateStatus() {
        String callId = UUID.randomUUID().toString();

        CallRecord record = CallRecord.builder()
                .callId(callId)
                .callerNumber("+1234567890")
                .calleeNumber("1001")
                .direction("INBOUND")
                .status("INITIATED")
                .startTime(LocalDateTime.now())
                .build();

        callRecordRepository.insert(record);

        record.setStatus("ANSWERED");
        record.setAnswerTime(LocalDateTime.now());
        callRecordRepository.updateById(record);

        CallRecord updated = callRecordRepository.selectById(record.getId());
        assertNotNull(updated);
        assertEquals("ANSWERED", updated.getStatus());
        assertNotNull(updated.getAnswerTime());
    }

    @Test
    void testDelete() {
        String callId = UUID.randomUUID().toString();

        CallRecord record = CallRecord.builder()
                .callId(callId)
                .callerNumber("+1234567890")
                .calleeNumber("1001")
                .direction("INBOUND")
                .status("COMPLETED")
                .startTime(LocalDateTime.now())
                .durationSeconds(60)
                .build();

        callRecordRepository.insert(record);
        Long id = record.getId();

        callRecordRepository.deleteById(id);

        CallRecord deleted = callRecordRepository.selectById(id);
        assertNull(deleted);
    }

    @Test
    void testFindByDirection() {
        for (int i = 0; i < 2; i++) {
            CallRecord inbound = CallRecord.builder()
                    .callId(UUID.randomUUID().toString())
                    .callerNumber("+1111111111")
                    .calleeNumber("1001")
                    .direction("INBOUND")
                    .status("ANSWERED")
                    .startTime(LocalDateTime.now())
                    .durationSeconds(60)
                    .build();
            callRecordRepository.insert(inbound);
        }

        for (int i = 0; i < 3; i++) {
            CallRecord outbound = CallRecord.builder()
                    .callId(UUID.randomUUID().toString())
                    .callerNumber("1001")
                    .calleeNumber("+2222222222")
                    .direction("OUTBOUND")
                    .status("ANSWERED")
                    .startTime(LocalDateTime.now())
                    .durationSeconds(60)
                    .build();
            callRecordRepository.insert(outbound);
        }

        List<CallRecord> inboundCalls = callRecordRepository.selectList(
                new LambdaQueryWrapper<CallRecord>()
                        .eq(CallRecord::getDirection, "INBOUND")
        );

        assertEquals(2, inboundCalls.size());
    }

    @Test
    void testFindByExtension() {
        CallRecord record = CallRecord.builder()
                .callId(UUID.randomUUID().toString())
                .callerNumber("+1234567890")
                .calleeNumber("1001")
                .direction("INBOUND")
                .status("ANSWERED")
                .startTime(LocalDateTime.now())
                .durationSeconds(120)
                .extensionId(extensionId)
                .recordingUrl("/recordings/test.wav")
                .build();

        callRecordRepository.insert(record);

        List<CallRecord> extensionRecords = callRecordRepository.selectList(
                new LambdaQueryWrapper<CallRecord>()
                        .eq(CallRecord::getExtensionId, extensionId)
        );

        assertEquals(1, extensionRecords.size());
        assertEquals("+1234567890", extensionRecords.get(0).getCallerNumber());
    }

    @Test
    void testFindByDateRange() {
        LocalDateTime now = LocalDateTime.now();

        CallRecord today = CallRecord.builder()
                .callId(UUID.randomUUID().toString())
                .callerNumber("+1111111111")
                .calleeNumber("1001")
                .direction("INBOUND")
                .status("ANSWERED")
                .startTime(now)
                .durationSeconds(60)
                .build();
        callRecordRepository.insert(today);

        CallRecord yesterday = CallRecord.builder()
                .callId(UUID.randomUUID().toString())
                .callerNumber("+2222222222")
                .calleeNumber("1001")
                .direction("INBOUND")
                .status("ANSWERED")
                .startTime(now.minusDays(1))
                .durationSeconds(60)
                .build();
        callRecordRepository.insert(yesterday);

        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        List<CallRecord> todayRecords = callRecordRepository.selectList(
                new LambdaQueryWrapper<CallRecord>()
                        .ge(CallRecord::getStartTime, startOfDay)
                        .orderByDesc(CallRecord::getStartTime)
        );

        assertEquals(1, todayRecords.size());
    }

    @Test
    void testCallStatistics() {
        for (int i = 0; i < 3; i++) {
            CallRecord record = CallRecord.builder()
                    .callId(UUID.randomUUID().toString())
                    .callerNumber("+1234567890")
                    .calleeNumber("1001")
                    .direction("INBOUND")
                    .status("ANSWERED")
                    .startTime(LocalDateTime.now())
                    .durationSeconds(60 * (i + 1))
                    .build();
            callRecordRepository.insert(record);
        }

        List<CallRecord> allRecords = callRecordRepository.selectList(
                new LambdaQueryWrapper<CallRecord>()
                        .eq(CallRecord::getStatus, "ANSWERED")
        );

        int totalDuration = allRecords.stream()
                .mapToInt(CallRecord::getDurationSeconds)
                .sum();

        assertEquals(3, allRecords.size());
        assertEquals(360, totalDuration);
    }

    @Test
    void testUniqueCallId() {
        String callId = UUID.randomUUID().toString();

        CallRecord record1 = CallRecord.builder()
                .callId(callId)
                .callerNumber("+1111111111")
                .calleeNumber("1001")
                .direction("INBOUND")
                .status("ANSWERED")
                .startTime(LocalDateTime.now())
                .durationSeconds(60)
                .build();
        callRecordRepository.insert(record1);

        CallRecord record2 = CallRecord.builder()
                .callId(callId)
                .callerNumber("+2222222222")
                .calleeNumber("1002")
                .direction("INBOUND")
                .status("ANSWERED")
                .startTime(LocalDateTime.now())
                .durationSeconds(60)
                .build();

        assertThrows(Exception.class, () -> callRecordRepository.insert(record2));
    }
}
