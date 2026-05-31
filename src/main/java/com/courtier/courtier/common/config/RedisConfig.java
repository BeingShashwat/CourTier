package com.courtier.courtier.common.config;

import com.courtier.courtier.case_.dto.CaseResponse;
import com.courtier.courtier.user.dto.CachedUser;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Bean
    public RedisTemplate<String, CachedUser> cachedUserRedisTemplate(
            RedisConnectionFactory factory
    ) {
        RedisTemplate<String, CachedUser> template = new RedisTemplate<>();

        template.setConnectionFactory(factory);

        template.setKeySerializer(new StringRedisSerializer());

        template.setValueSerializer(
                new org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer<>(
                        CachedUser.class
                )
        );

        template.afterPropertiesSet();

        return template;
    }

    @Bean
    public RedisTemplate<String, CaseResponse> caseRedisTemplate(
            RedisConnectionFactory factory
    ) {

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Jackson2JsonRedisSerializer<CaseResponse> serializer =
                new Jackson2JsonRedisSerializer<>(mapper, CaseResponse.class);

        RedisTemplate<String, CaseResponse> template =
                new RedisTemplate<>();

        template.setConnectionFactory(factory);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);

        template.afterPropertiesSet();

        return template;
    }

    // Raw byte[] connection for Bucket4j — separate from Spring's managed connection
    @Bean
    public StatefulRedisConnection<String, byte[]> lettuceConnection() {
        RedisClient client = RedisClient.create("redis://" + redisHost + ":" + redisPort);
        return client.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));
    }
}