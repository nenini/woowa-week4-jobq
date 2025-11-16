package com.yerin.jobq.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yerin.jobq.application.JobHandler;
import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
@DisplayName("테스트용 핸들러 구성(Stub/테스트 빈)")
public class TestHandlersConfig {
    @Bean
    public JobHandler emailWelcomeTestHandler() {
        ObjectMapper om = new ObjectMapper();
        return new JobHandler() {
            @Override public String type() { return "email_welcome"; }
            @Override public void handle(String jobId, String payloadJson) {
                try {
                    var node = om.readTree(payloadJson);
                    int userId = node.path("userId").asInt();
                    if (userId == 777) throw new RuntimeException("simulated failure");
                    // 성공은 아무 일 없이 통과
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new RuntimeException("parse failure", e);
                }
            }
        };
    }
}

