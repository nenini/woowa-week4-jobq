package com.yerin.jobq.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Profile("!test")
@Component
public class EmailWelcomeHandler implements JobHandler {
    @Value("${jobq.handler.emailWelcome.failAlways:false}")
    private boolean failAlways;

    private final ObjectMapper om = new ObjectMapper();

    @Override
    public String type() {
        return "email_welcome";
    }

    @Override
    public void handle(String jobId, String payloadJson) {
        if (failAlways) throw new RuntimeException("fail for DLQ test");
        try {
            var node = om.readTree(payloadJson);
            int userId = node.path("userId").asInt();
            if (userId == 777) throw new RuntimeException("simulated failure by userId=777");

            log.info("[Handler.email_welcome] jobId={}, payload={}", jobId, payloadJson);
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            throw new RuntimeException("payload parse error", e);
        }
    }
}
