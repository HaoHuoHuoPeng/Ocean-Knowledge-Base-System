// 文件说明：这个业务实现类负责收藏分组的具体数据库操作。
package com.shiwangsi.oceanwiki.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shiwangsi.oceanwiki.entity.UserFavoriteFolder;
import com.shiwangsi.oceanwiki.mapper.UserFavoriteFolderMapper;
import com.shiwangsi.oceanwiki.service.IUserFavoriteFolderService;
import org.springframework.stereotype.Service;

// 用户收藏分组业务实现类
@Service
public class UserFavoriteFolderServiceImpl extends ServiceImpl<UserFavoriteFolderMapper, UserFavoriteFolder> implements IUserFavoriteFolderService {
}
