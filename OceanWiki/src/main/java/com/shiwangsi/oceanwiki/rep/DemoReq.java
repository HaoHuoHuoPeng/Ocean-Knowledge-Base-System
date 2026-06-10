// 文件说明：这个请求参数类用来接收前端提交的DemoReq表单数据。
package com.shiwangsi.oceanwiki.rep;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

/*
 * 测试接口请求参数。
 * 这里演示后端参数校验：字段不符合要求时，会进入统一异常处理。
 */
@Schema(description = "测试 sayHello 参数")
@Data
public class DemoReq {

    // @NotBlank 表示字符串不能为 null，也不能是空字符串
    @NotBlank(message = "姓名不能为空")
    @Schema(description = "姓名")
    private String name;

    // @Pattern 用正则表达式校验手机号格式
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3|4|5|6|7|8|9][0-9]{9}$", message = "手机号格式不正确")
    @Schema(description = "手机号")
    private String phone;

    // @Length 校验字符串长度，这里要求密码长度在 6 到 20 位之间
    @NotEmpty(message = "密码不能为空")
    @Length(min = 6, max = 20, message = "密码长度必须在 6 到 20 位之间")
    @Schema(description = "密码")
    private String password;


}
