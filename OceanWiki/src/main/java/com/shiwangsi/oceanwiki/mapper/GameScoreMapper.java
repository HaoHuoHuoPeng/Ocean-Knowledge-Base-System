// 文件说明：这个 Mapper 负责小游戏成绩的数据库访问，继承 BaseMapper 后可以直接做基础增删改查。
package com.shiwangsi.oceanwiki.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shiwangsi.oceanwiki.entity.GameScore;

// 小游戏成绩 Mapper
// BaseMapper 会提供新增成绩、查询成绩列表等基础方法
public interface GameScoreMapper extends BaseMapper<GameScore> {
}
