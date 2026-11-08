package com.swordshield.next.activity;
import java.util.List;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
@Mapper
public interface EventMapper extends BaseMapper<EventEntity> {
    @Select("SELECT id,tenant_id,title,status,created_by FROM event WHERE tenant_id=#{tenantId} AND status='PUBLISHED' ORDER BY id")
    List<EventEntity> published(@Param("tenantId") String tenantId);
    @Select("SELECT id,tenant_id,title,status,created_by FROM event WHERE id=#{id} AND tenant_id=#{tenantId}")
    EventEntity findOne(@Param("id") String id,@Param("tenantId") String tenantId);
    @Update("UPDATE event SET status='PUBLISHED' WHERE id=#{id} AND tenant_id=#{tenantId} AND status='DRAFT'")
    int publishDraft(@Param("id") String id,@Param("tenantId") String tenantId);
}
