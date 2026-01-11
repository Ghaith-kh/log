package com.bank.logging.propagation;

import com.bank.logging.mdc.MdcKeys;
import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

public class RestTemplateCorrelationInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                         ClientHttpRequestExecution execution) throws IOException {
        
        String correlationId = MDC.get(MdcKeys.CORRELATION_ID);
        if (correlationId != null && !correlationId.isBlank()) {
            request.getHeaders().set(MdcKeys.HEADER_CORRELATION_ID, correlationId);
        }

        String transactionId = MDC.get(MdcKeys.TRANSACTION_ID);
        if (transactionId != null && !transactionId.isBlank()) {
            request.getHeaders().set(MdcKeys.HEADER_TRANSACTION_ID, transactionId);
        }

        return execution.execute(request, body);
    }
}
