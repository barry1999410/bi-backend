package com.yupi.springbootinit.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.zhipu.oapi.ClientV4;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.TimeUnit;



/**
 * EasyExcel 测试
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@SpringBootTest
public class V4Test {

    private static final boolean devMode = false;

    private static final ClientV4 client = new ClientV4.Builder("61a46a3869139c304d2dc833bb2873cf.tJPsjjWyfdJxZ3Ip")
            .devMode(devMode)
            .enableTokenCache()
            .networkConfig(300, 100, 100, 100, TimeUnit.SECONDS)
            .connectionPool(new okhttp3.ConnectionPool(8, 1, TimeUnit.SECONDS))
            .build();


    // 请自定义自己的业务id
    private static final String requestIdTemplate = "mycompany-%d";

    private static final ObjectMapper mapper = defaultObjectMapper();



    public static ObjectMapper defaultObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        return mapper;
    }



    @Test
    public void aiTest() {
        AiChatUtils.doChat(null);

    }
}
