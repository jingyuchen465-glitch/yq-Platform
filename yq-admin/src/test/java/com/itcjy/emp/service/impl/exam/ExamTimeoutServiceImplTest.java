package com.itcjy.emp.service.impl.exam;

import com.itcjy.common.properties.ExamProperties;
import com.itcjy.emp.mapper.exam.ExamMapper;
import com.itcjy.emp.mapper.exam.ExamTimeoutOutboxMapper;
import com.itcjy.emp.mapper.exam.StudentExamRecordMapper;
import com.itcjy.emp.pojo.entity.exam.StudentExamRecord;
import com.itcjy.emp.pojo.enums.exam.ExamRecordStatus;
import com.itcjy.emp.pojo.enums.exam.ExamSubmitReason;
import com.itcjy.emp.service.exam.IExamSubmissionService;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExamTimeoutServiceImplTest {

    @Test
    @DisplayName("RocketMQ 4.x 延时档位按剩余时长分段")
    void shouldResolveRocketMqDelayLevels() {
        assertThat(ExamTimeoutServiceImpl.resolveDelayLevel(Duration.ofHours(3))).isEqualTo(18);
        assertThat(ExamTimeoutServiceImpl.resolveDelayLevel(Duration.ofMinutes(61))).isEqualTo(17);
        assertThat(ExamTimeoutServiceImpl.resolveDelayLevel(Duration.ofMinutes(31))).isEqualTo(16);
        assertThat(ExamTimeoutServiceImpl.resolveDelayLevel(Duration.ofSeconds(31))).isEqualTo(4);
        assertThat(ExamTimeoutServiceImpl.resolveDelayLevel(Duration.ofSeconds(2))).isEqualTo(1);
        assertThat(ExamTimeoutServiceImpl.resolveDelayLevel(Duration.ofSeconds(-2))).isEqualTo(1);
    }

    @Test
    @DisplayName("到期消息只封卷仍在答题的记录")
    void shouldSubmitOnlyExpiredInProgressRecord() {
        StudentExamRecordMapper recordMapper = mock(StudentExamRecordMapper.class);
        IExamSubmissionService submissionService = mock(IExamSubmissionService.class);
        ExamTimeoutServiceImpl service = service(recordMapper, submissionService);
        StudentExamRecord record = new StudentExamRecord();
        record.setId(9L);
        record.setStatus(ExamRecordStatus.IN_PROGRESS.name());
        record.setDeadlineTime(LocalDateTime.now().minusSeconds(1));
        when(recordMapper.selectById(9L)).thenReturn(record);

        service.handle(new com.itcjy.emp.pojo.message.ExamTimeoutMessage(9L, record.getDeadlineTime()));

        verify(submissionService).submit(9L, ExamSubmitReason.TIMEOUT);
    }

    @Test
    @DisplayName("重复到达的超时消息不会再次封卷")
    void shouldIgnoreMessageForFinalRecord() {
        StudentExamRecordMapper recordMapper = mock(StudentExamRecordMapper.class);
        IExamSubmissionService submissionService = mock(IExamSubmissionService.class);
        ExamTimeoutServiceImpl service = service(recordMapper, submissionService);
        StudentExamRecord record = new StudentExamRecord();
        record.setId(9L);
        record.setStatus(ExamRecordStatus.SUBMITTED.name());
        when(recordMapper.selectById(9L)).thenReturn(record);

        service.handle(new com.itcjy.emp.pojo.message.ExamTimeoutMessage(9L, LocalDateTime.now()));

        verify(submissionService, never()).submit(9L, ExamSubmitReason.TIMEOUT);
    }

    private ExamTimeoutServiceImpl service(StudentExamRecordMapper recordMapper,
                                           IExamSubmissionService submissionService) {
        return new ExamTimeoutServiceImpl(
                mock(ExamTimeoutOutboxMapper.class), recordMapper, mock(ExamMapper.class),
                mock(RocketMQTemplate.class), new ExamProperties(), submissionService);
    }
}
