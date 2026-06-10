// 文件说明：这个 Controller 负责通知相关接口，前端请求会先进入这里再调用业务层处理。
package com.shiwangsi.oceanwiki.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shiwangsi.oceanwiki.entity.UserNotice;
import com.shiwangsi.oceanwiki.resp.CommonResp;
import com.shiwangsi.oceanwiki.service.IUserNoticeService;
import com.shiwangsi.oceanwiki.utils.AuthUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// 用户通知接口
// 审核、反馈处理等结果会进入通知列表，普通用户可以在个人中心查看
@Tag(name = "用户通知接口")
@RestController
@RequestMapping("/notice")
public class NoticeController {

    private final IUserNoticeService noticeService;

    public NoticeController(IUserNoticeService noticeService) {
        this.noticeService = noticeService;
    }

    // 查询我的通知
    @Operation(summary = "查询我的通知")
    @GetMapping("/my")
    public CommonResp<List<UserNotice>> my(HttpServletRequest request) {
        Long userId = AuthUtil.requireLogin(request);
        return CommonResp.ok(noticeService.list(new QueryWrapper<UserNotice>()
                .eq("user_id", userId)
                .orderByAsc("read_flag")
                .orderByDesc("create_time")));
    }

    // 标记单条通知为已读
    @Operation(summary = "标记通知已读")
    @PostMapping("/read/{id}")
    public CommonResp<Object> read(@PathVariable Long id, HttpServletRequest request) {
        Long userId = AuthUtil.requireLogin(request);
        UserNotice notice = noticeService.getOne(new QueryWrapper<UserNotice>()
                .eq("id", id)
                .eq("user_id", userId));
        if (notice != null) {
            notice.setReadFlag(1);
            noticeService.updateById(notice);
        }
        return CommonResp.ok("已读成功", null);
    }

    // 标记自己的全部通知为已读
    @Operation(summary = "全部通知已读")
    @PostMapping("/readAll")
    public CommonResp<Object> readAll(HttpServletRequest request) {
        Long userId = AuthUtil.requireLogin(request);
        List<UserNotice> notices = noticeService.list(new QueryWrapper<UserNotice>()
                .eq("user_id", userId)
                .eq("read_flag", 0));
        notices.forEach(notice -> notice.setReadFlag(1));
        noticeService.updateBatchById(notices);
        return CommonResp.ok("全部已读成功", null);
    }
}
