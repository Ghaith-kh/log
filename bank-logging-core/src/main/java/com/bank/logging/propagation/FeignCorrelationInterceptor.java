package com.bank.logging.propagation;

import com.bank.logging.mdc.MdcKeys;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.slf4j.MDC;

public class FeignCorrelationInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        String correlationId = MDC.get(MdcKeys.CORRELATION_ID);
        if (correlationId != null && !correlationId.isBlank()) {
            template.header(MdcKeys.HEADER_CORRELATION_ID, correlationId);
        }

        String transactionId = MDC.get(MdcKeys.TRANSACTION_ID);
        if (transactionId != null && !transactionId.isBlank()) {
            template.header(MdcKeys.HEADER_TRANSACTION_ID, transactionId);
        }
    }
}
