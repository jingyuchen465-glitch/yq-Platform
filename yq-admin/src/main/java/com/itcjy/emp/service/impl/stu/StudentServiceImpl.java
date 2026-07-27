package com.itcjy.emp.service.impl.stu;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itcjy.common.constants.TokenConstants;
import com.itcjy.common.exception.BusinessException;
import com.itcjy.common.pojo.PageResult;
import com.itcjy.emp.pojo.entity.SysClass;
import com.itcjy.emp.pojo.req.stu.StudentPageReq;
import com.itcjy.emp.pojo.req.stu.StudentUpdateReq;
import com.itcjy.emp.pojo.res.stu.StudentClassOptionRes;
import com.itcjy.emp.pojo.res.stu.StudentRes;
import com.itcjy.emp.service.academic.ISysClassService;
import com.itcjy.emp.service.stu.IStudentService;
import com.itcjy.stu.mapper.LoginMapper;
import com.itcjy.stu.pojo.entity.Student;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentServiceImpl extends ServiceImpl<LoginMapper, Student> implements IStudentService {

    private static final String PASSWORD_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private static final int INITIAL_PASSWORD_LENGTH = 10;
    private static final int BCRYPT_LOG_ROUNDS = 12;
    private static final Set<String> VALID_STATUSES = Set.of(
            Student.TEMPORARY,
            Student.AT_SCHOOL,
            Student.GRADUATE,
            Student.WITCHDRAWAL
    );

    private final ISysClassService sysClassService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StudentCreationResult createIfAbsent(String name, String phone, String email, Long createdUserId) {
        String normalizedPhone = StrUtil.trim(phone);
        boolean exists = this.count(Wrappers.<Student>lambdaQuery()
                .eq(Student::getPhone, normalizedPhone)) > 0;
        if (exists) {
            return StudentCreationResult.existing();
        }

        String initialPassword = generateInitialPassword();
        Student student = new Student();
        student.setName(StrUtil.trim(name));
        student.setPhone(normalizedPhone);
        student.setPassword(BCrypt.hashpw(
                initialPassword,
                BCrypt.gensalt(BCRYPT_LOG_ROUNDS, TokenConstants.SECURE_RANDOM)
        ));
        student.setEmail(normalizeNullable(email));
        student.setStatus(Student.TEMPORARY);
        student.setCreatedUserId(createdUserId);
        LocalDateTime now = LocalDateTime.now();
        student.setCreatedAt(now);
        student.setUpdatedAt(now);
        this.save(student);
        return StudentCreationResult.created(initialPassword);
    }

    @Override
    public PageResult<StudentRes> pageStudents(StudentPageReq req) {
        String keyword = StrUtil.trim(req.getKeyword());
        String status = StrUtil.trim(req.getStatus());
        IPage<Student> page = this.page(
                new Page<>(req.getCurrent(), req.getSize()),
                Wrappers.<Student>lambdaQuery()
                        .and(StrUtil.isNotBlank(keyword), query -> query
                                .like(Student::getName, keyword)
                                .or()
                                .like(Student::getPhone, keyword)
                                .or()
                                .like(Student::getEmail, keyword))
                        .eq(req.getClassId() != null, Student::getClassId, req.getClassId())
                        .eq(StrUtil.isNotBlank(status), Student::getStatus, status)
                        .orderByDesc(Student::getId)
        );
        return new PageResult<>(page.getTotal(), toResponses(page.getRecords()));
    }

    @Override
    public StudentRes getStudentDetail(Long id) {
        Student student = requireStudent(id);
        String className = null;
        if (student.getClassId() != null) {
            SysClass sysClass = sysClassService.getById(student.getClassId());
            className = sysClass == null ? null : sysClass.getClassPeriod();
        }
        return StudentRes.from(student, className);
    }

    @Override
    public List<StudentClassOptionRes> listClassOptions() {
        return sysClassService.list(Wrappers.<SysClass>lambdaQuery()
                        .orderByDesc(SysClass::getId))
                .stream()
                .map(StudentClassOptionRes::from)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStudent(Long id, StudentUpdateReq req) {
        Student student = requireStudent(id);
        String phone = StrUtil.trim(req.phone());
        boolean phoneExists = this.count(Wrappers.<Student>lambdaQuery()
                .eq(Student::getPhone, phone)
                .ne(Student::getId, id)) > 0;
        if (phoneExists) {
            throw BusinessException.STUDENT_EXIST.newInstance("该手机号已绑定其他学员");
        }
        validateClass(req.classId());
        validateStatus(req.status());

        student.setName(StrUtil.trim(req.name()));
        student.setPhone(phone);
        student.setEmail(normalizeNullable(req.email()));
        student.setClassId(req.classId());
        student.setStatus(req.status());
        student.setUpdatedAt(LocalDateTime.now());
        if (!this.updateById(student)) {
            throw BusinessException.DATA_ERROR.newInstance("学员资料更新失败，请刷新后重试");
        }
        evictStudentSession(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdrawStudent(Long id) {
        Student student = requireStudent(id);
        if (!Student.WITCHDRAWAL.equals(student.getStatus())) {
            student.setStatus(Student.WITCHDRAWAL);
            student.setUpdatedAt(LocalDateTime.now());
            if (!this.updateById(student)) {
                throw BusinessException.DATA_ERROR.newInstance("学员退学状态更新失败，请刷新后重试");
            }
        }
        evictStudentSession(id);
    }

    private String generateInitialPassword() {
        StringBuilder password = new StringBuilder(INITIAL_PASSWORD_LENGTH);
        for (int index = 0; index < INITIAL_PASSWORD_LENGTH; index++) {
            int alphabetIndex = TokenConstants.SECURE_RANDOM.nextInt(PASSWORD_ALPHABET.length());
            password.append(PASSWORD_ALPHABET.charAt(alphabetIndex));
        }
        return password.toString();
    }

    private String normalizeNullable(String value) {
        String normalized = StrUtil.trim(value);
        return StrUtil.isBlank(normalized) ? null : normalized;
    }

    private Student requireStudent(Long id) {
        Student student = this.getById(id);
        if (student == null) {
            throw BusinessException.STUDENT_NOT_EXIST.newInstance("学员不存在");
        }
        return student;
    }

    private void validateClass(Long classId) {
        if (classId != null && sysClassService.getById(classId) == null) {
            throw BusinessException.CLAZZ_NOT_EXIST.newInstance("所选班级不存在");
        }
    }

    private void validateStatus(String status) {
        if (!VALID_STATUSES.contains(status)) {
            throw BusinessException.PARAMS_ERROR.newInstance("学员状态不合法");
        }
    }

    private List<StudentRes> toResponses(List<Student> students) {
        if (students.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> classIds = students.stream()
                .map(Student::getClassId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        Map<Long, SysClass> classMap = classIds.isEmpty()
                ? Collections.emptyMap()
                : sysClassService.listByIds(classIds).stream()
                        .collect(Collectors.toMap(SysClass::getId, Function.identity()));
        return students.stream()
                .map(student -> {
                    SysClass sysClass = classMap.get(student.getClassId());
                    return StudentRes.from(student, sysClass == null ? null : sysClass.getClassPeriod());
                })
                .toList();
    }

    private void evictStudentSession(Long studentId) {
        redisTemplate.delete(TokenConstants.STUDENT_JWT_KEY_PREFIX + studentId);
    }
}
