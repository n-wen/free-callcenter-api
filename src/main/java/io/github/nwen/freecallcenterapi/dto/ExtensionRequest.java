package io.github.nwen.freecallcenterapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtensionRequest {

    @NotBlank(message = "分机号不能为空")
    @Pattern(regexp = "^[0-9]{3,10}$", message = "分机号必须是3-10位数字")
    private String extensionNumber;

    @NotBlank(message = "密码不能为空")
    @Size(min = 4, max = 20, message = "密码长度必须在4-20位之间")
    private String password;

    @NotBlank(message = "显示名称不能为空")
    @Size(max = 50, message = "显示名称最大50个字符")
    private String displayName;

    @NotBlank(message = "上下文不能为空")
    @Size(max = 50, message = "上下文最大50个字符")
    private String context;
}
