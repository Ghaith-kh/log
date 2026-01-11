package com.bank.logging.masking;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.LayoutBase;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class MaskingJsonLayout extends LayoutBase<ILoggingEvent> {

    private static final DateTimeFormatter ISO_FORMATTER = 
        DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.systemDefault());

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DataMasker dataMasker = new DataMasker();

    private String serviceName = "unknown-service";
    private String environment = "unknown";
    private boolean includeStackTrace = true;
    private boolean includeMdc = true;
    private boolean maskingEnabled = true;
    private int maxStackTraceDepth = 50;

    @Override
    public String doLayout(ILoggingEvent event) {
        ObjectNode root = objectMapper.createObjectNode();

        root.put("@timestamp", ISO_FORMATTER.format(Instant.ofEpochMilli(event.getTimeStamp())));
        root.put("level", event.getLevel().toString());
        root.put("logger", event.getLoggerName());
        root.put("thread", event.getThreadName());
        root.put("service", serviceName);
        root.put("environment", environment);

        if (includeMdc) {
            Map<String, String> mdc = event.getMDCPropertyMap();
            if (mdc != null && !mdc.isEmpty()) {
                ObjectNode contextNode = root.putObject("context");
                mdc.forEach((key, value) -> {
                    String maskedValue = maskingEnabled ? dataMasker.mask(value) : value;
                    contextNode.put(key, maskedValue);
                });
            }
        }

        String message = event.getFormattedMessage();
        if (message != null) {
            root.put("message", maskingEnabled ? dataMasker.mask(message) : message);
        }

        IThrowableProxy throwable = event.getThrowableProxy();
        if (throwable != null && includeStackTrace) {
            ObjectNode exNode = root.putObject("exception");
            exNode.put("class", throwable.getClassName());
            if (throwable.getMessage() != null) {
                exNode.put("message", maskingEnabled ? dataMasker.mask(throwable.getMessage()) : throwable.getMessage());
            }
            ArrayNode stackTrace = exNode.putArray("stack_trace");
            StackTraceElementProxy[] frames = throwable.getStackTraceElementProxyArray();
            int depth = Math.min(frames.length, maxStackTraceDepth);
            for (int i = 0; i < depth; i++) {
                stackTrace.add(frames[i].getSTEAsString());
            }
        }

        try {
            return objectMapper.writeValueAsString(root) + System.lineSeparator();
        } catch (JsonProcessingException e) {
            return "{\"error\":\"JSON serialization failed\"}" + System.lineSeparator();
        }
    }

    // Setters for Logback configuration
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public void setEnvironment(String environment) { this.environment = environment; }
    public void setIncludeStackTrace(boolean includeStackTrace) { this.includeStackTrace = includeStackTrace; }
    public void setIncludeMdc(boolean includeMdc) { this.includeMdc = includeMdc; }
    public void setMaskingEnabled(boolean maskingEnabled) { this.maskingEnabled = maskingEnabled; }
    public void setMaxStackTraceDepth(int maxStackTraceDepth) { this.maxStackTraceDepth = maxStackTraceDepth; }
}
