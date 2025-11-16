//package com.yerin.jobq.support;
//
//import org.junit.jupiter.api.Test;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.DynamicPropertyRegistry;
//import org.springframework.test.context.DynamicPropertySource;
//import org.testcontainers.containers.GenericContainer;
//import org.testcontainers.junit.jupiter.Container;
//import org.testcontainers.junit.jupiter.Testcontainers;
//import static org.assertj.core.api.Assertions.*;
//
//@Testcontainers
//@SpringBootTest
//class RedisIT {
//
//    @Container
//    static GenericContainer<?> redis = new GenericContainer<>("redis:7.4.7")
//            .withExposedPorts(6379);
//
//    @DynamicPropertySource
//    static void props(DynamicPropertyRegistry r) {
//        r.add("spring.data.redis.host", () -> redis.getHost());
//        r.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
//    }
//
//    @Test
//    void up() {
//        assertThat(redis.isRunning()).isTrue();
//    }
//}
