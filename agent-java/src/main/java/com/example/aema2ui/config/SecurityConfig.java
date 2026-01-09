package com.example.aema2ui.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    /**
     * Filter for security headers and request logging.
     */
    @Bean
    public FilterRegistrationBean<SecurityHeadersFilter> securityHeadersFilter() {
        FilterRegistrationBean<SecurityHeadersFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new SecurityHeadersFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    /**
     * Simple rate limiter filter.
     */
    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilter() {
        FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new RateLimitFilter());
        registration.addUrlPatterns("/tasks", "/advanced/tasks");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        return registration;
    }

    /**
     * Filter that adds security headers to all responses.
     */
    public static class SecurityHeadersFilter implements Filter {

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {

            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;

            // Generate request ID for tracing
            String requestId = UUID.randomUUID().toString().substring(0, 8);
            httpResponse.setHeader("X-Request-Id", requestId);

            // Security headers
            httpResponse.setHeader("X-Content-Type-Options", "nosniff");
            httpResponse.setHeader("X-Frame-Options", "DENY");
            httpResponse.setHeader("X-XSS-Protection", "1; mode=block");
            httpResponse.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
            httpResponse.setHeader("Pragma", "no-cache");

            // Log request
            long startTime = System.currentTimeMillis();
            String method = httpRequest.getMethod();
            String uri = httpRequest.getRequestURI();

            try {
                chain.doFilter(request, response);
            } finally {
                long duration = System.currentTimeMillis() - startTime;
                logger.info("[{}] {} {} - {} ({}ms)",
                        requestId, method, uri, httpResponse.getStatus(), duration);
            }
        }
    }

    /**
     * Simple in-memory rate limiter.
     * In production, use Redis or a dedicated rate limiting solution.
     */
    public static class RateLimitFilter implements Filter {

        private static final int MAX_REQUESTS_PER_MINUTE = 60;
        private static final long WINDOW_MS = 60_000;

        private final Map<String, RequestCounter> counters = new ConcurrentHashMap<>();

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {

            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;

            String clientIp = getClientIp(httpRequest);
            RequestCounter counter = counters.computeIfAbsent(clientIp, k -> new RequestCounter());

            if (counter.isRateLimited()) {
                httpResponse.setStatus(429);
                httpResponse.setContentType("application/json");
                httpResponse.getWriter().write("{\"error\":\"Too many requests. Please try again later.\"}");
                return;
            }

            counter.increment();
            httpResponse.setHeader("X-RateLimit-Limit", String.valueOf(MAX_REQUESTS_PER_MINUTE));
            httpResponse.setHeader("X-RateLimit-Remaining", String.valueOf(counter.getRemaining()));

            chain.doFilter(request, response);
        }

        private String getClientIp(HttpServletRequest request) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isEmpty()) {
                return forwarded.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        }

        private static class RequestCounter {
            private final AtomicInteger count = new AtomicInteger(0);
            private volatile long windowStart = System.currentTimeMillis();

            boolean isRateLimited() {
                checkWindow();
                return count.get() >= MAX_REQUESTS_PER_MINUTE;
            }

            void increment() {
                checkWindow();
                count.incrementAndGet();
            }

            int getRemaining() {
                checkWindow();
                return Math.max(0, MAX_REQUESTS_PER_MINUTE - count.get());
            }

            private void checkWindow() {
                long now = System.currentTimeMillis();
                if (now - windowStart > WINDOW_MS) {
                    synchronized (this) {
                        if (now - windowStart > WINDOW_MS) {
                            count.set(0);
                            windowStart = now;
                        }
                    }
                }
            }
        }
    }
}
