package com.itcjy.emp.pojo.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 课程详情 Excel 导入数据模型
 * Excel 列顺序：阶段 | 第几天 | 上课内容
 */
@Data
public class CourseDetailExcelData {

    @ExcelProperty(index = 0)
    private String stageName;

    @ExcelProperty(index = 1)
    private Integer dayNumber;

    @ExcelProperty(index = 2)
    private String classContent;
}
