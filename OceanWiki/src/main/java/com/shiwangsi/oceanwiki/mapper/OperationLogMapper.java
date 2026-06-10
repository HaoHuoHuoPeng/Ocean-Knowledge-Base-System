// 文件说明：这个 Mapper 负责操作日志的数据库访问，继承 BaseMapper 后可以直接做基础增删改查。
package com.shiwangsi.oceanwiki.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shiwangsi.oceanwiki.entity.OperationLog;

// 操作日志 Mapper，负责 operation_log 表的基础增删改查
public interface OperationLogMapper extends BaseMapper<OperationLog> {
}
