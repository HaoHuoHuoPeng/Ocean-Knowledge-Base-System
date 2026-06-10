// 文件说明：这个 Mapper 负责文档的数据库访问，继承 BaseMapper 后可以直接做基础增删改查。
package com.shiwangsi.oceanwiki.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shiwangsi.oceanwiki.entity.Doc;

// 文档 Mapper
// 这里除了 MyBatis-Plus 基础方法，还放阅读数、点赞数自增 SQL
public interface DocMapper extends BaseMapper<Doc> {

    // 阅读文档时，让 view_count 加 1
    void increaseViewCount(Long id);

    // 点赞文档时，让 vote_count 加 1
    void increaseVoteCount(Long id);

    // 取消点赞时，让 vote_count 减 1，最低不能小于 0
    void decreaseVoteCount(Long id);
}
