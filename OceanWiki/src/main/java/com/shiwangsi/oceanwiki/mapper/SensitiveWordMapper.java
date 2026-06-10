// 文件说明：这个 Mapper 负责敏感词的数据库访问，继承 BaseMapper 后可以直接做基础增删改查。
package com.shiwangsi.oceanwiki.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shiwangsi.oceanwiki.entity.SensitiveWord;

// 敏感词 Mapper，负责 sensitive_word 表的基础增删改查
public interface SensitiveWordMapper extends BaseMapper<SensitiveWord> {
}
