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
@TableName("call_record")
public class CallRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String callId;

    private String callerNumber;

    private String calleeNumber;

    private String direction;

    private String status;

    private LocalDateTime startTime;

    private LocalDateTime answerTime;

    private LocalDateTime endTime;

    private Integer durationSeconds;

    private Long extensionId;

    private String recordingUrl;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
