package com.bank.logging.propagation;

import com.bank.logging.mdc.MdcKeys;
import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/**
 * Interceptor pour RestClient (Spring Boot 3.2+)
 * Même interface que RestTemplate car RestClient utilise ClientHttpRequestInterceptor
 */
public class RestClientCorrelationInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {

        String correlationId = MDC.get(MdcKeys.CORRELATION_ID);
        String transactionId = MDC.get(MdcKeys.TRANSACTION_ID);
        String userId = MDC.get(MdcKeys.USER_ID);

        if (correlationId != null && !correlationId.isBlank()) {
            request.getHeaders().set(MdcKeys.HEADER_CORRELATION_ID, correlationId);
        }
        if (transactionId != null && !transactionId.isBlank()) {
            request.getHeaders().set(MdcKeys.HEADER_TRANSACTION_ID, transactionId);
        }
        if (userId != null && !userId.isBlank()) {
            request.getHeaders().set("X-User-ID", userId);
        }

        return execution.execute(request, body);
    }
}