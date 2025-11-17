package com.yerin.jobq.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "spring.task.scheduling.enabled=false"
})
@DisplayName("E2E: Admin 메트릭 API 스모크")
class AdminMetricsE2ESmokeIT extends IntegrationTestBase {

    @Autowired
    TestRestTemplate rest;

    private HttpEntity<Void> admin() {
        HttpHeaders h = new HttpHeaders();
        h.set("X-Admin-Token", "test-admin-token");
        return new HttpEntity<>(h);
    }

    @Test
    @DisplayName("큐/잡 메트릭 200 응답 & 기본 구조 확인")
    void metrics_endpoints_respond_and_lookSane() {
        ResponseEntity<String> q = rest.exchange("/admin/metrics/queue", HttpMethod.GET, admin(), String.class);
        ResponseEntity<String> j = rest.exchange("/admin/metrics/jobs",  HttpMethod.GET, admin(), String.class);

        assertThat(q.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(j.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(q.getBody()).contains("streams").contains("email_welcome").contains("xlen");
        assertThat(j.getBody()).contains("QUEUED").contains("RUNNING").contains("SUCCEEDED").contains("DLQ");
    }
}
