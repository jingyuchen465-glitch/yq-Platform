package com.itcjy.stu.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itcjy.common.constants.TokenConstants;
import com.itcjy.common.exception.BusinessException;
import com.itcjy.common.interceptor.AuthThreadlocal;
import com.itcjy.common.interceptor.LoginSession;
import com.itcjy.common.properties.JwtProperties;
import com.itcjy.common.utils.JwtUtil;
import com.itcjy.stu.mapper.LoginMapper;
import com.itcjy.stu.pojo.DTO.LoginDTO;
import com.itcjy.stu.pojo.VO.LoginInfo;
import com.itcjy.stu.pojo.VO.LoginVO;
import com.itcjy.stu.pojo.VO.StudentDetailsVO;
import com.itcjy.stu.pojo.entity.Student;
import com.itcjy.stu.service.LoginService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class LoginServiceImpl implements LoginService {

    private final LoginMapper loginMapper;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 学生登录
     * @param loginDTO
     * @return
     */
    @Override
    public LoginVO login(LoginDTO loginDTO) {
        String phone = StrUtil.trim(loginDTO.getPhone());
        String password = loginDTO.getPassword();

        // 1. 根据手机号查询学生
        Student student = loginMapper.selectOne(new LambdaQueryWrapper<Student>()
                .eq(Student::getPhone, phone)
                .last("LIMIT 1"));
        if (student == null) {
            throw BusinessException.USER_NOT_EXIST.newInstance("学生账号不存在");
        }

        // 2. 校验密码（BCrypt）
        if (!BCrypt.checkpw(password, student.getPassword())) {
            throw BusinessException.PASSWORD_ERROR.newInstance("密码错误");
        }
        //校验状态
        if (Student.WITCHDRAWAL.equals(student.getStatus())) {
            throw BusinessException.ACCOUNT_NOT_ACTIVE.newInstance("学生账号已注销");
        }

        // 3. 生成 JWT token（学生无角色，传空集合）
        String token = jwtUtil.generateToken(
                student.getId(),
                student.getPhone(),
                Collections.emptyList(),
                TokenConstants.PRINCIPAL_STUDENT
        );

        // 4. 生成签名密钥
        String signSecret = createSignSecret();

        // 5. 构建学生详情 VO
        StudentDetailsVO detailsVO = StudentDetailsVO.from(student);

        // 6. 将登录信息保存到 Redis（TTL 与 JWT 有效期保持一致）
        LoginInfo loginInfo = new LoginInfo();
        loginInfo.setToken(token);
        loginInfo.setSignSecret(signSecret);
        loginInfo.setStudentDetailsVO(detailsVO);

        redisTemplate.opsForValue().set(
                TokenConstants.STUDENT_JWT_KEY_PREFIX + student.getId(),
                loginInfo,
                jwtProperties.getExpireMillis(),
                TimeUnit.MILLISECONDS
        );

        // 7. 返回登录信息
        return LoginVO.of(token, signSecret, detailsVO);
    }

    @Override
    public void logout() {
        LoginSession session = requireStudentSession();
        redisTemplate.delete(TokenConstants.STUDENT_JWT_KEY_PREFIX + session.getPrincipalId());
    }

    @Override
    public StudentDetailsVO getCurrentStudent() {
        LoginSession session = requireStudentSession();
        Object value = redisTemplate.opsForValue().get(
                TokenConstants.STUDENT_JWT_KEY_PREFIX + session.getPrincipalId()
        );
        if (!(value instanceof LoginInfo loginInfo) || loginInfo.getStudentDetailsVO() == null) {
            throw BusinessException.USER_NO_TOKEN;
        }
        return loginInfo.getStudentDetailsVO();
    }

    private LoginSession requireStudentSession() {
        LoginSession session = AuthThreadlocal.getLoginInfo();
        if (session == null
                || session.getPrincipalId() == null
                || !TokenConstants.PRINCIPAL_STUDENT.equals(session.getPrincipalType())) {
            throw BusinessException.USER_NO_TOKEN;
        }
        return session;
    }

    /**
     * 创建签名密钥（32 字节 Base64 URL 安全编码）
     */
    private String createSignSecret() {
        byte[] bytes = new byte[32];
        TokenConstants.SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
