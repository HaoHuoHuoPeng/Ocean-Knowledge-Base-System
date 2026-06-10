// 文件说明：这个 Mapper 负责收藏的数据库访问，继承 BaseMapper 后可以直接做基础增删改查。
package com.shiwangsi.oceanwiki.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shiwangsi.oceanwiki.entity.UserFavorite;

// 用户收藏 Mapper，负责 user_favorite 表的基础增删改查
public interface UserFavoriteMapper extends BaseMapper<UserFavorite> {
}
