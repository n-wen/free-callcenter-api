package io.github.nwen.freecallcenterapi.service.impl;

import io.github.nwen.freecallcenterapi.config.EslConfig;
import io.github.nwen.freecallcenterapi.service.EslService;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.freeswitch.esl.client.inbound.Client;
import org.freeswitch.esl.client.transport.message.EslMessage;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EslConnectionManagerImpl implements EslService {

    private final EslConfig eslConfig;
    private Client eslClient;

    public EslConnectionManagerImpl(EslConfig eslConfig) {
        this.eslConfig = eslConfig;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        try {
            connect();
        } catch (Exception e) {
            log.warn("FreeSWITCH ESL connection failed during startup, will retry on demand");
        }
    }

    @Override
    public boolean isConnected() {
        return eslClient != null;
    }

    @Override
    public void connect() {
        if (eslClient != null) {
            return;
        }
        try {
            log.info("Connecting to FreeSWITCH ESL: {}:{}", eslConfig.getHost(), eslConfig.getPort());
            eslClient = new Client();
            eslClient.connect(eslConfig.getHost(), eslConfig.getPort(), eslConfig.getPassword(), 5000);
            log.info("Connected to FreeSWITCH ESL successfully");
        } catch (Exception e) {
            log.warn("Failed to connect to FreeSWITCH ESL: {}", e.getMessage());
            eslClient = null;
        }
    }

    @Override
    public void disconnect() {
        if (eslClient != null) {
            log.info("Disconnecting from FreeSWITCH ESL");
        }
    }

    @Override
    public String sendCommand(String command) {
        if (!isConnected()) {
            connect();
        }
        try {
            EslMessage response = eslClient.sendSyncApiCommand(command, "");
            if (response != null && response.getBodyLines() != null && !response.getBodyLines().isEmpty()) {
                return String.join("\n", response.getBodyLines());
            }
            return "";
        } catch (Exception e) {
            log.error("Failed to send ESL command: {}", command, e);
            return null;
        }
    }

    @PreDestroy
    public void destroy() {
        disconnect();
    }
}
