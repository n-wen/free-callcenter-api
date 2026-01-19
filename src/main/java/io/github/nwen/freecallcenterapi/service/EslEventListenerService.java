package io.github.nwen.freecallcenterapi.service;

import io.github.nwen.freecallcenterapi.entity.CallRecord;
import io.github.nwen.freecallcenterapi.service.impl.EslConnectionManagerImpl;
import lombok.extern.slf4j.Slf4j;
import org.freeswitch.esl.client.IEslEventListener;
import org.freeswitch.esl.client.inbound.Client;
import org.freeswitch.esl.client.transport.event.EslEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class EslEventListenerService {

    private final EslService eslService;
    private final CallRecordService callRecordService;
    private Client eslClient;

    private final Map<String, CallRecord> pendingCalls = new ConcurrentHashMap<>();

    public EslEventListenerService(EslService eslService, CallRecordService callRecordService) {
        this.eslService = eslService;
        this.callRecordService = callRecordService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        Thread.startVirtualThread(() -> {
            try {
                for (int i = 0; i < 30; i++) {
                    if (eslService.isConnected()) {
                        startListening();
                        return;
                    }
                    Thread.sleep(1000);
                }
                log.warn("ESL not connected after 30 seconds, event listening not started");
            } catch (Exception e) {
                log.warn("Failed to start ESL event listening: {}", e.getMessage());
            }
        });
    }

    public void startListening() {
        try {
            if (eslService.isConnected()) {
                eslClient = ((EslConnectionManagerImpl) eslService).getClient();
                eslClient.addEventListener(new ExtensionEventListener());
                eslClient.setEventSubscriptions("plain", "all");
                log.info("ESL event listening started");
            } else {
                log.warn("ESL not connected, cannot start event listening");
            }
        } catch (Exception e) {
            log.warn("Failed to start ESL event listening: {}", e.getMessage());
            if (eslClient != null) {
                eslClient = null;
            }
        }
    }

    private class ExtensionEventListener implements IEslEventListener {

        @Override
        public void eventReceived(EslEvent event) {
            String eventName = event.getEventName();
            log.debug("Received ESL event: {}", eventName);

            if ("sofia::register".equals(eventName)) {
                handleRegisterEvent(event);
            } else if ("sofia::unregister".equals(eventName)) {
                handleUnregisterEvent(event);
            } else if (eventName != null && eventName.startsWith("CHANNEL_")) {
                handleChannelEvent(event);
            }
        }

        @Override
        public void backgroundJobResultReceived(EslEvent event) {
        }

        private void handleRegisterEvent(EslEvent event) {
            String user = event.getEventHeaders().get("sip_auth_username");
            if (user != null) {
                log.info("分机 {} 注册成功", user);
            }
        }

        private void handleUnregisterEvent(EslEvent event) {
            String user = event.getEventHeaders().get("sip_auth_username");
            if (user != null) {
                log.info("分机 {} 注销", user);
            }
        }

        private void handleChannelEvent(EslEvent event) {
            String eventName = event.getEventName();
            String uniqueId = event.getEventHeaders().get("Unique-ID");
            if (uniqueId == null) {
                return;
            }

            Map<String, String> headers = event.getEventHeaders();
            String callerNumber = getCallerNumber(headers);
            String calleeNumber = getCalleeNumber(headers);
            String direction = headers.get("Call-Direction");

            if ("CHANNEL_CREATE".equals(eventName)) {
                handleChannelCreate(uniqueId, callerNumber, calleeNumber, direction, headers);
            } else if ("CHANNEL_ANSWER".equals(eventName)) {
                handleChannelAnswer(uniqueId);
            } else if ("CHANNEL_HANGUP".equals(eventName)) {
                handleChannelHangup(uniqueId, headers.get("Hangup-Cause"));
            }
        }

        private String getCallerNumber(Map<String, String> headers) {
            String callerNumber = headers.get("Caller-Number");
            if (callerNumber == null || callerNumber.isEmpty()) {
                callerNumber = headers.get("sip_from_user");
            }
            return callerNumber;
        }

        private String getCalleeNumber(Map<String, String> headers) {
            String calleeNumber = headers.get("Caller-Destination-Number");
            if (calleeNumber == null || calleeNumber.isEmpty()) {
                calleeNumber = headers.get("sip_to_user");
            }
            return calleeNumber;
        }

        private void handleChannelCreate(String uniqueId, String callerNumber, String calleeNumber,
                                         String direction, Map<String, String> headers) {
            if (callerNumber == null || calleeNumber == null) {
                return;
            }

            CallRecord record = CallRecord.builder()
                    .callId(uniqueId)
                    .callerNumber(callerNumber)
                    .calleeNumber(calleeNumber)
                    .direction(direction != null ? direction : "outbound")
                    .startTime(LocalDateTime.now())
                    .status("INITIATED")
                    .build();

            pendingCalls.put(uniqueId, record);
            callRecordService.createCallRecord(record);
            log.info("通话创建: callId={}, caller={}, callee={}", uniqueId, callerNumber, calleeNumber);
        }

        private void handleChannelAnswer(String uniqueId) {
            CallRecord record = pendingCalls.get(uniqueId);
            if (record != null) {
                LocalDateTime answerTime = LocalDateTime.now();
                callRecordService.updateCallAnswerTime(uniqueId, answerTime);
                record.setAnswerTime(answerTime);
                record.setStatus("ANSWERED");
                log.info("通话接通: callId={}", uniqueId);
            }
        }

        private void handleChannelHangup(String uniqueId, String hangupCause) {
            CallRecord record = pendingCalls.remove(uniqueId);
            if (record != null) {
                LocalDateTime endTime = LocalDateTime.now();
                callRecordService.updateCallEndTime(uniqueId, endTime);
                record.setEndTime(endTime);
                log.info("通话挂断: callId={}, cause={}", uniqueId, hangupCause);
            }
        }
    }

    @PreDestroy
    public void destroy() {
    }
}
