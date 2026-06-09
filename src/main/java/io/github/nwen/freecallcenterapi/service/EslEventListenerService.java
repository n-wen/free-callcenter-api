package io.github.nwen.freecallcenterapi.service;

import io.github.nwen.freecallcenterapi.config.AiAgentConfig;
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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
@Service
public class EslEventListenerService {

    private final EslService eslService;
    private final CallRecordService callRecordService;
    private final AiAgentConfig aiAgentConfig;
    private Client eslClient;

    private final Map<String, CallRecord> pendingCalls = new ConcurrentHashMap<>();
    /** 追踪 AI 智能体通话的 UUID */
    private final Set<String> aiAgentCallIds = new CopyOnWriteArraySet<>();

    public EslEventListenerService(EslService eslService, CallRecordService callRecordService, AiAgentConfig aiAgentConfig) {
        this.eslService = eslService;
        this.callRecordService = callRecordService;
        this.aiAgentConfig = aiAgentConfig;
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

            // DEBUG: 查看 CHANNEL_CREATE 的头部信息
            if ("CHANNEL_CREATE".equals(eventName)) {
                log.info("CHANNEL_CREATE: id={}, caller={}, callee={}, dir={}, dest={}, to={}",
                        uniqueId, callerNumber, calleeNumber, direction,
                        headers.get("Caller-Destination-Number"),
                        headers.get("sip_to_user"));
            }

            if ("CHANNEL_CREATE".equals(eventName)) {
                handleChannelCreate(uniqueId, callerNumber, calleeNumber, direction, headers);
            } else if ("CHANNEL_ANSWER".equals(eventName)) {
                handleChannelAnswer(uniqueId, headers);
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
            if (calleeNumber == null) {
                return;
            }
            // callerNumber 可能为空（从 Linphone 注册有时不带 Caller-Number 头）
            String effectiveCaller = callerNumber != null ? callerNumber : "unknown";

            CallRecord record = CallRecord.builder()
                    .callId(uniqueId)
                    .callerNumber(effectiveCaller)
                    .calleeNumber(calleeNumber)
                    .direction(direction != null ? direction : "outbound")
                    .startTime(LocalDateTime.now())
                    .status("INITIATED")
                    .build();

            pendingCalls.put(uniqueId, record);
            callRecordService.createCallRecord(record);
            log.info("通话创建: callId={}, caller={}, callee={}", uniqueId, effectiveCaller, calleeNumber);

            // 检测是否拨打 AI 智能体分机
            String aiExtension = aiAgentConfig.getFreeswitchExtension();
            if (aiExtension.equals(calleeNumber)) {
                aiAgentCallIds.add(uniqueId);
                log.info("AI 智能体通话已标记: callId={}, extension={}", uniqueId, aiExtension);
            }
        }

        private void handleChannelAnswer(String uniqueId, Map<String, String> headers) {
            CallRecord record = pendingCalls.get(uniqueId);
            if (record != null) {
                LocalDateTime answerTime = LocalDateTime.now();
                callRecordService.updateCallAnswerTime(uniqueId, answerTime);
                record.setAnswerTime(answerTime);
                record.setStatus("ANSWERED");
                log.info("通话接通: callId={}", uniqueId);

                // 如果是 AI 智能体通话，启动 audio_stream
                if (aiAgentCallIds.contains(uniqueId)) {
                    startAiAudioStream(uniqueId);
                }
            }
        }

        private void handleChannelHangup(String uniqueId, String hangupCause) {
            CallRecord record = pendingCalls.remove(uniqueId);
            if (record != null) {
                LocalDateTime endTime = LocalDateTime.now();
                callRecordService.updateCallEndTime(uniqueId, endTime);
                record.setEndTime(endTime);
                log.info("通话挂断: callId={}, cause={}", uniqueId, hangupCause);

                // 如果是 AI 智能体通话，停止 audio_stream
                if (aiAgentCallIds.remove(uniqueId)) {
                    stopAiAudioStream(uniqueId);
                }
            } else if (aiAgentCallIds.contains(uniqueId)) {
                // record 可能已被清理，但仍需停止 audio_stream
                aiAgentCallIds.remove(uniqueId);
                stopAiAudioStream(uniqueId);
                log.info("AI 通话挂断清理: callId={}", uniqueId);
            }
        }

        /**
         * 通过 ESL API 启动 uuid_audio_stream
         * 将通话音频流通过 WebSocket 发送到后端
         */
        private void startAiAudioStream(String uniqueId) {
            String wsUrl = aiAgentConfig.getWsUrl();
            // mod_audio_stream 要求 start 命令至少 4 个参数: <uuid> start <url> <mode> [sampling]
            // mode: mixed(双向)|mono(仅上行)|stereo, sampling: 8000|16000
            String command = String.format("uuid_audio_stream %s start %s mixed 16000", uniqueId, wsUrl);
            log.info("启动 AI audio stream: command={}", command);
            try {
                String result = eslService.sendCommand(command);
                log.info("AI audio stream 启动结果: {}", result);
            } catch (Exception e) {
                log.error("启动 AI audio stream 失败: callId={}", uniqueId, e);
            }
        }

        /**
         * 通过 ESL API 停止 uuid_audio_stream
         */
        private void stopAiAudioStream(String uniqueId) {
            String command = String.format("uuid_audio_stream %s stop", uniqueId);
            log.info("停止 AI audio stream: command={}", command);
            try {
                String result = eslService.sendCommand(command);
                log.debug("AI audio stream 停止结果: {}", result);
            } catch (Exception e) {
                log.error("停止 AI audio stream 失败: callId={}", uniqueId, e);
            }
        }
    }

    @PreDestroy
    public void destroy() {
    }
}