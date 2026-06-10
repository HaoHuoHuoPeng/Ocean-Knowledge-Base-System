// 文件说明：这个业务接口定义文档版本模块可以做哪些业务操作。
package com.shiwangsi.oceanwiki.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shiwangsi.oceanwiki.entity.DocVersion;

// 文档版本业务接口
// Controller 和其他 Service 通过它操作 doc_version 表
public interface IDocVersionService extends IService<DocVersion> {
}
