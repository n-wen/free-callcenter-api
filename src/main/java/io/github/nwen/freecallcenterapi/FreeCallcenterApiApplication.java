package io.github.nwen.freecallcenterapi;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
@MapperScan("io.github.nwen.freecallcenterapi.repository")
public class FreeCallcenterApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(FreeCallcenterApiApplication.class, args);
    }
}
