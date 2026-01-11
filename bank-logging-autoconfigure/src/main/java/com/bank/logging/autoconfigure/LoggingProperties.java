package com.bank.logging.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bank.logging")
public class LoggingProperties {

    private boolean enabled = true;
    private String serviceName;
    private String environment;
    private MaskingProperties masking = new MaskingProperties();
    private AspectProperties aspect = new AspectProperties();
    private CorrelationProperties correlation = new CorrelationProperties();
    private PropagationProperties propagation = new PropagationProperties();

    public static class MaskingProperties {
        private boolean enabled = true;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    public static class AspectProperties {
        private boolean enabled = true;
        private long performanceThresholdMs = 1000L;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public long getPerformanceThresholdMs() { return performanceThresholdMs; }
        public void setPerformanceThresholdMs(long performanceThresholdMs) { this.performanceThresholdMs = performanceThresholdMs; }
    }

    public static class CorrelationProperties {
        private boolean enabled = true;
        private String headerName = "X-Correlation-ID";
        private boolean generateIfMissing = true;
        private boolean includeClientIp = true;
        private boolean includeRequestUri = true;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getHeaderName() { return headerName; }
        public void setHeaderName(String headerName) { this.headerName = headerName; }
        public boolean isGenerateIfMissing() { return generateIfMissing; }
        public void setGenerateIfMissing(boolean generateIfMissing) { this.generateIfMissing = generateIfMissing; }
        public boolean isIncludeClientIp() { return includeClientIp; }
        public void setIncludeClientIp(boolean includeClientIp) { this.includeClientIp = includeClientIp; }
        public boolean isIncludeRequestUri() { return includeRequestUri; }
        public void setIncludeRequestUri(boolean includeRequestUri) { this.includeRequestUri = includeRequestUri; }
    }

    public static class PropagationProperties {
        private boolean restTemplate = true;
        private boolean webClient = true;
        private boolean feign = true;

        public boolean isRestTemplate() { return restTemplate; }
        public void setRestTemplate(boolean restTemplate) { this.restTemplate = restTemplate; }
        public boolean isWebClient() { return webClient; }
        public void setWebClient(boolean webClient) { this.webClient = webClient; }
        public boolean isFeign() { return feign; }
        public void setFeign(boolean feign) { this.feign = feign; }
    }

    // Getters and setters
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public String getEnvironment() { return environment; }
    public void setEnvironment(String environment) { this.environment = environment; }
    public MaskingProperties getMasking() { return masking; }
    public void setMasking(MaskingProperties masking) { this.masking = masking; }
    public AspectProperties getAspect() { return aspect; }
    public void setAspect(AspectProperties aspect) { this.aspect = aspect; }
    public CorrelationProperties getCorrelation() { return correlation; }
    public void setCorrelation(CorrelationProperties correlation) { this.correlation = correlation; }
    public PropagationProperties getPropagation() { return propagation; }
    public void setPropagation(PropagationProperties propagation) { this.propagation = propagation; }
}
