package com.yerin.jobq.infra;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.*;

@DisplayName("백오프/지터 계산 테스트")
public class BackoffTest {

    @Test
    @DisplayName("지터 0과 상한 경계값 검증")
    void noJitter_and_cap() {
        Duration d0 = Backoff.expJitter(0, 1000, 60000, 0.0);
        Duration d3 = Backoff.expJitter(3, 1000, 60000, 0.0);
        Duration dc = Backoff.expJitter(10, 1000, 60000, 0.0);

        assertThat(d0.toMillis()).isEqualTo(1000);
        assertThat(d3.toMillis()).isEqualTo(8000);
        assertThat(dc.toMillis()).isEqualTo(60000); // cap
    }
}
