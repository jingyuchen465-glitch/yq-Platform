package com.itcjy.emp.mapper.exam;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itcjy.emp.pojo.entity.exam.StudentExamRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
@Mapper public interface StudentExamRecordMapper extends BaseMapper<StudentExamRecord> {
    @Select("SELECT * FROM student_exam_record WHERE id = #{id} FOR UPDATE")
    StudentExamRecord selectByIdForUpdate(@Param("id") Long id);
}
