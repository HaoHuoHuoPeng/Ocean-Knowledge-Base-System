// 文件说明：这个业务实现类负责电子书的具体业务逻辑，并调用 Mapper 操作数据库。
package com.shiwangsi.oceanwiki.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shiwangsi.oceanwiki.entity.Ebook;
import com.shiwangsi.oceanwiki.mapper.EbookMapper;
import com.shiwangsi.oceanwiki.service.IEbookService;
import org.springframework.stereotype.Service;

// 电子书业务实现类
// 继承 ServiceImpl 后，可以直接使用 list、getById、saveOrUpdate、removeById 等方法
@Service
public class EbookServiceImpl extends ServiceImpl<EbookMapper, Ebook> implements IEbookService {

    @Override
    public void refreshEbookInfo() {
        // 统计字段来自 doc 表，刷新后首页卡片上的文档数、阅读数、点赞数才会准确
        baseMapper.refreshEbookInfo();
    }
}
