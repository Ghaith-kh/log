package com.bank.logging.propagation;

import com.bank.logging.mdc.MdcKeys;
import org.slf4j.MDC;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

import java.util.Map;

public class WebClientCorrelationFilter implements ExchangeFilterFunction {

    public static WebClientCorrelationFilter create() {
        return new WebClientCorrelationFilter();
    }

    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        Map<String, String> mdcContext = MDC.getCopyOfContextMap();
        
        ClientRequest.Builder requestBuilder = ClientRequest.from(request);
        
        if (mdcContext != null) {
            String correlationId = mdcContext.get(MdcKeys.CORRELATION_ID);
            if (correlationId != null && !correlationId.isBlank()) {
                requestBuilder.header(MdcKeys.HEADER_CORRELATION_ID, correlationId);
            }

            String transactionId = mdcContext.get(MdcKeys.TRANSACTION_ID);
            if (transactionId != null && !transactionId.isBlank()) {
                requestBuilder.header(MdcKeys.HEADER_TRANSACTION_ID, transactionId);
            }
        }

        return next.exchange(requestBuilder.build());
    }
}
