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
public class ExtensionStatusResponse {

    private Long id;

    private String extensionNumber;

    private String displayName;

    private String status;

    private String context;

    private LocalDateTime lastRegisteredAt;

    private LocalDateTime lastUnregisteredAt;
}
