package com.itcjy.emp.service.exam;
import com.itcjy.emp.pojo.message.ExamTimeoutMessage;
import java.time.LocalDateTime;
public interface IExamTimeoutService {
    void schedule(Long recordId, LocalDateTime deadlineTime);
    void publishPendingForRecord(Long recordId);
    void handle(ExamTimeoutMessage message);
}
