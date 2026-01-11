package com.bank.logging.aspect;

import com.bank.logging.annotation.NoLogging;
import com.bank.logging.annotation.PaymentLog;
import com.bank.logging.annotation.PaymentLog.LogLevel;
import com.bank.logging.masking.DataMasker;
import com.bank.logging.mdc.MdcKeys;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Aspect
public class PaymentLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(PaymentLoggingAspect.class);
    private static final Logger auditLog = LoggerFactory.getLogger("AUDIT");

    private final DataMasker dataMasker;
    private final ObjectMapper objectMapper;
    private boolean enabled = true;
    private long defaultPerformanceThresholdMs = 1000L;

    public PaymentLoggingAspect() {
        this.dataMasker = new DataMasker();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    }

    public PaymentLoggingAspect(DataMasker dataMasker) {
        this.dataMasker = dataMasker != null ? dataMasker : new DataMasker();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    }

    @Pointcut("@annotation(com.bank.logging.annotation.PaymentLog)")
    public void paymentLogMethodAnnotation() {}

    @Pointcut("@within(com.bank.logging.annotation.PaymentLog) && execution(public * *(..))")
    public void paymentLogClassAnnotation() {}

    @Pointcut("@annotation(com.bank.logging.annotation.NoLogging)")
    public void noLoggingAnnotation() {}

    @Pointcut("(paymentLogMethodAnnotation() || paymentLogClassAnnotation()) && !noLoggingAnnotation()")
    public void loggableMethods() {}

    @Around("loggableMethods()")
    public Object logPaymentOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!enabled) {
            return joinPoint.proceed();
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Class<?> targetClass = joinPoint.getTarget().getClass();

        PaymentLog annotation = getPaymentLogAnnotation(method, targetClass);
        if (annotation == null) {
            return joinPoint.proceed();
        }

        String operationId = UUID.randomUUID().toString().substring(0, 8);
        String operation = annotation.operation().isBlank() ? method.getName().toUpperCase() : annotation.operation();
        String className = targetClass.getSimpleName();
        String methodName = method.getName();

        MDC.put(MdcKeys.OPERATION, operation);
        MDC.put(MdcKeys.OPERATION_ID, operationId);

        long startTimeNanos = System.nanoTime();

        try {
            logEntry(annotation, className, methodName, operation, joinPoint.getArgs(), signature.getParameterNames());

            Object result = joinPoint.proceed();

            long executionTimeMs = (System.nanoTime() - startTimeNanos) / 1_000_000;
            logExit(annotation, className, methodName, operation, result, executionTimeMs);
            checkPerformanceThreshold(annotation, operation, executionTimeMs);

            if (annotation.auditEnabled()) {
                logAudit(operation, operationId, "SUCCESS", executionTimeMs, null);
            }

            return result;

        } catch (Exception e) {
            long executionTimeMs = (System.nanoTime() - startTimeNanos) / 1_000_000;
            logError(className, methodName, operation, e, executionTimeMs);

            if (annotation.auditEnabled()) {
                logAudit(operation, operationId, "FAILURE", executionTimeMs, e);
            }
            throw e;

        } finally {
            MDC.remove(MdcKeys.OPERATION);
            MDC.remove(MdcKeys.OPERATION_ID);
        }
    }

    private PaymentLog getPaymentLogAnnotation(Method method, Class<?> targetClass) {
        PaymentLog methodAnnotation = method.getAnnotation(PaymentLog.class);
        if (methodAnnotation != null) {
            return methodAnnotation;
        }
        return targetClass.getAnnotation(PaymentLog.class);
    }

    private void logEntry(PaymentLog annotation, String className, String methodName,
                          String operation, Object[] args, String[] paramNames) {
        if (!isLogLevelEnabled(annotation.entryLevel())) return;

        Map<String, Object> logData = new LinkedHashMap<>();
        logData.put("phase", "ENTRY");
        logData.put("operation", operation);
        logData.put("class", className);
        logData.put("method", methodName);

        if (annotation.logParams() && args != null && args.length > 0) {
            Map<String, Object> params = new LinkedHashMap<>();
            for (int i = 0; i < args.length; i++) {
                String paramName = (paramNames != null && i < paramNames.length) ? paramNames[i] : "arg" + i;
                params.put(paramName, maskObject(args[i]));
            }
            logData.put("params", params);
        }

        logAtLevel(annotation.entryLevel(), "Payment operation started: {}", safeSerialize(logData));
    }

    private void logExit(PaymentLog annotation, String className, String methodName,
                         String operation, Object result, long executionTimeMs) {
        if (!isLogLevelEnabled(annotation.exitLevel())) return;

        Map<String, Object> logData = new LinkedHashMap<>();
        logData.put("phase", "EXIT");
        logData.put("operation", operation);
        logData.put("class", className);
        logData.put("method", methodName);
        logData.put("execution_time_ms", executionTimeMs);

        if (annotation.logResult() && result != null) {
            logData.put("result", maskObject(result));
        }

        logAtLevel(annotation.exitLevel(), "Payment operation completed: {}", safeSerialize(logData));
    }

    private void logError(String className, String methodName, String operation, Exception e, long executionTimeMs) {
        Map<String, Object> logData = new LinkedHashMap<>();
        logData.put("phase", "ERROR");
        logData.put("operation", operation);
        logData.put("class", className);
        logData.put("method", methodName);
        logData.put("execution_time_ms", executionTimeMs);
        logData.put("exception_type", e.getClass().getName());
        logData.put("exception_message", dataMasker.mask(e.getMessage()));

        log.error("Payment operation failed: {}", safeSerialize(logData), e);
    }

    private void checkPerformanceThreshold(PaymentLog annotation, String operation, long executionTimeMs) {
        long threshold = annotation.performanceThresholdMs() > 0 
            ? annotation.performanceThresholdMs() 
            : defaultPerformanceThresholdMs;

        if (executionTimeMs > threshold) {
            log.warn("Performance threshold exceeded for {}: {}ms (threshold: {}ms)", operation, executionTimeMs, threshold);
        }
    }

    private void logAudit(String operation, String operationId, String status, long executionTimeMs, Exception e) {
        Map<String, Object> auditData = new LinkedHashMap<>();
        auditData.put("audit_type", "PAYMENT_OPERATION");
        auditData.put("timestamp", Instant.now().toString());
        auditData.put("operation", operation);
        auditData.put("operation_id", operationId);
        auditData.put("status", status);
        auditData.put("execution_time_ms", executionTimeMs);
        auditData.put("correlation_id", MDC.get(MdcKeys.CORRELATION_ID));
        auditData.put("user_id", MDC.get(MdcKeys.USER_ID));

        if (e != null) {
            auditData.put("error_type", e.getClass().getName());
            auditData.put("error_message", dataMasker.mask(e.getMessage()));
        }

        auditLog.info("AUDIT: {}", safeSerialize(auditData));
    }

    private Object maskObject(Object obj) {
        if (obj == null) return null;
        try {
            String json = objectMapper.writeValueAsString(obj);
            String maskedJson = dataMasker.mask(json);
            return objectMapper.readValue(maskedJson, Object.class);
        } catch (JsonProcessingException e) {
            return dataMasker.mask(obj.toString());
        }
    }

    private String safeSerialize(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return obj.toString();
        }
    }

    private boolean isLogLevelEnabled(LogLevel level) {
        return switch (level) {
            case TRACE -> log.isTraceEnabled();
            case DEBUG -> log.isDebugEnabled();
            case INFO -> log.isInfoEnabled();
            case WARN -> log.isWarnEnabled();
        };
    }

    private void logAtLevel(LogLevel level, String message, Object... args) {
        switch (level) {
            case TRACE -> log.trace(message, args);
            case DEBUG -> log.debug(message, args);
            case INFO -> log.info(message, args);
            case WARN -> log.warn(message, args);
        }
    }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setDefaultPerformanceThresholdMs(long ms) { this.defaultPerformanceThresholdMs = ms; }
}
