package com.itcjy.emp.pojo.req.exam;

import com.itcjy.common.pojo.BasePageReq;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ExamPageReq extends BasePageReq {
    private Long paperId;
    private Long classId;
    private LocalDateTime startFrom;
    private LocalDateTime startTo;
}
