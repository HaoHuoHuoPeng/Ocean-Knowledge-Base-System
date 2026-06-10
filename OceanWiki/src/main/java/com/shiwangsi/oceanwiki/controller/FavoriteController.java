// 文件说明：这个 Controller 负责收藏和收藏分组相关接口，前端请求会先进入这里再调用业务层处理。
package com.shiwangsi.oceanwiki.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shiwangsi.oceanwiki.entity.Doc;
import com.shiwangsi.oceanwiki.entity.Ebook;
import com.shiwangsi.oceanwiki.entity.UserFavorite;
import com.shiwangsi.oceanwiki.entity.UserFavoriteFolder;
import com.shiwangsi.oceanwiki.rep.FavoriteReq;
import com.shiwangsi.oceanwiki.resp.CommonResp;
import com.shiwangsi.oceanwiki.resp.FavoriteStatusResp;
import com.shiwangsi.oceanwiki.service.IDocService;
import com.shiwangsi.oceanwiki.service.IEbookService;
import com.shiwangsi.oceanwiki.service.IUserFavoriteFolderService;
import com.shiwangsi.oceanwiki.service.IUserFavoriteService;
import com.shiwangsi.oceanwiki.utils.AuthUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

// 用户收藏接口
// 普通用户可以收藏电子书或文档，并按自己的收藏分组进行整理
@Tag(name = "用户收藏接口")
@RestController
@RequestMapping("/favorite")
public class FavoriteController {

    private static final String TARGET_EBOOK = "ebook";
    private static final String TARGET_DOC = "doc";
    private static final String DEFAULT_FOLDER = "默认分组";

    private final IUserFavoriteService favoriteService;
    private final IUserFavoriteFolderService favoriteFolderService;
    private final IEbookService ebookService;
    private final IDocService docService;

    public FavoriteController(IUserFavoriteService favoriteService,
                              IUserFavoriteFolderService favoriteFolderService,
                              IEbookService ebookService,
                              IDocService docService) {
        this.favoriteService = favoriteService;
        this.favoriteFolderService = favoriteFolderService;
        this.ebookService = ebookService;
        this.docService = docService;
    }

    // 收藏或取消收藏
    // 收藏时只能放入当前用户已经创建好的分组；取消收藏时直接删除这条收藏记录
    @Operation(summary = "收藏或取消收藏")
    @PostMapping("/toggle")
    public CommonResp<FavoriteStatusResp> toggle(@RequestBody FavoriteReq req, HttpServletRequest request) {
        Long userId = AuthUtil.requireLogin(request);
        checkTarget(req.getTargetType(), req.getTargetId());
        ensureDefaultFolder(userId);

        String folderName = normalizeFolderName(req.getFolderName());
        if (!hasFolder(userId, folderName)) {
            return CommonResp.fail("请先在我的收藏里新增该分组");
        }

        UserFavorite favorite = getFavorite(userId, req.getTargetType(), req.getTargetId());
        boolean favorited;
        if (favorite == null) {
            UserFavorite newFavorite = new UserFavorite();
            newFavorite.setUserId(userId);
            newFavorite.setTargetType(req.getTargetType());
            newFavorite.setTargetId(req.getTargetId());
            newFavorite.setFolderName(folderName);
            newFavorite.setCreateTime(LocalDateTime.now());
            favoriteService.save(newFavorite);
            favorited = true;
        } else {
            favoriteService.removeById(favorite.getId());
            favorited = false;
            folderName = favorite.getFolderName();
        }

        FavoriteStatusResp resp = new FavoriteStatusResp();
        resp.setFavorited(favorited);
        resp.setFolderName(folderName);
        return CommonResp.ok(favorited ? "收藏成功" : "已取消收藏", resp);
    }

    // 查询当前用户是否已经收藏某个对象
    @Operation(summary = "查询收藏状态")
    @GetMapping("/status")
    public CommonResp<FavoriteStatusResp> status(String targetType, Long targetId, HttpServletRequest request) {
        Long userId = AuthUtil.requireLogin(request);
        ensureDefaultFolder(userId);

        UserFavorite favorite = getFavorite(userId, targetType, targetId);
        FavoriteStatusResp resp = new FavoriteStatusResp();
        resp.setFavorited(favorite != null);
        resp.setFolderName(favorite == null ? DEFAULT_FOLDER : normalizeFolderName(favorite.getFolderName()));
        return CommonResp.ok(resp);
    }

    // 查询我的收藏列表
    @Operation(summary = "查询我的收藏")
    @GetMapping("/my")
    public CommonResp<List<UserFavorite>> my(HttpServletRequest request) {
        Long userId = AuthUtil.requireLogin(request);
        ensureDefaultFolder(userId);

        List<UserFavorite> list = favoriteService.list(new QueryWrapper<UserFavorite>()
                .eq("user_id", userId)
                .orderByAsc("folder_name")
                .orderByDesc("create_time"));
        list.forEach(this::fillTargetName);
        return CommonResp.ok(list);
    }

    // 查询我的收藏分组
    @Operation(summary = "查询我的收藏分组")
    @GetMapping("/folders")
    public CommonResp<List<UserFavoriteFolder>> folders(HttpServletRequest request) {
        Long userId = AuthUtil.requireLogin(request);
        ensureDefaultFolder(userId);

        List<UserFavoriteFolder> list = favoriteFolderService.list(new QueryWrapper<UserFavoriteFolder>()
                .eq("user_id", userId)
                .orderByAsc("create_time")
                .orderByAsc("id"));
        return CommonResp.ok(list);
    }

    // 新增我的收藏分组
    @Operation(summary = "新增我的收藏分组")
    @PostMapping("/folders")
    public CommonResp<UserFavoriteFolder> addFolder(@RequestBody Map<String, Object> req, HttpServletRequest request) {
        Long userId = AuthUtil.requireLogin(request);
        ensureDefaultFolder(userId);

        String folderName = normalizeFolderName(String.valueOf(req.getOrDefault("name", "")));
        if (!StringUtils.hasText(folderName)) {
            return CommonResp.fail("分组名称不能为空");
        }
        if (hasFolder(userId, folderName)) {
            return CommonResp.fail("该收藏分组已存在");
        }

        UserFavoriteFolder folder = new UserFavoriteFolder();
        folder.setUserId(userId);
        folder.setName(folderName);
        folder.setCreateTime(LocalDateTime.now());
        favoriteFolderService.save(folder);
        return CommonResp.ok("分组创建成功", folder);
    }

    // 删除收藏分组
    // 删除分组时，会把这个分组下面的收藏记录一起删除
    @Operation(summary = "删除我的收藏分组")
    @DeleteMapping("/folders/{id}")
    @Transactional(rollbackFor = Exception.class)
    public CommonResp<Object> deleteFolder(@PathVariable Long id, HttpServletRequest request) {
        Long userId = AuthUtil.requireLogin(request);
        ensureDefaultFolder(userId);

        UserFavoriteFolder folder = favoriteFolderService.getOne(new QueryWrapper<UserFavoriteFolder>()
                .eq("id", id)
                .eq("user_id", userId));
        if (folder == null) {
            return CommonResp.fail("收藏分组不存在");
        }
        String folderName = normalizeFolderName(folder.getName());
        if (DEFAULT_FOLDER.equals(folderName)) {
            return CommonResp.fail("默认分组不能删除");
        }

        favoriteService.remove(new QueryWrapper<UserFavorite>()
                .eq("user_id", userId)
                .eq("folder_name", folderName));
        favoriteFolderService.removeById(folder.getId());
        return CommonResp.ok("分组和分组内收藏已删除", null);
    }

    // 修改收藏记录所属分组
    @Operation(summary = "修改收藏分组")
    @PostMapping("/folder")
    public CommonResp<Object> updateFolder(@RequestBody Map<String, Object> req, HttpServletRequest request) {
        Long userId = AuthUtil.requireLogin(request);
        ensureDefaultFolder(userId);

        Long favoriteId = Long.valueOf(String.valueOf(req.get("id")));
        String folderName = normalizeFolderName(String.valueOf(req.getOrDefault("folderName", DEFAULT_FOLDER)));
        if (!hasFolder(userId, folderName)) {
            return CommonResp.fail("请先新增该收藏分组");
        }

        UserFavorite favorite = favoriteService.getOne(new QueryWrapper<UserFavorite>()
                .eq("id", favoriteId)
                .eq("user_id", userId));
        if (favorite == null) {
            return CommonResp.fail("收藏不存在");
        }
        favorite.setFolderName(folderName);
        favoriteService.updateById(favorite);
        return CommonResp.ok("分组已更新", null);
    }

    private UserFavorite getFavorite(Long userId, String targetType, Long targetId) {
        return favoriteService.getOne(new QueryWrapper<UserFavorite>()
                .eq("user_id", userId)
                .eq("target_type", targetType)
                .eq("target_id", targetId));
    }

    private void checkTarget(String targetType, Long targetId) {
        if (targetId == null) {
            throw new IllegalArgumentException("收藏对象不能为空");
        }
        if (TARGET_EBOOK.equals(targetType) && ebookService.getById(targetId) != null) {
            return;
        }
        if (TARGET_DOC.equals(targetType) && docService.getById(targetId) != null) {
            return;
        }
        throw new IllegalArgumentException("收藏对象不存在");
    }

    private void fillTargetName(UserFavorite favorite) {
        if (TARGET_EBOOK.equals(favorite.getTargetType())) {
            Ebook ebook = ebookService.getById(favorite.getTargetId());
            favorite.setTargetName(ebook == null ? "电子书已删除" : ebook.getName());
            favorite.setEbookId(favorite.getTargetId());
        }
        if (TARGET_DOC.equals(favorite.getTargetType())) {
            Doc doc = docService.getById(favorite.getTargetId());
            favorite.setTargetName(doc == null ? "文档已删除" : doc.getName());
            favorite.setEbookId(doc == null ? null : doc.getEbookId());
        }
    }

    private void ensureDefaultFolder(Long userId) {
        if (hasFolder(userId, DEFAULT_FOLDER)) {
            return;
        }
        UserFavoriteFolder folder = new UserFavoriteFolder();
        folder.setUserId(userId);
        folder.setName(DEFAULT_FOLDER);
        folder.setCreateTime(LocalDateTime.now());
        favoriteFolderService.save(folder);
    }

    private boolean hasFolder(Long userId, String folderName) {
        return favoriteFolderService.count(new QueryWrapper<UserFavoriteFolder>()
                .eq("user_id", userId)
                .eq("name", normalizeFolderName(folderName))) > 0;
    }

    private String normalizeFolderName(String folderName) {
        if (!StringUtils.hasText(folderName)) {
            return DEFAULT_FOLDER;
        }
        String name = folderName.trim();
        return name.length() > 50 ? name.substring(0, 50) : name;
    }
}
