// 文件说明：这个 Mapper 负责电子书的数据库访问，继承 BaseMapper 后可以直接做基础增删改查。
package com.shiwangsi.oceanwiki.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shiwangsi.oceanwiki.entity.Ebook;

// 电子书 Mapper
// BaseMapper 已经提供 insert、delete、update、selectById 等基础方法
public interface EbookMapper extends BaseMapper<Ebook> {

    // 根据 doc 表汇总结果，刷新每本电子书的文档数、阅读数、点赞数
    void refreshEbookInfo();
}
