package com.yupi.springbootinit.bizmq;


import com.rabbitmq.client.Channel;
import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.constant.BiConstant;
import com.yupi.springbootinit.exception.BusinessException;
import com.yupi.springbootinit.model.entity.Chart;
import com.yupi.springbootinit.model.enums.ChartEnum;
import com.yupi.springbootinit.service.ChartService;
import com.yupi.springbootinit.utils.AiChatUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@Slf4j
public class BiMessageConsumer {
    @Resource
    private RabbitTemplate rabbitTemplate;
    @Resource
    private ChartService chartService;

    @SneakyThrows
    @RabbitListener(queues = {BiConstant.BI_QUEUE_NAME}, ackMode = "MANUAL")
    public void receiveMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTage) {
        if (StringUtils.isBlank(message)){
            //消息拒绝
            channel.basicNack(deliveryTage,false,false);
            throw  new BusinessException(ErrorCode.SYSTEM_ERROR,"消息为空");
        }
        long chartId = Long.parseLong(message);
        Chart chart = chartService.getById(chartId);
        if (chart == null){
            channel.basicNack(deliveryTage,false,false);
            throw  new BusinessException(ErrorCode.SYSTEM_ERROR,"图表信息为空");
        }

        //先修改如图表任务状态为‘执行中’ 。 等待执行成功后，修改为‘已完成’，状态修改为‘失败’。记录任务失败信息。
        Chart updateChart = new Chart();
        updateChart.setId(chart.getId());
        updateChart.setStatus(ChartEnum.RUNNING.getValue());
        boolean b = chartService.updateById(updateChart);
        if (!b){
            channel.basicNack(deliveryTage,false,false);
            handleChartUpdateError(chart.getId(),"更新图表执行状态失败");
            return;
        }

        //调用Ai
        String aiResponse = AiChatUtils.doChat(buildUserInput(chart));
        String[] split = aiResponse.split("---");
        if (split.length<3){
            channel.basicNack(deliveryTage,false,false);
            handleChartUpdateError(chart.getId(),"AI 生成错误");
            return;
        }
        String genChart = split[1].trim();
        String genResult = split[2].trim();
        updateChart.setGenChart(genChart);
        updateChart.setGenResult(genResult);
        updateChart.setStatus(ChartEnum.SUCCESSED.getValue());
        boolean updateResult = chartService.updateById(updateChart);
        if (!updateResult){
            channel.basicNack(deliveryTage,false,false);
            handleChartUpdateError(chart.getId(),"更新图表成功状态失败");
        }
        //消息确认
        channel.basicAck(deliveryTage,false);
        log.info("receiveMessage message = {}", message);

    }

    /**
     * 构造用户输入
     * @param chart
     * @return
     */
    public  String  buildUserInput(Chart chart){
        String goal = chart.getGoal();
        String chartType = chart.getChartType();
        String csvdata = chart.getChartData();
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求: ").append("\n");
        String userGoal = goal;
        if (StringUtils.isNotBlank(chartType)){
            goal+= "并使用"+chartType+"进行数据可视化";
        }
        userInput.append(chartType).append("\n");
        userInput.append("原始数据: ").append("\n");
        userInput.append(csvdata).append("\n");

        return userInput.toString();

    }

    private void  handleChartUpdateError(long chartId , String execMessag){
        Chart updateChart = new Chart();
        updateChart.setId(chartId);
        updateChart.setStatus(ChartEnum.FAILED.getValue());
        updateChart.setExecMessage(execMessag);
        boolean updateResult = chartService.updateById(updateChart);
        if (!updateResult){
            log.error("更新图表失败状态失败"+chartId+" " +execMessag);
        }
    }


}
