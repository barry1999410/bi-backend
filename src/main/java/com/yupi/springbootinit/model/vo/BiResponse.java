package com.yupi.springbootinit.model.vo;

import lombok.Data;

/**
 * BI的返回结果
 */
@Data

public class BiResponse {
    private Long  chartId;
    private String genChart;

    private String genResult;
}
