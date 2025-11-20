package com.yerin.jobq.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.awaitility.Awaitility;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestHandlersConfig.class)
@ActiveProfiles("test")
@DisplayName("E2E: enqueue → worker → SUCCEEDED")
class JobFlowIT extends IntegrationTestBase {

    @Autowired
    TestRestTemplate rest;

    @Test
    @DisplayName("성공 플로우: email_welcome 42 → SUCCEEDED")
    void success_flow() {
        var req = Map.of("userId", 42, "idempotencyKey", "it-" + System.currentTimeMillis());
        ResponseEntity<Map> created = rest.postForEntity("/jobs/email_welcome", req, Map.class);

        Map<?, ?> body = created.getBody();
        assertThat(body).isNotNull();

        Map<?, ?> data = (Map<?, ?>) body.get("data");
        String jobId = String.valueOf(data.get("jobId"));

        Awaitility.await()
                .atMost(Duration.ofSeconds(8))
                .pollInterval(Duration.ofMillis(300))
                .untilAsserted(() -> {
                    ResponseEntity<Map> r = rest.getForEntity("/jobs/{id}", Map.class, jobId);
                    assertThat(r.getStatusCode().is2xxSuccessful()).isTrue();

                    Map<?, ?> respBody = r.getBody();
                    assertThat(respBody).isNotNull();
                    Map<?, ?> respData = (Map<?, ?>) respBody.get("data");

                    assertThat(respData.get("status")).isEqualTo("SUCCEEDED");
                });
    }
}