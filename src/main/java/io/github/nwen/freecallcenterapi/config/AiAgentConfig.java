package io.github.nwen.freecallcenterapi.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "ai-agent")
public class AiAgentConfig {
    private int wsPort = 8080;
    private String wsPath = "/audio";
    private String processor = "echoProcessor";
    private int timeout = 5000;
    private String freeswitchExtension = "9000";

    /** FreeSWITCH audio_stream 连接的 WebSocket 地址（Python AI 后端） */
    private String wsUrl = "ws://172.28.0.10:8080/";
}