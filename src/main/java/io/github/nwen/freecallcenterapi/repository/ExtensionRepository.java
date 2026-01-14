package io.github.nwen.freecallcenterapi.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.nwen.freecallcenterapi.entity.Extension;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Optional;

@Mapper
public interface ExtensionRepository extends BaseMapper<Extension> {

    @Select("SELECT * FROM extension WHERE extension_number = #{extensionNumber} LIMIT 1")
    Optional<Extension> findByExtensionNumber(String extensionNumber);
}
