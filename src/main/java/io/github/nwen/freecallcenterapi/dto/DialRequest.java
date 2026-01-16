package io.github.nwen.freecallcenterapi.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DialRequest {

    @NotBlank(message = "发起方分机号不能为空")
    private String source;

    private String destination;

    private String callerIdNumber;

    private String callerIdName;
}
