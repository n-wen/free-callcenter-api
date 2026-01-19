package io.github.nwen.freecallcenterapi.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseInitializer {

    private static final Pattern SQL_COMMENT_PATTERN = Pattern.compile("--.*$", Pattern.MULTILINE);
    private static final Pattern BLANK_LINE_PATTERN = Pattern.compile("^\\s*$", Pattern.MULTILINE);

    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void initialize() {
        log.info("Checking database tables...");
        if (tableExists("extension")) {
            log.info("Database tables already exist, skipping initialization");
            return;
        }
        log.info("Tables not found, initializing database...");
        executeSqlScript("sql/001-create-tables.sql");
        log.info("Database initialization completed");
    }

    private boolean tableExists(String tableName) {
        String sql = "SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_name = ?)";
        try {
            Boolean exists = jdbcTemplate.queryForObject(sql, Boolean.class, tableName);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.warn("Failed to check table existence: {}", e.getMessage());
            return false;
        }
    }

    private void executeSqlScript(String resourcePath) {
        try {
            ClassPathResource resource = new ClassPathResource(resourcePath);
            String sql = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            String normalizedSql = normalizeSql(sql);
            String[] statements = normalizedSql.split(";");
            int executed = 0;
            for (String statement : statements) {
                String trimmed = statement.trim();
                if (!trimmed.isEmpty()) {
                    try {
                        jdbcTemplate.execute(trimmed);
                        executed++;
                    } catch (Exception e) {
                        log.warn("SQL statement failed: {}", e.getMessage());
                    }
                }
            }
            log.info("Executed {} SQL statements", executed);
        } catch (Exception e) {
            log.warn("Failed to execute SQL script: {}", e.getMessage());
        }
    }

    private String normalizeSql(String sql) {
        String cleaned = SQL_COMMENT_PATTERN.matcher(sql).replaceAll("");
        cleaned = BLANK_LINE_PATTERN.matcher(cleaned).replaceAll("");
        return cleaned;
    }
}
