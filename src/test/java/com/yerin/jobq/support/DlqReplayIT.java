package com.yerin.jobq.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestHandlersConfig.class)
@DisplayName("E2E: 실패 → DLQ → Replay")
@ActiveProfiles("test")
class DlqReplayIT extends IntegrationTestBase {

    @Autowired
    TestRestTemplate rest;

    private static String toJobId(Object v) {
        if (v == null) throw new IllegalStateException("jobId is null");
        if (v instanceof Number n) return String.valueOf(n.longValue());
        return String.valueOf(v); // String 등
    }

    @Test
    @DisplayName("userId=777은 실패 → DLQ, /admin/jobs/{id}/replay로 재큐잉")
    void dlq_then_replay() {
        var req = Map.of("userId", 777);
        ResponseEntity<Map> resp = rest.postForEntity("/jobs/email_welcome", req, Map.class);
        String jobId = toJobId(resp.getBody().get("jobId"));

        Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofMillis(300))
                .untilAsserted(() -> {
                    var r = rest.getForEntity("/jobs/{id}", Map.class, jobId);
                    assertThat(r.getStatusCode().is2xxSuccessful()).isTrue();
                    assertThat(r.getBody().get("status")).isEqualTo("DLQ");
                });

        HttpHeaders h = new HttpHeaders();
        h.set("X-Admin-Token", "test-admin-token");
        var replay = rest.exchange("/admin/jobs/{id}/replay", HttpMethod.POST, new HttpEntity<>(h), Map.class, jobId);
        assertThat(replay.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(replay.getStatusCode().is2xxSuccessful()).isTrue();

        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    var r2 = rest.getForEntity("/jobs/{id}", Map.class, jobId);
                    assertThat(r2.getBody().get("status")).isIn("QUEUED", "RUNNING", "RETRY", "DLQ");
                });

    }
}
