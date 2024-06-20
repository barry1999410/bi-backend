package com.yupi.springbootinit.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.yupi.springbootinit.model.entity.DialogueResponse;
import com.yupi.springbootinit.service.AICallback;
import com.zhipu.oapi.ClientV4;
import com.zhipu.oapi.Constants;
import com.zhipu.oapi.service.v4.model.*;
import io.reactivex.Flowable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;



public class AiChatUtils {
    private final static Logger logger = LoggerFactory.getLogger(AiChatUtils.class);
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

    /**
     * glm3同步调用
     */
    // public static void testInvoke() throws JsonProcessingException {
    //     List<ChatMessage> messages = new ArrayList<>();
    //     ChatMessage chatMessage = new ChatMessage(ChatMessageRole.USER.value(), "你和苏梦远是什么关系");
    //     messages.add(chatMessage);
    //     String requestId = String.format(requestIdTemplate, System.currentTimeMillis());
    //
    //
    //     // 函数调用参数构建部分
    //     HashMap<String, Object> extraJson = new HashMap<>();
    //     extraJson.put("temperature", 0.5);
    //     ChatMeta meta = new ChatMeta();
    //     meta.setUser_info("我是陆星辰，是一个男性，是一位知名导演，也是苏梦远的合作导演。我擅长拍摄音乐题材的电影。苏梦远对我的态度是尊敬的，并视我为良师益友。");
    //     meta.setBot_info("苏梦远，本名苏远心，是一位当红的国内女歌手及演员。在参加选秀节目后，凭借独特的嗓音及出众的舞台魅力迅速成名，进入娱乐圈。她外表美丽动人，但真正的魅力在于她的才华和勤奋。苏梦远是音乐学院毕业的优秀生，善于创作，拥有多首热门原创歌曲。除了音乐方面的成就，她还热衷于慈善事业，积极参加公益活动，用实际行动传递正能量。在工作中，她对待工作非常敬业，拍戏时总是全身心投入角色，赢得了业内人士的赞誉和粉丝的喜爱。虽然在娱乐圈，但她始终保持低调、谦逊的态度，深得同行尊重。在表达时，苏梦远喜欢使用“我们”和“一起”，强调团队精神。");
    //     meta.setBot_name("苏梦远");
    //     meta.setUser_name("陆星辰");
    //
    //     ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
    //             .model(Constants.ModelCharGLM3)
    //             .stream(Boolean.FALSE)
    //             .invokeMethod(Constants.invokeMethod)
    //             .messages(messages)
    //             .requestId(requestId)
    //             .meta(meta)
    //             .extraJson(extraJson)
    //             .build();
    //     ModelApiResponse sseModelApiResp = client.invokeModelApi(chatCompletionRequest);
    //
    //     logger.info("model output: {}", mapper.writeValueAsString(sseModelApiResp));
    // }

    /**
     * glm3同步调用
     */
    public static String doChat(String content)  {
        List<ChatMessage> messages = new ArrayList<>();
        ChatMessage chatSysMessage = new ChatMessage(ChatMessageRole.SYSTEM.value(),
                "作为一位高级数据分析师兼前端开发专家，接下来我会按照以下固定格式给你提供内容: \n"+
                        "分析需求: \n"+
                        "{数据分析需求或者目标}\n"+
                        "原始数据: \n"+
                        "{csv格式的原始数据，用,作为分隔符}\n"+
                        "请根据以上两部分内容，按照以下格式严格生成内容，生成的内容禁止```符号和javascript字母的出现\n"+
                        "---\n"+
                        "{前端Echarts v5版本的js的option的完整代码，代码只包含option配置对象中的属性}\n"+
                        "---\n"+
                        "{精确、详细的数据分析结论，可以进行一定的拓展，不要生成多余的注释}"
        );

        ChatMessage chaUsertMessage = new ChatMessage(ChatMessageRole.USER.value(), content);
        messages.add(chatSysMessage);
        messages.add(chaUsertMessage);
        String requestId = String.format(requestIdTemplate, System.currentTimeMillis());

        // 函数调用参数构建部分
        HashMap<String, Object> extraJson = new HashMap<>();
        extraJson.put("temperature", 0.5);

        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
                .model(Constants.ModelChatGLM3TURBO)
                .stream(Boolean.FALSE)
                .invokeMethod(Constants.invokeMethod)
                .messages(messages)
                .requestId(requestId)
                // .extraJson(extraJson)
                .build();
        ModelApiResponse sseModelApiResp = client.invokeModelApi(chatCompletionRequest);
        String data = (String) sseModelApiResp.getData().getChoices().get(0).getMessage().getContent();

        logger.info("model output: {}",content);
        return data;
    }

    /**
     * sse-V4：非function调用
     */
    public static void testNonFunctionSSE1(String content, AICallback callback) throws JsonProcessingException {
        List<ChatMessage> messages = new ArrayList<>();
        ChatMessage chatMessage = new ChatMessage(ChatMessageRole.USER.value(), content);
        messages.add(chatMessage);
        HashMap<String, Object> extraJson = new HashMap<>();
        extraJson.put("temperature", 0.5);

        String requestId = String.format(requestIdTemplate, System.currentTimeMillis());
        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
                .model(Constants.ModelChatGLM4)
                .stream(Boolean.TRUE)
                .messages(messages)
                .requestId(requestId)
                .extraJson(extraJson)
                .build();
        ModelApiResponse sseModelApiResp = client.invokeModelApi(chatCompletionRequest);
        // stream 处理方法
        if (sseModelApiResp.isSuccess()) {
            AtomicBoolean isFirst = new AtomicBoolean(true);
            List<Choice> choices = new ArrayList<>();
            ChatMessageAccumulator chatMessageAccumulator = mapStreamToAccumulator(sseModelApiResp.getFlowable())
                    .doOnNext(accumulator -> {
                        {
                            if (isFirst.getAndSet(false)) {
                                logger.info("Response: ");
                            }
                            if (accumulator.getDelta() != null && accumulator.getDelta().getTool_calls() != null) {
                                String jsonString = mapper.writeValueAsString(accumulator.getDelta().getTool_calls());
                                logger.info("tool_calls: {}", jsonString);
                            }
                            if (accumulator.getDelta() != null && accumulator.getDelta().getContent() != null) {
                                logger.info("accumulator.getDelta().getContent(): {}", accumulator.getDelta().getContent());

                                DialogueResponse dialogueResponse = new DialogueResponse();
                                dialogueResponse.setData(accumulator.getDelta().getContent());

                                //触发一个回调函数。这个回调函数负责向 SseEmitter 发送数据。
                                callback.onDataReceived(dialogueResponse);
                            }
                            choices.add(accumulator.getChoice());
                        }
                    })
                    .doOnComplete(System.out::println)
                    .lastElement()
                    .blockingGet();

            ModelData data = new ModelData();
            data.setChoices(choices);
            data.setUsage(chatMessageAccumulator.getUsage());
            data.setId(chatMessageAccumulator.getId());
            data.setCreated(chatMessageAccumulator.getCreated());
            data.setRequestId(chatCompletionRequest.getRequestId());
            sseModelApiResp.setFlowable(null);// 打印前置空
            sseModelApiResp.setData(data);

        }
        // logger.info("model output: {}", sseModelApiResp.getData().getChoices());

    }


    //流式处理
    public static Flowable<ChatMessageAccumulator> mapStreamToAccumulator(Flowable<ModelData> flowable) {
        return flowable.map(chunk -> {
            return new ChatMessageAccumulator(chunk.getChoices().get(0).getDelta(), null, chunk.getChoices().get(0), chunk.getUsage(), chunk.getCreated(), chunk.getId());
        });
    }



}

