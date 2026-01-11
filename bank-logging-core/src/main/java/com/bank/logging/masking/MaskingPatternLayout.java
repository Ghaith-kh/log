package com.bank.logging.masking;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;

public class MaskingPatternLayout extends PatternLayout {

    private final DataMasker dataMasker = new DataMasker();
    private boolean maskingEnabled = true;

    public void setMaskingEnabled(boolean enabled) {
        this.maskingEnabled = enabled;
    }

    @Override
    public String doLayout(ILoggingEvent event) {
        String message = super.doLayout(event);
        if (!maskingEnabled || message == null) {
            return message;
        }
        return dataMasker.mask(message);
    }
}
