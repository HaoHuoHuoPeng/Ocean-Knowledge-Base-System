// 文件说明：这个业务实现类负责操作日志的具体业务逻辑，并调用 Mapper 操作数据库。
package com.shiwangsi.oceanwiki.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shiwangsi.oceanwiki.entity.OperationLog;
import com.shiwangsi.oceanwiki.mapper.OperationLogMapper;
import com.shiwangsi.oceanwiki.service.IOperationLogService;
import org.springframework.stereotype.Service;

// 操作日志业务实现类
// 后台重要操作会记录到 operation_log 表，方便之后排查是谁操作了什么
@Service
public class OperationLogServiceImpl extends ServiceImpl<OperationLogMapper, OperationLog> implements IOperationLogService {
}
