package com.yerin.jobq.web;


import com.yerin.jobq.support.IntegrationTestBase;
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

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("관리자 메트릭 보안(헤더) - 컨트롤러 경로 보호 스모크")
class AdminMetricsControllerSecurityTest extends IntegrationTestBase {

    @Autowired
    TestRestTemplate rest;

    @Test
    @DisplayName("헤더 없음 → 401/403으로 차단")
    void block_when_header_missing() {
        ResponseEntity<String> r1 = rest.getForEntity("/admin/metrics/queue", String.class);
        ResponseEntity<String> r2 = rest.getForEntity("/admin/metrics/jobs", String.class);
        assertThat(r1.getStatusCode().value()).isIn(401, 403);
        assertThat(r2.getStatusCode().value()).isIn(401, 403);
    }

    @Test
    @DisplayName("잘못된 토큰 → 401/403으로 차단")
    void blocks_when_header_invalid() {
        HttpHeaders h = new HttpHeaders();
        h.set("X-Admin-Token", "wrong-token");
        ResponseEntity<String> r1 = rest.exchange("/admin/metrics/queue", HttpMethod.GET, new HttpEntity<>(h), String.class);
        ResponseEntity<String> r2 = rest.exchange("/admin/metrics/jobs",  HttpMethod.GET, new HttpEntity<>(h), String.class);
        assertThat(r1.getStatusCode().value()).isIn(401, 403);
        assertThat(r2.getStatusCode().value()).isIn(401, 403);
    }

    @Test
    @DisplayName("정상 토큰 → 200과 JSON 본문")
    void passes_when_header_valid() {
        HttpHeaders h = new HttpHeaders();
        h.set("X-Admin-Token", "test-admin-token"); // application-test.yml
        ResponseEntity<String> q = rest.exchange("/admin/metrics/queue", HttpMethod.GET, new HttpEntity<>(h), String.class);
        ResponseEntity<String> j = rest.exchange("/admin/metrics/jobs",  HttpMethod.GET, new HttpEntity<>(h), String.class);

        assertThat(q.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(j.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(q.getBody()).contains("streams");
        assertThat(j.getBody()).contains("QUEUED").contains("SUCCEEDED").contains("DLQ");
    }
}