package com.itcjy.emp.service.impl.stu;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itcjy.common.constants.TokenConstants;
import com.itcjy.emp.service.stu.IStudentService;
import com.itcjy.stu.mapper.LoginMapper;
import com.itcjy.stu.pojo.entity.Student;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class StudentServiceImpl extends ServiceImpl<LoginMapper, Student> implements IStudentService {

    private static final String PASSWORD_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private static final int INITIAL_PASSWORD_LENGTH = 10;
    private static final int BCRYPT_LOG_ROUNDS = 12;
    private static final String TEMPORARY_STATUS = "TEMPORARY";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StudentCreationResult createIfAbsent(String name, String phone, String email, Long createdUserId) {
        String normalizedPhone = StrUtil.trim(phone);
        boolean exists = this.lambdaQuery()
                .eq(Student::getPhone, normalizedPhone)
                .exists();
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
        student.setStatus(TEMPORARY_STATUS);
        student.setCreatedUserId(createdUserId);
        LocalDateTime now = LocalDateTime.now();
        student.setCreatedAt(now);
        student.setUpdatedAt(now);
        this.save(student);
        return StudentCreationResult.created(initialPassword);
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
}
