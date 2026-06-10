// 文件说明：这个文件负责BaseIntegrationTest对应模块的代码逻辑。
package com.shiwangsi.oceanwiki;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shiwangsi.oceanwiki.entity.*;
import com.shiwangsi.oceanwiki.service.*;
import com.shiwangsi.oceanwiki.utils.PasswordUtil;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

// 集成测试父类
// 所有测试都会连接真实 Spring 容器和当前 MySQL，测试结束后用事务回滚，避免污染你的业务数据
@SpringBootTest
@Transactional
public abstract class BaseIntegrationTest {

    protected static final String TEST_PREFIX = "test_auto_";

    @Autowired
    protected IUserService userService;

    @Autowired
    protected IRoleService roleService;

    @Autowired
    protected IPermissionService permissionService;

    @Autowired
    protected IUserRoleService userRoleService;

    @Autowired
    protected IRolePermissionService rolePermissionService;

    @Autowired
    protected IEbookService ebookService;

    @Autowired
    protected IDocService docService;

    @Autowired
    protected IContentService contentService;

    @Autowired
    protected IDocVoteService docVoteService;

    @Autowired
    protected IGameScoreService gameScoreService;

    @Autowired
    protected ISensitiveWordService sensitiveWordService;

    @Autowired
    protected IUserCommentService commentService;

    @Autowired
    protected IFeedbackReplyService feedbackReplyService;

    @Autowired
    protected IUserFeedbackService feedbackService;

    @Autowired
    protected IOperationLogService operationLogService;

    protected User normalUser;
    protected User anotherUser;
    protected Role normalRole;
    protected Permission gamePermission;
    protected Ebook ebook;
    protected Doc doc;

    @BeforeEach
    void prepareBaseData() {
        normalRole = createRole(TEST_PREFIX + "NORMAL_USER", "测试普通用户");
        gamePermission = createPermission(TEST_PREFIX + "game_play", "测试小游戏权限");
        assignPermission(normalRole, gamePermission);

        normalUser = createUser(TEST_PREFIX + "user_1", "测试用户1");
        anotherUser = createUser(TEST_PREFIX + "user_2", "测试用户2");
        assignRole(normalUser, normalRole);
        assignRole(anotherUser, normalRole);

        ebook = createEbook();
        doc = createDoc(ebook.getId(), "测试文档");
    }

    protected User createUser(String loginName, String name) {
        User user = new User();
        user.setLoginName(loginName);
        user.setName(name);
        user.setPassword(PasswordUtil.encode("123456"));
        userService.save(user);
        return user;
    }

    protected Role createRole(String code, String name) {
        Role role = new Role();
        role.setCode(code);
        role.setName(name);
        role.setDescription("测试自动创建的角色");
        role.setBuiltIn(0);
        role.setSort(99);
        roleService.save(role);
        return role;
    }

    protected Permission createPermission(String code, String name) {
        Permission permission = new Permission();
        permission.setCode(code);
        permission.setName(name);
        permission.setModule("测试模块");
        permission.setPath("/test");
        permission.setSort(99);
        permissionService.save(permission);
        return permission;
    }

    protected void assignRole(User user, Role role) {
        UserRole userRole = new UserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(role.getId());
        userRoleService.save(userRole);
    }

    protected void assignPermission(Role role, Permission permission) {
        RolePermission rolePermission = new RolePermission();
        rolePermission.setRoleId(role.getId());
        rolePermission.setPermissionId(permission.getId());
        rolePermissionService.save(rolePermission);
    }

    protected Ebook createEbook() {
        Ebook testEbook = new Ebook();
        testEbook.setName(TEST_PREFIX + "电子书");
        testEbook.setCategory1Id(100L);
        testEbook.setCategory2Id(101L);
        testEbook.setDescription("测试电子书");
        testEbook.setCover("http://example.com/test.jpg");
        testEbook.setDocCount(0);
        testEbook.setViewCount(0);
        testEbook.setVoteCount(0);
        ebookService.save(testEbook);
        return testEbook;
    }

    protected Doc createDoc(Long ebookId, String name) {
        Doc testDoc = new Doc();
        testDoc.setEbookId(ebookId);
        testDoc.setParent(0L);
        testDoc.setName(TEST_PREFIX + name);
        testDoc.setSort(1);
        testDoc.setViewCount(0);
        testDoc.setVoteCount(0);
        testDoc.setStatus("published");
        testDoc.setContent("<p>测试正文</p>");
        docService.saveDoc(testDoc);
        return testDoc;
    }

    protected GameScore createGameScore(Long userId, int score, int accuracy, int maxCombo) {
        GameScore gameScore = new GameScore();
        gameScore.setUserId(userId);
        gameScore.setGameCode("piano_tiles");
        gameScore.setScore(score);
        gameScore.setTotalPressCount(Math.max(score, 1));
        gameScore.setWrongCount(0);
        gameScore.setAccuracy(accuracy);
        gameScore.setMaxCombo(maxCombo);
        gameScore.setReason("测试");
        gameScore.setCreateTime(LocalDateTime.now());
        gameScoreService.save(gameScore);
        return gameScore;
    }

    protected long countDocVote(Long docId, Long userId) {
        return docVoteService.count(new QueryWrapper<DocVote>()
                .eq("doc_id", docId)
                .eq("user_id", userId));
    }
}
