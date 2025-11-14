package com.yerin.jobq.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class AdminTokenInterceptor implements HandlerInterceptor {

    @Value("${jobq.admin.token:}")
    private String adminToken;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        // 필요한 엔드포인트만 WebMvcConfig에서 pathPattern으로 묶어서 들어오게 할 예정
        String token = request.getHeader("X-ADMIN-TOKEN");
        if (adminToken != null && !adminToken.isBlank() && adminToken.equals(token)) {
            return true;
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        return false;
    }
}
