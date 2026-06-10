// 文件说明：这个文件负责OceanWikiApplicationTests对应模块的代码逻辑。
package com.shiwangsi.oceanwiki;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shiwangsi.oceanwiki.entity.User;
import com.shiwangsi.oceanwiki.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class OceanWikiApplicationTests {

    @Autowired
    private UserMapper userMapper;

    @Test
    void contextLoads() {
    }

    @Test
    void queryUser() {
        // 查询 user 表中的所有数据，用来验证 MyBatis-Plus 是否配置成功
        List<User> users = userMapper.selectList(new QueryWrapper<>());
        System.out.println(users);
    }
}
