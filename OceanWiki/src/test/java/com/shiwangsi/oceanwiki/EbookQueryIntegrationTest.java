// 文件说明：这个测试类负责验证电子书查询和推荐接口，重点覆盖分类层级和推荐数量限制。
package com.shiwangsi.oceanwiki;

import com.shiwangsi.oceanwiki.controller.EbookController;
import com.shiwangsi.oceanwiki.entity.Category;
import com.shiwangsi.oceanwiki.entity.Ebook;
import com.shiwangsi.oceanwiki.resp.CommonResp;
import com.shiwangsi.oceanwiki.resp.ListByPageResp;
import com.shiwangsi.oceanwiki.service.ICategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// 电子书查询测试
// 当前系统的分类可能有很多级，所以要验证按父分类查询时能找到子孙分类下的电子书
class EbookQueryIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ICategoryService categoryService;

    @Autowired
    private EbookController ebookController;

    @Test
    void listByPageShouldFindEbookUnderDeepChildCategory() {
        Category root = createCategory(0L, TEST_PREFIX + "一级分类", 1);
        Category child = createCategory(root.getId(), TEST_PREFIX + "二级分类", 1);
        Category grandChild = createCategory(child.getId(), TEST_PREFIX + "三级分类", 1);
        Ebook childEbook = createPublishedEbook(TEST_PREFIX + "多级分类电子书", root.getId(), child.getId(), grandChild.getId());

        CommonResp<ListByPageResp<Ebook>> resp = ebookController.listByPage(1, 10, null, root.getId(), "published");
        List<Ebook> list = resp.getContent().getList();

        assertThat(resp.isSuccess()).isTrue();
        assertThat(list).extracting(Ebook::getId).contains(childEbook.getId());
    }

    @Test
    void recommendShouldOnlyReturnRequestedLimit() {
        createPublishedEbook(TEST_PREFIX + "推荐电子书1", null, null, null);
        createPublishedEbook(TEST_PREFIX + "推荐电子书2", null, null, null);
        createPublishedEbook(TEST_PREFIX + "推荐电子书3", null, null, null);
        createPublishedEbook(TEST_PREFIX + "推荐电子书4", null, null, null);

        CommonResp<List<Ebook>> resp = ebookController.recommend(3, new MockHttpServletRequest());

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getContent()).hasSizeLessThanOrEqualTo(3);
    }

    private Category createCategory(Long parent, String name, Integer sort) {
        Category category = new Category();
        category.setParent(parent);
        category.setName(name);
        category.setSort(sort);
        categoryService.save(category);
        return category;
    }

    private Ebook createPublishedEbook(String name, Long category1Id, Long category2Id, Long categoryId) {
        Ebook testEbook = new Ebook();
        testEbook.setName(name);
        testEbook.setCategory1Id(category1Id);
        testEbook.setCategory2Id(category2Id);
        testEbook.setCategoryId(categoryId);
        testEbook.setDescription("测试电子书查询");
        testEbook.setCover("http://example.com/query-test.jpg");
        testEbook.setStatus("published");
        testEbook.setDocCount(0);
        testEbook.setViewCount(0);
        testEbook.setVoteCount(0);
        ebookService.save(testEbook);
        return testEbook;
    }
}
