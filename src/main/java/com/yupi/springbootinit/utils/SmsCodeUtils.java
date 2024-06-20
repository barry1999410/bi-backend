package com.yupi.springbootinit.utils;

import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.dysmsapi20170525.models.SendSmsResponseBody;
import com.aliyun.tea.TeaException;
import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.config.AliSmsConfig;
import com.yupi.springbootinit.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * 阿里云sms发送短信api
 */

@Component
public class SmsCodeUtils {

    @Resource
    private  AliSmsConfig aliSmsConfig;

   @Resource
   private RedisTemplate<String,String> redisTemplate;

    public  String sendCode(String phone, String code) throws Exception {
        com.aliyun.dysmsapi20170525.Client client = aliSmsConfig.createClient();
        com.aliyun.dysmsapi20170525.models.SendSmsRequest sendSmsRequest = new com.aliyun.dysmsapi20170525.models.SendSmsRequest()
                .setSignName("阿里云短信测试")
                .setTemplateCode("SMS_154950909")
                .setPhoneNumbers(phone)
                .setTemplateParam(String.format("{\"code\":\"%s\"}", code));
        com.aliyun.teautil.models.RuntimeOptions runtime = new com.aliyun.teautil.models.RuntimeOptions();
        SendSmsResponseBody responseBody = new SendSmsResponseBody();
        try {
            // 复制代码运行请自行打印 API 的返回值
            SendSmsResponse sendSmsResponse = client.sendSmsWithOptions(sendSmsRequest, runtime);
            String message = sendSmsResponse.getBody().getMessage();
            if (!StringUtils.equals("OK", message)) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统错误，请重试！");
            }
        } catch (TeaException error) {
            // 此处仅做打印展示，请谨慎对待异常处理，在工程项目中切勿直接忽略异常。
            // 错误 message
            System.out.println(error.getMessage());
            // 诊断地址
            System.out.println(error.getData().get("Recommend"));
            com.aliyun.teautil.Common.assertAsString(error.message);
        } catch (Exception _error) {
            TeaException error = new TeaException(_error.getMessage(), _error);
            // 此处仅做打印展示，请谨慎对待异常处理，在工程项目中切勿直接忽略异常。
            // 错误 message
            System.out.println(error.getMessage());
            // 诊断地址
            System.out.println(error.getData().get("Recommend"));
            com.aliyun.teautil.Common.assertAsString(error.message);
        }
        return code;
    }

    /**
     *redis存储短信
     * @param phoneNumber
     * @param code
     */
    public void saveVerificationCode(String phoneNumber, String code) {
        String key = "SMS_verification_code:" + phoneNumber;
        redisTemplate.opsForValue().set(key, code, 60, TimeUnit.SECONDS); // 设置过期时间为3分钟
    }


    public Boolean getVerificationCodeAndCheck(String phoneNumber, String code) {
        String key = "SMS_verification_code:" + phoneNumber;
        String SmSVerificationCode = null;
        try {
            SmSVerificationCode = redisTemplate.opsForValue().get(key);
            if (SmSVerificationCode == null){
                throw  new BusinessException(ErrorCode.SYSTEM_ERROR,"验证码验证失败，请重试");
            }
            if (StringUtils.equals(code,SmSVerificationCode)){
                return true;
            }
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"验证码错误！");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
