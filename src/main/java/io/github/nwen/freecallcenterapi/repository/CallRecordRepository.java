package io.github.nwen.freecallcenterapi.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.nwen.freecallcenterapi.entity.CallRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CallRecordRepository extends BaseMapper<CallRecord> {
}
