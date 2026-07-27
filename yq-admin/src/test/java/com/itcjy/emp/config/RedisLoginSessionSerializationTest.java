package com.itcjy.emp.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.itcjy.emp.pojo.res.system.UserDetailRes;
import com.itcjy.stu.pojo.VO.StudentDetailsVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RedisLoginSessionSerializationTest {

    private final ObjectMapper jsonMapper = new ObjectMapper();
    private RedisSerializer<Object> serializer;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);
        serializer = (RedisSerializer<Object>) new RedisConfig()
                .redisTemplate(connectionFactory)
                .getValueSerializer();
    }

    @Test
    @DisplayName("员工登录会话应保留权限并忽略派生身份字段")
    void shouldRoundTripEmployeeLoginSessionAndReadLegacyDerivedFields() throws Exception {
        UserDetailRes employee = new UserDetailRes();
        employee.setId(23L);

        com.itcjy.emp.pojo.res.system.LoginInfo loginInfo =
                new com.itcjy.emp.pojo.res.system.LoginInfo();
        loginInfo.setToken("employee-token");
        loginInfo.setSignSecret("employee-sign-secret");
        loginInfo.setUserDetailRes(employee);
        loginInfo.setRoles(List.of("admin"));
        loginInfo.setPermissions(List.of("course:list"));

        byte[] serialized = serializer.serialize(loginInfo);

        assertThat(serialized).isNotNull();
        String json = new String(serialized, StandardCharsets.UTF_8);
        assertThat(json)
                .contains("\"permissions\"")
                .doesNotContain("\"principalId\"")
                .doesNotContain("\"principalType\"");

        byte[] legacyJson = addLegacyProperties(serialized, payload -> {
            payload.put("principalId", 23L);
            payload.put("principalType", "employee");
        });
        Object restored = serializer.deserialize(legacyJson);

        assertThat(restored)
                .isInstanceOf(com.itcjy.emp.pojo.res.system.LoginInfo.class);
        com.itcjy.emp.pojo.res.system.LoginInfo restoredLogin =
                (com.itcjy.emp.pojo.res.system.LoginInfo) restored;
        assertThat(restoredLogin.getPrincipalId()).isEqualTo(23L);
        assertThat(restoredLogin.getPermissions()).containsExactly("course:list");
    }

    @Test
    @DisplayName("学生登录会话应忽略派生身份字段和默认权限字段")
    void shouldRoundTripStudentLoginSessionAndReadLegacyDerivedFields() throws Exception {
        StudentDetailsVO student = new StudentDetailsVO();
        student.setId(7L);

        com.itcjy.stu.pojo.VO.LoginInfo loginInfo = new com.itcjy.stu.pojo.VO.LoginInfo();
        loginInfo.setToken("student-token");
        loginInfo.setSignSecret("student-sign-secret");
        loginInfo.setStudentDetailsVO(student);

        byte[] serialized = serializer.serialize(loginInfo);

        assertThat(serialized).isNotNull();
        String json = new String(serialized, StandardCharsets.UTF_8);
        assertThat(json)
                .doesNotContain("\"principalId\"")
                .doesNotContain("\"principalType\"")
                .doesNotContain("\"permissions\"");

        byte[] legacyJson = addLegacyProperties(serialized, payload -> {
            payload.put("principalId", 7L);
            payload.put("principalType", "student");
            payload.putArray("permissions").add("legacy-value");
        });
        Object restored = serializer.deserialize(legacyJson);

        assertThat(restored).isInstanceOf(com.itcjy.stu.pojo.VO.LoginInfo.class);
        com.itcjy.stu.pojo.VO.LoginInfo restoredLogin =
                (com.itcjy.stu.pojo.VO.LoginInfo) restored;
        assertThat(restoredLogin.getPrincipalId()).isEqualTo(7L);
        assertThat(restoredLogin.getPermissions()).isEmpty();
    }

    private byte[] addLegacyProperties(byte[] serialized, Consumer<ObjectNode> addProperties)
            throws Exception {
        JsonNode root = jsonMapper.readTree(serialized);
        JsonNode payload = root.isArray() ? root.get(1) : root;
        assertThat(payload).isInstanceOf(ObjectNode.class);
        addProperties.accept((ObjectNode) payload);
        return jsonMapper.writeValueAsBytes(root);
    }
}
