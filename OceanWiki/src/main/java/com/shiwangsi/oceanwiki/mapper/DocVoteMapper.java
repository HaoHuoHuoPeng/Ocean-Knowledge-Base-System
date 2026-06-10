// 文件说明：这个 Mapper 负责文档点赞的数据库访问，继承 BaseMapper 后可以直接做基础增删改查。
package com.shiwangsi.oceanwiki.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shiwangsi.oceanwiki.entity.DocVote;

// 文档点赞记录 Mapper
// BaseMapper 已经提供 insert、delete、selectOne 等基础方法
public interface DocVoteMapper extends BaseMapper<DocVote> {
}
