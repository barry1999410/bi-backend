package com.yupi.springbootinit.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 图表信息表
 * @TableName chart
 */
@TableName(value ="chart")
@Data
public class Chart implements Serializable {
    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 分析目标
     */
    private String goal;

    /**
     * 图标名称
     */
    private String name;

    /**
     * 图表数据
     */
    private String chartData;

    /**
     * 图表类型
     */
    private String chartType;

    /**
     * 任务状态
     */
    private String status;

    /**
     * 执行信息
     */
    private String execMessage;
    /**
     * 生成的图表信息
     */
    private String genChart;

    /**
     * 生成的图表信息
     */
    private String genResult;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 0 无删除 1删除
     */
    @TableLogic
    private Integer isDelete;

    /**
     * 创建用户id
     */
    private Long userId;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
