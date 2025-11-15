package com.yerin.jobq.web;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.*;

@DisplayName("관리자 토큰 인터셉터 테스트")
public class AdminTokenInterceptorTest {
    @Test
    @DisplayName("토큰 헤더가 없으면 401로 차단")
    void blocks_when_header_missing() throws Exception {
        var inter = new AdminTokenInterceptor("secret");

        var req = new org.springframework.mock.web.MockHttpServletRequest();
        var res = new org.springframework.mock.web.MockHttpServletResponse();

        boolean pass = inter.preHandle(req, res, new Object());

        assertThat(pass).isFalse();
        assertThat(res.getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("헤더가 토큰과 일치하면 통과")
    void passes_when_header_matches() throws Exception {
        var inter = new AdminTokenInterceptor("secret");

        var req = new org.springframework.mock.web.MockHttpServletRequest();
        req.addHeader("X-ADMIN-TOKEN", "secret");
        var res = new org.springframework.mock.web.MockHttpServletResponse();

        boolean pass = inter.preHandle(req, res, new Object());

        assertThat(pass).isTrue();
        assertThat(res.getStatus()).isEqualTo(200);
    }

}
