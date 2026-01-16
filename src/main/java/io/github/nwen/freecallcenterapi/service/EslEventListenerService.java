package io.github.nwen.freecallcenterapi.service;

import io.github.nwen.freecallcenterapi.service.impl.EslConnectionManagerImpl;
import lombok.extern.slf4j.Slf4j;
import org.freeswitch.esl.client.IEslEventListener;
import org.freeswitch.esl.client.inbound.Client;
import org.freeswitch.esl.client.transport.event.EslEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;

@Slf4j
@Service
public class EslEventListenerService {

    private final EslService eslService;
    private final ExtensionService extensionService;
    private Client eslClient;

    public EslEventListenerService(EslService eslService, ExtensionService extensionService) {
        this.eslService = eslService;
        this.extensionService = extensionService;
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
            }
        }

        @Override
        public void backgroundJobResultReceived(EslEvent event) {
        }

        private void handleRegisterEvent(EslEvent event) {
            String user = event.getEventHeaders().get("sip_auth_username");
            if (user != null) {
                log.info("分机 {} 注册成功", user);
                extensionService.updateExtensionStatus(user, "ONLINE");
            }
        }

        private void handleUnregisterEvent(EslEvent event) {
            String user = event.getEventHeaders().get("sip_auth_username");
            if (user != null) {
                log.info("分机 {} 注销", user);
                extensionService.updateExtensionStatus(user, "OFFLINE");
            }
        }
    }

    @PreDestroy
    public void destroy() {
    }
}
