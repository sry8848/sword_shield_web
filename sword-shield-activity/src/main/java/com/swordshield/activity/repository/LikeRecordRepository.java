package com.swordshield.activity.repository;

import com.swordshield.activity.domain.LikeRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 读取互动模块的 like_record 表（同库）用于聚合点赞数。
 *
 * <p>注意：该仓储不用于业务写入，只用于 count 聚合，避免活动服务依赖 HTTP/RPC。
 */
public interface LikeRecordRepository extends JpaRepository<LikeRecord, Long> {

    @Query(value = """
            select lr.target_id as targetId, count(*) as cnt
              from like_record lr
             where lr.target_type = :targetType
               and lr.target_id in (:targetIds)
             group by lr.target_id
            """, nativeQuery = true)
    List<LikeCount> countLikesByTargetIds(@Param("targetType") String targetType, @Param("targetIds") List<Long> targetIds);

    interface LikeCount {
        Long getTargetId();
        Long getCnt();
    }
}
