// 文件说明：这个 Mapper 负责 doc_version 表的基础增删改查。
package com.shiwangsi.oceanwiki.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shiwangsi.oceanwiki.entity.DocVersion;
import org.apache.ibatis.annotations.Mapper;

// 文档版本 Mapper
// 继承 BaseMapper 后，MyBatis-Plus 会自动提供常用 CRUD 方法
@Mapper
public interface DocVersionMapper extends BaseMapper<DocVersion> {
}
