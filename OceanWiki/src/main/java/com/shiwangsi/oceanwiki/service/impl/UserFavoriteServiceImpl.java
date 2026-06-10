// 文件说明：这个业务实现类负责收藏的具体业务逻辑，并调用 Mapper 操作数据库。
package com.shiwangsi.oceanwiki.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shiwangsi.oceanwiki.entity.UserFavorite;
import com.shiwangsi.oceanwiki.mapper.UserFavoriteMapper;
import com.shiwangsi.oceanwiki.service.IUserFavoriteService;
import org.springframework.stereotype.Service;

// 用户收藏业务实现类
// 这里继承 ServiceImpl 后，就自动拥有 save、remove、list、getById 等常用方法
@Service
public class UserFavoriteServiceImpl extends ServiceImpl<UserFavoriteMapper, UserFavorite> implements IUserFavoriteService {
}
