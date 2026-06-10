// 文件说明：这个业务实现类负责文档版本记录的具体业务逻辑。
package com.shiwangsi.oceanwiki.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shiwangsi.oceanwiki.entity.DocVersion;
import com.shiwangsi.oceanwiki.mapper.DocVersionMapper;
import com.shiwangsi.oceanwiki.service.IDocVersionService;
import org.springframework.stereotype.Service;

// 文档版本业务实现类
// 目前主要使用 MyBatis-Plus 默认方法，后续复杂版本逻辑可以继续写在这里
@Service
public class DocVersionServiceImpl extends ServiceImpl<DocVersionMapper, DocVersion> implements IDocVersionService {
}
