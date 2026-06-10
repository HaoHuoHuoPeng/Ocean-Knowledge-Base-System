// 文件说明：这个 Mapper 负责统计快照的数据库访问，继承 BaseMapper 后可以直接做基础增删改查。
package com.shiwangsi.oceanwiki.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shiwangsi.oceanwiki.entity.EbookSnapshot;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

// 统计快照 Mapper
// 用 XML 写统计 SQL，比在 Java 里手动循环更适合这种汇总场景
public interface EbookSnapshotMapper extends BaseMapper<EbookSnapshot> {

    // 保存当天快照；如果同一本电子书当天快照已存在，就直接更新，避免并发插入时报唯一键重复
    int upsertSnapshot(@Param("snapshot") EbookSnapshot snapshot);

    // 查询首页统计总览：总阅读、总点赞、今日阅读、今日点赞
    Map<String, Object> getStatistic();

    // 查询最近 30 天统计，用于统计页面展示趋势
    List<Map<String, Object>> get30Statistic();
}
