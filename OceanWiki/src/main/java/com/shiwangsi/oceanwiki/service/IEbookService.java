// 文件说明：这个业务接口定义电子书模块可以做哪些业务操作。
package com.shiwangsi.oceanwiki.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shiwangsi.oceanwiki.entity.Ebook;

// 电子书业务接口
public interface IEbookService extends IService<Ebook> {

    // 刷新电子书统计字段：文档数、阅读数、点赞数
    void refreshEbookInfo();
}
