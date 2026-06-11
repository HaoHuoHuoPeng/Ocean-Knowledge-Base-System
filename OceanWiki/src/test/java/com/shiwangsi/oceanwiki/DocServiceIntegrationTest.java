// 文件说明：这个文件负责文档对应模块的代码逻辑。
package com.shiwangsi.oceanwiki;

import com.shiwangsi.oceanwiki.entity.Doc;
import com.shiwangsi.oceanwiki.resp.DocVoteResp;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// 文档业务测试
// 主要验证阅读数和点赞数这两个容易出错的计数逻辑
class DocServiceIntegrationTest extends BaseIntegrationTest {

    @Test
    void findContentShouldIncreaseViewCountEveryTime() {
        // 第一次打开正文，阅读数应该加 1
        String firstContent = docService.findContent(doc.getId());
        Doc afterFirstRead = docService.getById(doc.getId());

        // 第二次打开同一篇正文，阅读数还应该继续加 1
        String secondContent = docService.findContent(doc.getId());
        Doc afterSecondRead = docService.getById(doc.getId());

        assertThat(firstContent).contains("测试正文");
        assertThat(secondContent).contains("测试正文");
        assertThat(afterFirstRead.getViewCount()).isEqualTo(1);
        assertThat(afterSecondRead.getViewCount()).isEqualTo(2);
    }

    @Test
    void saveDocShouldKeepWangEditorHtmlContent() {
        // wangEditor 保存到后端的是 HTML 字符串，这里验证标题、表格、图片这些富文本标签不会丢失
        String richContent = """
                <h2>虎鲸资料</h2>
                <p><strong>虎鲸</strong>是海洋哺乳动物。</p>
                <table><tbody><tr><td>分类</td><td>哺乳动物</td></tr></tbody></table>
                <p><img src="data:image/png;base64,test-image" alt="虎鲸图片"></p>
                """;
        doc.setContent(richContent);
        docService.saveDoc(doc);

        String savedContent = docService.findContent(doc.getId());

        assertThat(savedContent).contains("<h2>虎鲸资料</h2>");
        assertThat(savedContent).contains("<strong>虎鲸</strong>");
        assertThat(savedContent).contains("<table>");
        assertThat(savedContent).contains("data:image/png;base64,test-image");
    }

    @Test
    void toggleVoteShouldCreateOneVoteForSameUserAndThenCancelIt() {
        // 第一次点击：当前用户没有点赞记录，所以应该点赞成功
        DocVoteResp firstVote = docService.toggleVote(doc.getId(), normalUser.getId());
        Doc afterFirstVote = docService.getById(doc.getId());

        assertThat(firstVote.getVoted()).isTrue();
        assertThat(firstVote.getVoteCount()).isEqualTo(1);
        assertThat(afterFirstVote.getVoteCount()).isEqualTo(1);
        assertThat(countDocVote(doc.getId(), normalUser.getId())).isEqualTo(1);

        // 第二次点击：当前用户已经点过赞，所以应该取消点赞
        DocVoteResp secondVote = docService.toggleVote(doc.getId(), normalUser.getId());
        Doc afterSecondVote = docService.getById(doc.getId());

        assertThat(secondVote.getVoted()).isFalse();
        assertThat(secondVote.getVoteCount()).isEqualTo(0);
        assertThat(afterSecondVote.getVoteCount()).isEqualTo(0);
        assertThat(countDocVote(doc.getId(), normalUser.getId())).isZero();
    }

    @Test
    void differentUsersShouldVoteSameDocIndependently() {
        // 两个不同用户可以分别给同一篇文档点赞
        docService.toggleVote(doc.getId(), normalUser.getId());
        docService.toggleVote(doc.getId(), anotherUser.getId());

        Doc latestDoc = docService.getById(doc.getId());

        assertThat(latestDoc.getVoteCount()).isEqualTo(2);
        assertThat(countDocVote(doc.getId(), normalUser.getId())).isEqualTo(1);
        assertThat(countDocVote(doc.getId(), anotherUser.getId())).isEqualTo(1);
    }

    @Test
    void deleteDocShouldDeleteChildrenTogether() {
        Doc parent = createDoc(ebook.getId(), "父文档");
        Doc child = new Doc();
        child.setEbookId(ebook.getId());
        child.setParent(parent.getId());
        child.setName(TEST_PREFIX + "子文档");
        child.setStatus("published");
        child.setContent("<p>子文档正文</p>");
        docService.saveDoc(child);

        docService.deleteDoc(parent.getId());

        assertThat(docService.getById(parent.getId())).isNull();
        assertThat(docService.getById(child.getId())).isNull();
        assertThat(contentService.getById(parent.getId())).isNull();
        assertThat(contentService.getById(child.getId())).isNull();
    }
}
