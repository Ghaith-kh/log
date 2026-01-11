package com.bank.logging.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PaymentLog {

    String operation() default "";
    
    boolean logParams() default true;
    
    boolean logResult() default true;
    
    boolean auditEnabled() default false;
    
    long performanceThresholdMs() default 1000L;
    
    LogLevel entryLevel() default LogLevel.INFO;
    
    LogLevel exitLevel() default LogLevel.INFO;
    
    String message() default "";

    enum LogLevel {
        TRACE, DEBUG, INFO, WARN
    }
}
