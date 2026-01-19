package io.github.nwen.freecallcenterapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtensionResponse {

    private Long id;

    private String extensionNumber;

    private String displayName;

    private Boolean enabled;

    private String context;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
