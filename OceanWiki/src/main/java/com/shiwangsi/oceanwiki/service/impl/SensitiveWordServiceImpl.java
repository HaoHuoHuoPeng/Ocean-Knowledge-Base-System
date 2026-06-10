// 文件说明：这个业务实现类负责敏感词的具体业务逻辑，并调用 Mapper 操作数据库。
package com.shiwangsi.oceanwiki.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shiwangsi.oceanwiki.entity.SensitiveWord;
import com.shiwangsi.oceanwiki.mapper.SensitiveWordMapper;
import com.shiwangsi.oceanwiki.service.ISensitiveWordService;
import org.springframework.stereotype.Service;

// 敏感词业务实现类
@Service
public class SensitiveWordServiceImpl extends ServiceImpl<SensitiveWordMapper, SensitiveWord> implements ISensitiveWordService {
}
