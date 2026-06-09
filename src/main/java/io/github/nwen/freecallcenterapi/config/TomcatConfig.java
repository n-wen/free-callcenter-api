package io.github.nwen.freecallcenterapi.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * Tomcat Host 头验证配置
 * 在 Docker 网络中，FreeSWITCH 通过服务名 "springboot" 发起 WebSocket 连接，
 * Host 头为 springboot:8081，Tomcat 默认会拒绝非标准 host 头的 WebSocket 升级请求。
 * 此配置禁用 Tomcat HttpParser 的严格 Host 头校验。
 */
@Slf4j
@Configuration
public class TomcatConfig {

    @PostConstruct
    public void disableStrictHostChecking() {
        // Tomcat 10.x HttpParser 严格 Host 头校验
        // 设置为 false 允许容器服务名（如 springboot）作为 Host 头
        System.setProperty("tomcat.util.http.parser.HttpParser.REJECT_ILLEGAL_HEADERS", "false");
        log.info("已禁用 Tomcat HttpParser 严格 Host 头校验（允许 Docker 服务主机名）");
    }
}