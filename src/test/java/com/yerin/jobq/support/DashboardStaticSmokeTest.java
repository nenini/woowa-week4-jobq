package com.yerin.jobq.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "spring.task.scheduling.enabled=false"
})
@DisplayName("정적 리소스: 대시보드 HTML 스모크")
class DashboardStaticSmokeTest extends IntegrationTestBase {

    @Autowired
    TestRestTemplate rest;

    @Test
    @DisplayName("dashboard.html 200 OK & 핵심 텍스트 포함")
    void servesDashboardHtml() {
        var r = rest.getForEntity("/dashboard.html", String.class);
        assertThat(r.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(r.getBody()).contains("JobQ 대시보드")
                .contains("Redis Streams")
                .contains("Job 상태");
    }
}
