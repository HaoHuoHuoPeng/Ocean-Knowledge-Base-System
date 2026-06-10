// 文件说明：这个 Mapper 负责通知的数据库访问，继承 BaseMapper 后可以直接做基础增删改查。
package com.shiwangsi.oceanwiki.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shiwangsi.oceanwiki.entity.UserNotice;

// 用户通知 Mapper，负责 user_notice 表的基础增删改查
public interface UserNoticeMapper extends BaseMapper<UserNotice> {
}
