// 文件说明：这个业务实现类负责文档点赞的具体业务逻辑，并调用 Mapper 操作数据库。
package com.shiwangsi.oceanwiki.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shiwangsi.oceanwiki.entity.DocVote;
import com.shiwangsi.oceanwiki.mapper.DocVoteMapper;
import com.shiwangsi.oceanwiki.service.IDocVoteService;
import org.springframework.stereotype.Service;

// 文档点赞记录业务实现类
// 继承 ServiceImpl 后，可以直接使用 MyBatis-Plus 的常用方法
@Service
public class DocVoteServiceImpl extends ServiceImpl<DocVoteMapper, DocVote> implements IDocVoteService {
}
