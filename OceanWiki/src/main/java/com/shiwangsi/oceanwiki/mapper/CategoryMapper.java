// 文件说明：这个 Mapper 负责分类的数据库访问，继承 BaseMapper 后可以直接做基础增删改查。
package com.shiwangsi.oceanwiki.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shiwangsi.oceanwiki.entity.Category;

// 分类 Mapper，继承 BaseMapper 后拥有基础增删改查方法
public interface CategoryMapper extends BaseMapper<Category> {
}
