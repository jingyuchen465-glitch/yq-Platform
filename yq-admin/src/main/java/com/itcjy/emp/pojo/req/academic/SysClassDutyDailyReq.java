package com.itcjy.emp.pojo.req.academic;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
@Schema(description = "每日值班查询参数")
public class SysClassDutyDailyReq {

    @NotNull(message = "校区不能为空")
    @Positive(message = "校区ID必须大于0")
    private Long campusId;

    @NotNull(message = "值班日期不能为空")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dutyDate;
}
