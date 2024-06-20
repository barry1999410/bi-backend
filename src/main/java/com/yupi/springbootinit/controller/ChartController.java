package com.yupi.springbootinit.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.springbootinit.annotation.AuthCheck;
import com.yupi.springbootinit.bizmq.BiMessageProducer;
import com.yupi.springbootinit.common.BaseResponse;
import com.yupi.springbootinit.common.DeleteRequest;
import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.common.ResultUtils;
import com.yupi.springbootinit.constant.CommonConstant;
import com.yupi.springbootinit.constant.UserConstant;
import com.yupi.springbootinit.exception.BusinessException;
import com.yupi.springbootinit.exception.ThrowUtils;
import com.yupi.springbootinit.model.dto.chart.*;
import com.yupi.springbootinit.model.entity.Chart;
import com.yupi.springbootinit.model.entity.User;
import com.yupi.springbootinit.model.enums.ChartEnum;
import com.yupi.springbootinit.model.vo.BiResponse;
import com.yupi.springbootinit.service.ChartService;
import com.yupi.springbootinit.service.UserService;
import com.yupi.springbootinit.utils.AiChatUtils;
import com.yupi.springbootinit.utils.ExcelUtils;
import com.yupi.springbootinit.utils.RedisLimitUtils;
import com.yupi.springbootinit.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 帖子接口
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@RestController
@RequestMapping("/chart")
@Slf4j
public class ChartController {

    @Resource
    private ChartService chartService;

    @Resource
    private UserService userService;


    @Resource
    private RedisLimitUtils redisLimitUtils;

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Resource
    private BiMessageProducer biMessageProducer;


    /**
     * 智能分析同步
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    @PostMapping("/gen")
    public BaseResponse<BiResponse> genChatByAi(@RequestPart("file") MultipartFile multipartFile,
                                                GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {

        String goal = genChartByAiRequest.getGoal();
        String name = genChartByAiRequest.getName();
        String chartType = genChartByAiRequest.getChartType();
        // 校验
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "目标为空");
        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() > 100, ErrorCode.PARAMS_ERROR, "名称过长");

        User loginUser = userService.getLoginUser(request);

        // 限流判断
        redisLimitUtils.doRateLimit("genChartByAi" + String.valueOf(loginUser.getId()));


        // 构造用户输入
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求: ").append("\n");
        // userInput.append(goal).append("\n");
        // userInput.append("图表类型: ").append("\n");
        String userGoal = goal;
        if (StringUtils.isNotBlank(chartType)) {
            goal += "并使用" + chartType + "进行数据可视化";
        }
        userInput.append(chartType).append("\n");
        userInput.append("原始数据: ").append("\n");
        // 压缩后的数据
        String csvData = ExcelUtils.excelToCsv(multipartFile);
        userInput.append(csvData).append("\n");

        Chart updateChart = new Chart();
        // 调用Ai
        String aiResponse = AiChatUtils.doChat(userInput.toString());
        String[] split = aiResponse.split("---");
        if (split.length < 3) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图表生成失败，请重试！");
        }
        String genChart = split[1].trim();
        String genResult = split[2].trim();
        updateChart.setGenChart(genChart);
        updateChart.setChartType(chartType);
        updateChart.setGoal(goal);
        updateChart.setName(name);
        updateChart.setChartData(csvData);
        updateChart.setGenResult(genResult);
        updateChart.setUserId(loginUser.getId());
        updateChart.setStatus(ChartEnum.SUCCESSED.getValue());
        // 插入数据库
        boolean save = chartService.save(updateChart);
        ThrowUtils.throwIf(!save, ErrorCode.PARAMS_ERROR, "图表保存失败");
        BiResponse biResponse = new BiResponse();
        biResponse.setChartId(updateChart.getId());
        biResponse.setGenChart(genChart);
        biResponse.setGenResult(genResult);
        return ResultUtils.success(biResponse);

    }

    /**
     * 智能分析异步
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    @PostMapping("/gen/async")
    public BaseResponse<BiResponse> genChatByAiAsync(@RequestPart("file") MultipartFile multipartFile,
                                                     GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {

        String goal = genChartByAiRequest.getGoal();
        String name = genChartByAiRequest.getName();
        String chartType = genChartByAiRequest.getChartType();
        // 校验
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "目标为空");
        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() > 100, ErrorCode.PARAMS_ERROR, "名称过长");

        User loginUser = userService.getLoginUser(request);

        // 限流判断
        redisLimitUtils.doRateLimit("genChartByAi" + String.valueOf(loginUser.getId()));


        // 构造用户输入
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求: ").append("\n");
        // userInput.append(goal).append("\n");
        // userInput.append("图表类型: ").append("\n");
        String userGoal = goal;
        if (StringUtils.isNotBlank(chartType)) {
            goal += "并使用" + chartType + "进行数据可视化";
        }
        userInput.append(chartType).append("\n");
        userInput.append("原始数据: ").append("\n");
        // 压缩后的数据
        String csvData = ExcelUtils.excelToCsv(multipartFile);
        userInput.append(csvData).append("\n");

        // 插入数据库
        Chart chart = new Chart();
        chart.setGoal(goal);
        chart.setName(name);
        chart.setChartData(csvData);
        chart.setChartType(chartType);
        chart.setStatus(ChartEnum.WAIT.getValue());
        chart.setCreateTime(new Date());
        chart.setUpdateTime(new Date());
        chart.setUserId(loginUser.getId());
        boolean save = chartService.save(chart);
        ThrowUtils.throwIf(!save, ErrorCode.PARAMS_ERROR, "图表保存失败");
        // todo 处理任务队列抛异常的情况
        CompletableFuture.runAsync(() -> {
            // 先修改如图表任务状态为‘执行中’ 。 等待执行成功后，修改为‘已完成’，状态修改为‘失败’。记录任务失败信息。
            Chart updateChart = new Chart();
            updateChart.setId(chart.getId());
            updateChart.setStatus(ChartEnum.RUNNING.getValue());
            boolean b = chartService.updateById(updateChart);
            if (!b) {
                handleChartUpdateError(chart.getId(), "更新图表执行状态失败");
                return;
            }
            // 调用Ai
            String aiResponse = AiChatUtils.doChat(userInput.toString());
            String[] split = aiResponse.split("---");
            if (split.length < 3) {
                handleChartUpdateError(chart.getId(), "AI 生成错误");
                return;
            }
            String genChart = split[1].trim();
            String genResult = split[2].trim();
            updateChart.setGenChart(genChart);
            updateChart.setGenResult(genResult);
            updateChart.setStatus(ChartEnum.SUCCESSED.getValue());
            boolean b1 = chartService.updateById(updateChart);
            if (!b1) {
                handleChartUpdateError(chart.getId(), "更新图表成功状态失败");
                return;
            }

        }, threadPoolExecutor);

        BiResponse biResponse = new BiResponse();
        biResponse.setChartId(chart.getId());
        return ResultUtils.success(biResponse);

    }

    /**
     * 智能分析异步消息队列
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    @PostMapping("/gen/async/mq")
    public BaseResponse<BiResponse> genChatByAiAsyncMq(@RequestPart("file") MultipartFile multipartFile,
                                                       GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {

        String goal = genChartByAiRequest.getGoal();
        String name = genChartByAiRequest.getName();
        String chartType = genChartByAiRequest.getChartType();
        // 校验
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "目标为空");
        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() > 100, ErrorCode.PARAMS_ERROR, "名称过长");

        User loginUser = userService.getLoginUser(request);

        // 限流判断
        redisLimitUtils.doRateLimit("genChartByAi" + String.valueOf(loginUser.getId()));


        // 构造用户输入
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求: ").append("\n");
        // userInput.append(goal).append("\n");
        // userInput.append("图表类型: ").append("\n");
        String userGoal = goal;
        if (StringUtils.isNotBlank(chartType)) {
            goal += "并使用" + chartType + "进行数据可视化";
        }
        userInput.append(chartType).append("\n");
        userInput.append("原始数据: ").append("\n");
        // 压缩后的数据
        String csvData = ExcelUtils.excelToCsv(multipartFile);
        userInput.append(csvData).append("\n");

        // 插入数据库
        Chart chart = new Chart();
        chart.setGoal(goal);
        chart.setName(name);
        chart.setChartData(csvData);
        chart.setChartType(chartType);
        chart.setStatus(ChartEnum.WAIT.getValue());
        chart.setCreateTime(new Date());
        chart.setUpdateTime(new Date());
        chart.setUserId(loginUser.getId());
        boolean save = chartService.save(chart);
        ThrowUtils.throwIf(!save, ErrorCode.PARAMS_ERROR, "图表保存失败");
        // todo 处理任务队列抛异常的情况
        biMessageProducer.sendMessage(String.valueOf(chart.getId()));
        BiResponse biResponse = new BiResponse();
        biResponse.setChartId(chart.getId());
        return ResultUtils.success(biResponse);

    }

    private void handleChartUpdateError(long chartId, String execMessag) {
        Chart updateChart = new Chart();
        updateChart.setId(chartId);
        updateChart.setStatus(ChartEnum.FAILED.getValue());
        updateChart.setExecMessage(execMessag);
        boolean updateResult = chartService.updateById(updateChart);
        if (!updateResult) {
            log.error("更新图表失败状态失败" + chartId + " " + execMessag);
        }
    }


    /**
     * 创建
     *
     * @param chartAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addChart(@RequestBody ChartAddRequest chartAddRequest, HttpServletRequest request) {
        if (chartAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartAddRequest, chart);
        // List<String> tags = chartAddRequest.getTags();
        // if (tags != null) {
        //     chart.setTags(JSONUtil.toJsonStr(tags));
        // }
        // chartService.validChart(chart, true);
        User loginUser = userService.getLoginUser(request);
        chart.setUserId(loginUser.getId());
        // chart.setFavourNum(0);
        // chart.setThumbNum(0);
        boolean result = chartService.save(chart);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        long newChartId = chart.getId();
        return ResultUtils.success(newChartId);
    }

    /**
     * 删除
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteChart(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldChart.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = chartService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 更新（仅管理员）
     *
     * @param chartUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateChart(@RequestBody ChartUpdateRequest chartUpdateRequest) {
        if (chartUpdateRequest == null || chartUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartUpdateRequest, chart);
        List<String> tags = chartUpdateRequest.getTags();
        // if (tags != null) {
        //     chart.setTags(JSONUtil.toJsonStr(tags));
        // }
        // // 参数校验
        // chartService.validChart(chart, false);
        long id = chartUpdateRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取
     *
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<Chart> getChartById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = chartService.getById(id);
        if (chart == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(chart);
    }

    /**
     * 分页获取列表（仅管理员）
     *
     * @param chartQueryRequest
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Chart>> listChartByPage(@RequestBody ChartQueryRequest chartQueryRequest) {
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }

    /**
     * 分页获取列表（封装类）
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<Chart>> listChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
                                                     HttpServletRequest request) {
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }

    /**
     * 分页获取当前用户创建的资源列表
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<Chart>> listMyChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
                                                       HttpServletRequest request) {
        if (chartQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        chartQueryRequest.setUserId(loginUser.getId());
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }

    // endregion

    /**
     * 分页搜索（从 ES 查询，封装类）
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    // @PostMapping("/search/page/vo")
    // public BaseResponse<Page<Chart>> searchChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
    //         HttpServletRequest request) {
    //     long size = chartQueryRequest.getPageSize();
    //     // 限制爬虫
    //     ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
    //     Page<Chart> chartPage = chartService.searchFromEs(chartQueryRequest);
    //     return ResultUtils.success(chartPage);
    // }

    /**
     * 编辑（用户）
     *
     * @param chartEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editChart(@RequestBody ChartEditRequest chartEditRequest, HttpServletRequest request) {
        if (chartEditRequest == null || chartEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartEditRequest, chart);
        // List<String> tags = chartEditRequest.getTags();
        // if (tags != null) {
        //     chart.setTags(JSONUtil.toJsonStr(tags));
        // }
        // // 参数校验
        // chartService.validChart(chart, false);
        User loginUser = userService.getLoginUser(request);
        long id = chartEditRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldChart.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }

    /**
     * 获取查询包装类
     *
     * @param
     * @return
     */
    private QueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest) {
        QueryWrapper<Chart> queryWrapper = new QueryWrapper<>();
        if (chartQueryRequest == null) {
            return queryWrapper;
        }

        Long id = chartQueryRequest.getId();
        String goal = chartQueryRequest.getGoal();
        String chartType = chartQueryRequest.getChartType();
        Long userId = chartQueryRequest.getUserId();
        String sortField = chartQueryRequest.getSortField();
        String sortOrder = chartQueryRequest.getSortOrder();
        queryWrapper.eq(id != null && id > 0, "id", id);
        queryWrapper.eq(StringUtils.isNotBlank(goal), "goal", goal);
        queryWrapper.eq(StringUtils.isNotBlank(chartType), "chartType", chartType);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq("isDelete", false);
        // 拼接查询条件
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

}
