package com.itcjy.emp.listener;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.itcjy.emp.pojo.entity.SysCourseDetail;
import com.itcjy.emp.pojo.excel.CourseDetailExcelData;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * 课程详情 Excel 导入流式监听器
 * <p>
 * 每积累 BATCH_SIZE 条记录执行一次批量入库，内存占用恒定，防止大文件 OOM。
 */
public class CourseDetailImportListener implements ReadListener<CourseDetailExcelData> {

    private static final int BATCH_SIZE = 50;

    private final Long courseId;
    private final Consumer<List<SysCourseDetail>> batchSaver;
    private final List<SysCourseDetail> batch = new ArrayList<>(BATCH_SIZE);
    private final AtomicInteger totalCount = new AtomicInteger(0);

    /**
     * @param courseId   所属课程ID
     * @param batchSaver 批量保存回调（通常传入 service::saveBatch）
     */
    public CourseDetailImportListener(Long courseId, Consumer<List<SysCourseDetail>> batchSaver) {
        this.courseId = courseId;
        this.batchSaver = batchSaver;
    }

    @Override
    public void invoke(CourseDetailExcelData data, AnalysisContext context) {
        SysCourseDetail detail = new SysCourseDetail();
        detail.setCourseId(courseId);
        detail.setStageName(data.getStageName());
        detail.setDayNumber(data.getDayNumber());
        detail.setClassContent(data.getClassContent());
        batch.add(detail);
        if (batch.size() >= BATCH_SIZE) {
            flush();
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        flush();
    }

    private void flush() {
        if (!batch.isEmpty()) {
            batchSaver.accept(new ArrayList<>(batch));
            totalCount.addAndGet(batch.size());
            batch.clear();
        }
    }

    /**
     * 获取本次导入的总记录数
     */
    public int getTotalCount() {
        return totalCount.get();
    }
}
