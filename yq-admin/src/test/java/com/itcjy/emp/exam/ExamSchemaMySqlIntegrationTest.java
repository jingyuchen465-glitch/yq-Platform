package com.itcjy.emp.exam;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers(disabledWithoutDocker = true)
class ExamSchemaMySqlIntegrationTest {
    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.36")
            .withDatabaseName("yq_exam_test")
            .withUsername("test")
            .withPassword("test");

    @Test
    @DisplayName("考试模块建表脚本可执行且关键唯一约束生效")
    void shouldCreateSchemaAndEnforceSnapshotConstraints() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/exam_module.sql"));
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("INSERT INTO exam_question_course " +
                        "(question_id,course_id,stage_name,created_at) VALUES (1,2,'第一阶段',NOW())");
                assertThatThrownBy(() -> statement.executeUpdate("INSERT INTO exam_question_course " +
                        "(question_id,course_id,stage_name,created_at) VALUES (1,2,'第一阶段',NOW())"))
                        .isInstanceOf(SQLException.class);

                statement.executeUpdate("INSERT INTO student_exam_answer " +
                        "(record_id,exam_id,paper_id,paper_question_id,question_id,question_type,question_score,created_at,updated_at) " +
                        "VALUES (1,1,1,9,3,'SINGLE',5,NOW(),NOW())");
                assertThatThrownBy(() -> statement.executeUpdate("INSERT INTO student_exam_answer " +
                        "(record_id,exam_id,paper_id,paper_question_id,question_id,question_type,question_score,created_at,updated_at) " +
                        "VALUES (1,1,1,9,3,'SINGLE',5,NOW(),NOW())"))
                        .isInstanceOf(SQLException.class);
            }
        }
    }
}
