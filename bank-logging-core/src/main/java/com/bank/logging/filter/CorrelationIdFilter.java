package com.bank.logging.filter;

import com.bank.logging.mdc.MdcKeys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);

    private boolean includeClientIp = true;
    private boolean includeRequestUri = true;
    private boolean generateIfMissing = true;
    private String correlationIdHeader = MdcKeys.HEADER_CORRELATION_ID;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            setupMdc(request);
            
            String correlationId = MDC.get(MdcKeys.CORRELATION_ID);
            if (correlationId != null) {
                response.setHeader(correlationIdHeader, correlationId);
            }

            filterChain.doFilter(request, response);

        } finally {
            clearMdc();
        }
    }

    private void setupMdc(HttpServletRequest request) {
        // Correlation ID
        String correlationId = request.getHeader(correlationIdHeader);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = request.getHeader(MdcKeys.HEADER_REQUEST_ID);
        }
        if ((correlationId == null || correlationId.isBlank()) && generateIfMissing) {
            correlationId = UUID.randomUUID().toString();
        }
        if (correlationId != null) {
            MDC.put(MdcKeys.CORRELATION_ID, correlationId);
        }

        // Transaction ID
        String transactionId = request.getHeader(MdcKeys.HEADER_TRANSACTION_ID);
        if (transactionId != null && !transactionId.isBlank()) {
            MDC.put(MdcKeys.TRANSACTION_ID, transactionId);
        }

        // Client IP
        if (includeClientIp) {
            MDC.put(MdcKeys.CLIENT_IP, extractClientIp(request));
        }

        // Request URI
        if (includeRequestUri) {
            MDC.put(MdcKeys.REQUEST_URI, request.getRequestURI());
            MDC.put(MdcKeys.REQUEST_METHOD, request.getMethod());
        }

        // User ID from security context
        extractUserId();
    }

    private String extractClientIp(HttpServletRequest request) {
        String[] headers = {"X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP"};
        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.contains(",") ? ip.split(",")[0].trim() : ip;
            }
        }
        return request.getRemoteAddr();
    }

    private void extractUserId() {
        try {
            var ctx = org.springframework.security.core.context.SecurityContextHolder.getContext();
            var auth = ctx.getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                MDC.put(MdcKeys.USER_ID, auth.getName());
            }
        } catch (NoClassDefFoundError e) {
            // Spring Security not on classpath
        }
    }

    private void clearMdc() {
        MDC.remove(MdcKeys.CORRELATION_ID);
        MDC.remove(MdcKeys.TRANSACTION_ID);
        MDC.remove(MdcKeys.CLIENT_IP);
        MDC.remove(MdcKeys.REQUEST_URI);
        MDC.remove(MdcKeys.REQUEST_METHOD);
        MDC.remove(MdcKeys.USER_ID);
    }

    // Setters
    public void setIncludeClientIp(boolean includeClientIp) { this.includeClientIp = includeClientIp; }
    public void setIncludeRequestUri(boolean includeRequestUri) { this.includeRequestUri = includeRequestUri; }
    public void setGenerateIfMissing(boolean generateIfMissing) { this.generateIfMissing = generateIfMissing; }
    public void setCorrelationIdHeader(String correlationIdHeader) { this.correlationIdHeader = correlationIdHeader; }
}
