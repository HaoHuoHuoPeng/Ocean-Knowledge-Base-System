// 文件说明：这个业务实现类负责分类的具体业务逻辑，并调用 Mapper 操作数据库。
package com.shiwangsi.oceanwiki.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shiwangsi.oceanwiki.entity.Category;
import com.shiwangsi.oceanwiki.mapper.CategoryMapper;
import com.shiwangsi.oceanwiki.service.ICategoryService;
import org.springframework.stereotype.Service;

// 分类业务实现类，当前主要使用 MyBatis-Plus 提供的基础方法
@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements ICategoryService {
}
