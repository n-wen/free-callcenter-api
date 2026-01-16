package io.github.nwen.freecallcenterapi.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.nwen.freecallcenterapi.entity.CallRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CallRecordRepository extends BaseMapper<CallRecord> {

    @Select("""
        SELECT * FROM call_record
        WHERE created_at BETWEEN #{startTime} AND #{endTime}
        ORDER BY created_at DESC
        """)
    List<CallRecord> findByTimeRange(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    @Select("""
        SELECT * FROM call_record
        WHERE extension_id = #{extensionId}
        ORDER BY created_at DESC
        """)
    List<CallRecord> findByExtensionId(@Param("extensionId") Long extensionId);

    @Select("""
        SELECT COUNT(*) FROM call_record
        WHERE status = 'ANSWERED'
        AND created_at BETWEEN #{startTime} AND #{endTime}
        """)
    long countAnsweredCalls(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    @Select("""
        SELECT COUNT(*) FROM call_record
        WHERE created_at BETWEEN #{startTime} AND #{endTime}
        """)
    long countTotalCalls(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    @Select("""
        SELECT COALESCE(SUM(duration_seconds), 0) FROM call_record
        WHERE extension_id = #{extensionId}
        AND created_at BETWEEN #{startTime} AND #{endTime}
        """)
    long sumDurationByExtension(
            @Param("extensionId") Long extensionId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
}
