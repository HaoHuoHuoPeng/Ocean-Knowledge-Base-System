// 文件说明：这个业务接口定义统计快照模块可以做哪些业务操作。
package com.shiwangsi.oceanwiki.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shiwangsi.oceanwiki.entity.EbookSnapshot;

import java.util.List;
import java.util.Map;

// 统计快照业务接口
public interface IEbookSnapshotService extends IService<EbookSnapshot> {

    // 生成今天的快照
    void genSnapshot();

    // 首页统计总览
    Map<String, Object> getStatistic();

    // 最近 30 天统计
    List<Map<String, Object>> get30Statistic();
}
