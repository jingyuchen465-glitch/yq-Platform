package com.itcjy.emp.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 使用 StringRedisSerializer 序列化 key
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringRedisSerializer);//大key的序列化方式
        template.setHashKeySerializer(stringRedisSerializer);//设置hash类型的小key的序列化方式

        // 创建并配置 ObjectMapper
        ObjectMapper objectMapper = new ObjectMapper();//用于对象和json字符串之间相互转化
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);

        // 启用默认类型，解决反序列化时类型丢失问题
        objectMapper.activateDefaultTyping(//需要保存对象的类型（全限定名）
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL
        );
        // 支持 Java 8 时间类型
        objectMapper.registerModule(new JavaTimeModule());

        // 关键改动：通过构造函数传入 ObjectMapper，而非调用 setObjectMapper
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);

        template.setValueSerializer(jackson2JsonRedisSerializer);//设置value的序列化方式是json的序列化方式
        template.setHashValueSerializer(jackson2JsonRedisSerializer);//设置hash的value序列化方式是json的序列化方式

        template.afterPropertiesSet();
        return template;
    }
}