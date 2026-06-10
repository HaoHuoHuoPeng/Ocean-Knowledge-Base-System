// 文件说明：这个业务接口定义小游戏成绩模块可以做哪些业务操作。
package com.shiwangsi.oceanwiki.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shiwangsi.oceanwiki.entity.GameScore;

// 小游戏成绩业务接口
// Controller 调用 Service，避免直接把 Mapper 暴露到控制层
public interface IGameScoreService extends IService<GameScore> {
}
