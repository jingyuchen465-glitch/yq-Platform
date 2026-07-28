package com.itcjy.emp.pojo.message;
import java.time.LocalDateTime;
public record ExamTimeoutMessage(Long recordId, LocalDateTime deadlineTime) {}
