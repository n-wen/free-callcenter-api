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
@TableName("ivr_option")
public class IvrOption {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long ivrMenuId;

    private String digit;

    private String action;

    private String destination;

    private String description;

    private Integer priority;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
