package io.github.nwen.freecallcenterapi.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.nwen.freecallcenterapi.entity.Extension;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ExtensionRepository extends BaseMapper<Extension> {
}
