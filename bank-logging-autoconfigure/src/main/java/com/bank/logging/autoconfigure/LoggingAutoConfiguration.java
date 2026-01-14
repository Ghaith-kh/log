package com.bank.logging.autoconfigure;

import com.bank.logging.aspect.PaymentLoggingAspect;
import com.bank.logging.filter.CorrelationIdFilter;
import com.bank.logging.masking.DataMasker;
import com.bank.logging.propagation.FeignCorrelationInterceptor;
import com.bank.logging.propagation.RestClientCorrelationInterceptor;
import com.bank.logging.propagation.RestTemplateCorrelationInterceptor;
import com.bank.logging.propagation.WebClientCorrelationFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.Ordered;

@AutoConfiguration
@EnableConfigurationProperties(LoggingProperties.class)
@ConditionalOnProperty(prefix = "bank.logging", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LoggingAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(LoggingAutoConfiguration.class);

    private final LoggingProperties properties;

    @Value("${spring.application.name:unknown-service}")
    private String applicationName;

    public LoggingAutoConfiguration(LoggingProperties properties) {
        this.properties = properties;
        log.info("Bank Logging Starter initialized");
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "bank.logging.masking", name = "enabled", havingValue = "true", matchIfMissing = true)
    public DataMasker dataMasker() {
        return new DataMasker();
    }

    // Servlet Filter Configuration
    @Configuration
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnClass(name = "jakarta.servlet.Filter")
    public class ServletAutoConfiguration {

        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnProperty(prefix = "bank.logging.correlation", name = "enabled", havingValue = "true", matchIfMissing = true)
        public CorrelationIdFilter correlationIdFilter() {
            CorrelationIdFilter filter = new CorrelationIdFilter();
            LoggingProperties.CorrelationProperties corr = properties.getCorrelation();
            filter.setCorrelationIdHeader(corr.getHeaderName());
            filter.setGenerateIfMissing(corr.isGenerateIfMissing());
            filter.setIncludeClientIp(corr.isIncludeClientIp());
            filter.setIncludeRequestUri(corr.isIncludeRequestUri());
            return filter;
        }

        @Bean
        @ConditionalOnMissingBean(name = "correlationIdFilterRegistration")
        @ConditionalOnProperty(prefix = "bank.logging.correlation", name = "enabled", havingValue = "true", matchIfMissing = true)
        public FilterRegistrationBean<CorrelationIdFilter> correlationIdFilterRegistration(CorrelationIdFilter filter) {
            FilterRegistrationBean<CorrelationIdFilter> registration = new FilterRegistrationBean<>();
            registration.setFilter(filter);
            registration.addUrlPatterns("/*");
            registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
            registration.setName("correlationIdFilter");
            log.info("Registered CorrelationIdFilter");
            return registration;
        }
    }

    // AOP Configuration
    @Configuration
    @EnableAspectJAutoProxy
    @ConditionalOnClass(name = "org.aspectj.lang.annotation.Aspect")
    @ConditionalOnProperty(prefix = "bank.logging.aspect", name = "enabled", havingValue = "true", matchIfMissing = true)
    public class AspectAutoConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public PaymentLoggingAspect paymentLoggingAspect(DataMasker dataMasker) {
            PaymentLoggingAspect aspect = new PaymentLoggingAspect(dataMasker);
            aspect.setEnabled(properties.getAspect().isEnabled());
            aspect.setDefaultPerformanceThresholdMs(properties.getAspect().getPerformanceThresholdMs());
            log.info("Configured PaymentLoggingAspect with threshold={}ms", properties.getAspect().getPerformanceThresholdMs());
            return aspect;
        }
    }

    @Configuration
    @ConditionalOnClass(name = "org.springframework.web.client.RestClient")
    @ConditionalOnProperty(prefix = "bank.logging.propagation", name = "rest-client", havingValue = "true", matchIfMissing = true)
    public class RestClientAutoConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public RestClientCorrelationInterceptor restClientCorrelationInterceptor() {
            log.info(" RestClientCorrelationInterceptor created");
            return new RestClientCorrelationInterceptor();
        }
    }

    // RestTemplate Configuration
    @Configuration
    @ConditionalOnClass(name = "org.springframework.web.client.RestTemplate")
    @ConditionalOnProperty(prefix = "bank.logging.propagation", name = "rest-template", havingValue = "true", matchIfMissing = true)
    public class RestTemplateAutoConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public RestTemplateCorrelationInterceptor restTemplateCorrelationInterceptor() {
            return new RestTemplateCorrelationInterceptor();
        }
    }

    // WebClient Configuration
    @Configuration
    @ConditionalOnClass(name = "org.springframework.web.reactive.function.client.WebClient")
    @ConditionalOnProperty(prefix = "bank.logging.propagation", name = "web-client", havingValue = "true", matchIfMissing = true)
    public class WebClientAutoConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public WebClientCorrelationFilter webClientCorrelationFilter() {
            return WebClientCorrelationFilter.create();
        }
    }

    // Feign Configuration
    @Configuration
    @ConditionalOnClass(name = "feign.RequestInterceptor")
    @ConditionalOnProperty(prefix = "bank.logging.propagation", name = "feign", havingValue = "true", matchIfMissing = true)
    public class FeignAutoConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public FeignCorrelationInterceptor feignCorrelationInterceptor() {
            return new FeignCorrelationInterceptor();
        }
    }
}
