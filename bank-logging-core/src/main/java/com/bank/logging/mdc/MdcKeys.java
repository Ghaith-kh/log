package com.bank.logging.mdc;

public final class MdcKeys {

    private MdcKeys() {}

    // Tracing
    public static final String CORRELATION_ID = "correlation_id";
    public static final String TRANSACTION_ID = "transaction_id";
    public static final String SPAN_ID = "span_id";
    public static final String TRACE_ID = "trace_id";

    // Request context
    public static final String CLIENT_IP = "client_ip";
    public static final String REQUEST_URI = "request_uri";
    public static final String REQUEST_METHOD = "request_method";
    public static final String USER_AGENT = "user_agent";
    public static final String SESSION_ID = "session_id";

    // User context
    public static final String USER_ID = "user_id";

    // Operation context
    public static final String OPERATION = "operation";
    public static final String OPERATION_ID = "operation_id";
    public static final String SERVICE_NAME = "service";

    // HTTP Headers
    public static final String HEADER_CORRELATION_ID = "X-Correlation-ID";
    public static final String HEADER_TRANSACTION_ID = "X-Transaction-ID";
    public static final String HEADER_REQUEST_ID = "X-Request-ID";
    public static final String HEADER_SPAN_ID = "X-Span-ID";
    public static final String HEADER_PARENT_SPAN_ID = "X-Parent-Span-ID";
    public static final String HEADER_TRACE_ID = "X-Trace-ID";
}
