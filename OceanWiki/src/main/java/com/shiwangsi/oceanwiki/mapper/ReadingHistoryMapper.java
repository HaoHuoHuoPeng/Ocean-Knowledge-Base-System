// 文件说明：这个 Mapper 负责阅读历史的数据库访问，继承 BaseMapper 后可以直接做基础增删改查。
package com.shiwangsi.oceanwiki.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shiwangsi.oceanwiki.entity.ReadingHistory;

// 阅读历史 Mapper，负责 reading_history 表的基础增删改查
public interface ReadingHistoryMapper extends BaseMapper<ReadingHistory> {
}
