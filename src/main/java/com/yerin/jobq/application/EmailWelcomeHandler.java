package com.yerin.jobq.application;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EmailWelcomeHandler implements JobHandler {
    @Override public String type() { return "email_welcome"; }
    @Override public void handle(String jobId, String payloadJson) {
        log.info("[Handler.email_welcome] jobId={}, payload={}", jobId, payloadJson);
        // TODO: 실제 메일 발송 로직은 추후(지금은 데모 로그)
    }
}
