package com.yupi.springbootinit.controller;

import com.yupi.springbootinit.common.BaseResponse;
import com.yupi.springbootinit.common.ResultUtils;
import com.yupi.springbootinit.model.dto.smscode.SmsCodeRequest;
import com.yupi.springbootinit.utils.ComUtils;
import com.yupi.springbootinit.utils.SmsCodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 微信公众号相关接口
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 **/
@RestController
@RequestMapping("/sms")
@Slf4j
public class AliSmsController {

    @Resource
    private  SmsCodeUtils smsCodeUtils;
    @PostMapping("/code")
    public BaseResponse<String> sendSmsCode(@RequestBody SmsCodeRequest smsCodeRequest){
        String phone = smsCodeRequest.getPhone();
        String Code = ComUtils.generateVerificationCode();
        try {
             Code = smsCodeUtils.sendCode(phone, Code);
             smsCodeUtils.saveVerificationCode(phone,Code);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return ResultUtils.success(Code);
    }

}
