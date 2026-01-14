package io.github.nwen.freecallcenterapi.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("ivr_menu")
public class IvrMenu {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String description;

    private String welcomeSound;

    private Integer timeoutSeconds;

    private Integer maxAttempts;

    private String invalidSound;

    private Boolean enabled;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
