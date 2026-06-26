package com.nicico.rasmio.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final String X_FORWARDED_FOR = "X-Forwarded-For";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long startedAt = System.currentTimeMillis();
        String clientIp = clientIp(request);
        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startedAt;
            log.info("request ip={} method={} uri={} query={} status={} durationMs={}",
                    clientIp,
                    request.getMethod(),
                    request.getRequestURI(),
                    request.getQueryString(),
                    response.getStatus(),
                    duration);
        }
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader(X_FORWARDED_FOR);
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
