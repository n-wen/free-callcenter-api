package io.github.nwen.freecallcenterapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DialRequest {

    private String destination;

    private String callerIdNumber;

    private String callerIdName;
}
