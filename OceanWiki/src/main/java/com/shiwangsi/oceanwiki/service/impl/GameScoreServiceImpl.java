// 文件说明：这个业务实现类负责小游戏成绩的具体业务逻辑，并调用 Mapper 操作数据库。
package com.shiwangsi.oceanwiki.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shiwangsi.oceanwiki.entity.GameScore;
import com.shiwangsi.oceanwiki.mapper.GameScoreMapper;
import com.shiwangsi.oceanwiki.service.IGameScoreService;
import org.springframework.stereotype.Service;

// 小游戏成绩业务实现类
// 继承 ServiceImpl 后，可以直接使用 save、list、getOne 等 MyBatis-Plus 方法
@Service
public class GameScoreServiceImpl extends ServiceImpl<GameScoreMapper, GameScore> implements IGameScoreService {
}
