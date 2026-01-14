package io.github.nwen.freecallcenterapi.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "freeswitch.esl")
public class EslConfig {

    private String host = "127.0.0.1";
    private int port = 8021;
    private String password = "ClueCon";
    private int timeout = 5000;
}
