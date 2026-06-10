// 文件说明：这个业务实现类负责统计快照的具体业务逻辑，并调用 Mapper 操作数据库。
package com.shiwangsi.oceanwiki.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shiwangsi.oceanwiki.entity.Ebook;
import com.shiwangsi.oceanwiki.entity.EbookSnapshot;
import com.shiwangsi.oceanwiki.mapper.EbookMapper;
import com.shiwangsi.oceanwiki.mapper.EbookSnapshotMapper;
import com.shiwangsi.oceanwiki.service.IEbookSnapshotService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

// 电子书统计快照业务实现类
@Service
public class EbookSnapshotServiceImpl extends ServiceImpl<EbookSnapshotMapper, EbookSnapshot> implements IEbookSnapshotService {

    private final EbookMapper ebookMapper;

    public EbookSnapshotServiceImpl(EbookMapper ebookMapper) {
        this.ebookMapper = ebookMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void genSnapshot() {
        // 先刷新 ebook 的汇总字段，再生成今天的统计快照
        ebookMapper.refreshEbookInfo();
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        List<Ebook> ebooks = ebookMapper.selectList(null);

        for (Ebook ebook : ebooks) {
            EbookSnapshot yesterdaySnapshot = this.getOne(new QueryWrapper<EbookSnapshot>()
                    .eq("ebook_id", ebook.getId())
                    .eq("date", yesterday)
                    .last("limit 1"));
            EbookSnapshot todaySnapshot = new EbookSnapshot();
            todaySnapshot.setId(IdWorker.getId());
            todaySnapshot.setEbookId(ebook.getId());
            todaySnapshot.setDate(today);

            int viewCount = safeNumber(ebook.getViewCount());
            int voteCount = safeNumber(ebook.getVoteCount());
            int yesterdayViewCount = yesterdaySnapshot == null ? 0 : safeNumber(yesterdaySnapshot.getViewCount());
            int yesterdayVoteCount = yesterdaySnapshot == null ? 0 : safeNumber(yesterdaySnapshot.getVoteCount());

            todaySnapshot.setViewCount(viewCount);
            todaySnapshot.setVoteCount(voteCount);
            // 如果删除了文档、重建了数据或重跑了初始化 SQL，今天的累计数可能小于昨天快照。
            // 这种情况不能显示成负数，今日新增最低按 0 处理。
            todaySnapshot.setViewIncrease(Math.max(0, viewCount - yesterdayViewCount));
            todaySnapshot.setVoteIncrease(Math.max(0, voteCount - yesterdayVoteCount));
            baseMapper.upsertSnapshot(todaySnapshot);
        }
    }

    @Override
    public Map<String, Object> getStatistic() {
        // 查询前先生成一次今天快照，保证首页看到的数据是最新的
        this.genSnapshot();
        return baseMapper.getStatistic();
    }

    @Override
    public List<Map<String, Object>> get30Statistic() {
        this.genSnapshot();
        return baseMapper.get30Statistic();
    }

    private int safeNumber(Integer value) {
        return value == null ? 0 : value;
    }
}
