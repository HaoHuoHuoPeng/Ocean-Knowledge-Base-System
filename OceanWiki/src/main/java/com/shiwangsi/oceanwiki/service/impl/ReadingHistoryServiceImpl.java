// 文件说明：这个业务实现类负责阅读历史的具体业务逻辑，并调用 Mapper 操作数据库。
package com.shiwangsi.oceanwiki.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shiwangsi.oceanwiki.entity.ReadingHistory;
import com.shiwangsi.oceanwiki.mapper.ReadingHistoryMapper;
import com.shiwangsi.oceanwiki.service.IReadingHistoryService;
import org.springframework.stereotype.Service;

// 阅读历史业务实现类
// 每次用户打开文档时，Controller 会调用这里提供的基础方法保存或更新历史记录
@Service
public class ReadingHistoryServiceImpl extends ServiceImpl<ReadingHistoryMapper, ReadingHistory> implements IReadingHistoryService {
}
